/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.compose.animation.core

import com.tencent.kuikly.core.base.AbstractBaseView
import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.NativeAnimationBridge
import com.tencent.kuikly.core.base.PagerScope
import com.tencent.kuikly.core.base.nativeCallbackTimeoutMillis
import com.tencent.kuikly.core.base.nativeSnap
import com.tencent.kuikly.core.base.registerPersistentNativeAnimationCompletion
import com.tencent.kuikly.core.base.unregisterNativeAnimationCompletion
import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.pager.IPager
import com.tencent.kuikly.core.timer.clearTimeout
import com.tencent.kuikly.core.timer.setTimeout
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * A page-local transaction coordinator. It stages the target-state property writes produced by
 * one Compose render pass and only exposes them to Native Render after the complete batch has
 * passed validation.
 */
internal class NativeAnimationCoordinator private constructor(
    private val pager: IPager
) : NativeAnimationBridge {
    private val pagerScope = pager as PagerScope
    private data class Operation(
        val view: AbstractBaseView<*, *>,
        val attr: Attr,
        val propertyKey: String,
        val previousValue: Any?,
        val targetValue: Any
    )

    private class Group(
        val id: Long,
        val animation: Animation,
        val descriptorSignature: String,
        val continuation: CancellableContinuation<Boolean>?,
        val transitionKey: Any? = null,
        val operations: MutableList<Operation> = mutableListOf(),
        val propertyAnimations: MutableMap<String, Animation> = mutableMapOf(),
        val propertyAnimationSignatures: MutableMap<String, String> = mutableMapOf(),
        val transitionCompletions: MutableList<(Boolean) -> Unit> = mutableListOf(),
        var unsupported: Boolean = false,
        var committed: Boolean = false,
        var timeoutRef: String? = null,
        val pendingCallbacksByView: MutableMap<Int, Int> = mutableMapOf()
    )

    private var nextGroupId = 1L
    private var activeGroup: Group? = null
    private val runningGroups = mutableMapOf<Long, Group>()
    private val rejectedTransitionKeys = mutableSetOf<Any>()
    private var destroyed = false

    suspend fun animate(animation: Animation, targetStateCommit: () -> Unit): Boolean =
        suspendCancellableCoroutine { continuation ->
            if (destroyed) {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }
            cancelActiveGroup()
            val group = Group(
                nextGroupId++,
                animation,
                animation.toString(),
                continuation
            )
            animation.key = "composeNativeAnimation_${group.id}"
            activeGroup = group
            continuation.invokeOnCancellation {
                if (activeGroup === group) {
                    rollback(group)
                    activeGroup = null
                } else if (runningGroups[group.id] === group) {
                    snapCommittedGroupToLogicalTarget(group)
                    finish(group, false)
                }
            }
            targetStateCommit()
        }

    fun animateTransition(
        transitionKey: Any,
        propertyHint: String?,
        animation: Animation,
        targetStateCommit: () -> Unit,
        completion: (Boolean) -> Unit
    ): Boolean {
        if (destroyed || transitionKey in rejectedTransitionKeys) return false
        val signature = animation.toString()
        val pending = activeGroup
        val group = when {
            pending == null -> Group(
                nextGroupId++,
                animation,
                signature,
                continuation = null,
                transitionKey = transitionKey
            ).also {
                animation.key = "composeNativeAnimation_${it.id}"
                activeGroup = it
            }
            pending.continuation != null -> return false
            pending.transitionKey !== transitionKey -> return false
            propertyHint == null && pending.descriptorSignature != signature -> {
                pending.unsupported = true
                return false
            }
            else -> pending
        }
        if (propertyHint != null) {
            val existingSignature = group.propertyAnimationSignatures[propertyHint]
            if (existingSignature != null && existingSignature != signature) {
                group.unsupported = true
                return false
            }
            animation.key = group.animation.key
            group.propertyAnimations[propertyHint] = animation
            group.propertyAnimationSignatures[propertyHint] = signature
        }
        group.transitionCompletions += completion
        targetStateCommit()
        return true
    }

    fun rejectTransition(transitionKey: Any) {
        rejectedTransitionKeys += transitionKey
        activeGroup?.takeIf { it.transitionKey === transitionKey }?.unsupported = true
    }

    override fun stageProperty(
        view: AbstractBaseView<*, *>,
        attr: Attr,
        propertyKey: String,
        previousValue: Any?,
        targetValue: Any
    ): Boolean {
        val group = activeGroup ?: return false
        if (group.committed || propertyKey == Attr.StyleConst.ANIMATION) return false
        group.operations += Operation(view, attr, propertyKey, previousValue, targetValue)
        if (propertyKey !in SUPPORTED_PROPERTIES) {
            group.unsupported = true
        }
        return true
    }

    override fun stageFrame(view: AbstractBaseView<*, *>): Boolean {
        val group = activeGroup ?: return false
        if (group.committed) return false
        // A frame write is expected when an entering/crossfading node is first laid out. Do not
        // consume it: only a transaction with no supported visual property is a layout animation.
        if (group.propertyAnimations.isNotEmpty()) return false
        group.unsupported = true
        return true
    }

    override fun commitStagedProperties() {
        val group = activeGroup ?: run {
            rejectedTransitionKeys.clear()
            return
        }
        rejectedTransitionKeys.clear()
        if (group.committed) return
        if (
            group.unsupported ||
            group.operations.isEmpty()
        ) {
            rollback(group)
            activeGroup = null
            if (group.continuation?.isActive == true) group.continuation.resume(false)
            group.transitionCompletions.forEach { it(false) }
            return
        }

        group.committed = true
        val operationsByView = group.operations.groupBy { it.view }
        val replacingProperties = group.operations.mapTo(mutableSetOf()) {
            it.view.nativeRef to it.propertyKey
        }
        runningGroups.values.filter { running ->
            running.operations.any { (it.view.nativeRef to it.propertyKey) in replacingProperties }
        }.toList().forEach { finish(it, false) }
        operationsByView.forEach { (view, operations) ->
            val declarativeView = view as DeclarativeBaseView<*, *>
            val operationBatches = operations.groupBy { operation ->
                group.propertyAnimations[operation.propertyKey] ?: group.animation
            }
            group.pendingCallbacksByView[view.nativeRef] = operationBatches.size
            declarativeView.registerPersistentNativeAnimationCompletion(
                group.animation.key
            ) { finished: Boolean ->
                onViewAnimationFinished(group, view.nativeRef, finished)
            }
            operationBatches.forEach { (animation, batch) ->
                animation.key = group.animation.key
                view.syncProp(Attr.StyleConst.ANIMATION, animation.toString())
                batch.forEach { operation ->
                    view.syncProp(operation.propertyKey, operation.targetValue)
                }
            }
        }
        activeGroup = null
        runningGroups[group.id] = group
        val timeoutMillis = (
            group.propertyAnimations.values + group.animation
            ).maxOf { it.nativeCallbackTimeoutMillis() }
        group.timeoutRef = pagerScope.setTimeout(timeoutMillis) {
            if (runningGroups[group.id] === group) {
                snapCommittedGroupToLogicalTarget(group)
                finish(group, false)
            }
        }
        // Commit only after every descriptor and target property has entered the render queue.
        operationsByView.keys.forEach { view ->
            view.syncProp(Attr.StyleConst.ANIMATION, "")
        }
    }

    private fun onViewAnimationFinished(group: Group, viewRef: Int, finished: Boolean) {
        if (runningGroups[group.id] !== group) return
        if (!finished) {
            snapCommittedGroupToLogicalTarget(group)
            finish(group, false)
            return
        }
        val remaining = (group.pendingCallbacksByView[viewRef] ?: return) - 1
        if (remaining <= 0) {
            group.pendingCallbacksByView.remove(viewRef)
        } else {
            group.pendingCallbacksByView[viewRef] = remaining
        }
        if (group.pendingCallbacksByView.isEmpty()) finish(group, true)
    }

    private fun finish(group: Group, result: Boolean) {
        if (activeGroup === group) activeGroup = null
        runningGroups.remove(group.id)
        group.operations.map { it.view }.distinct().forEach {
            (it as? DeclarativeBaseView<*, *>)
                ?.unregisterNativeAnimationCompletion(group.animation.key)
        }
        group.timeoutRef?.let { pagerScope.clearTimeout(it) }
        group.timeoutRef = null
        if (group.continuation?.isActive == true) {
            if (result) {
                group.continuation.resume(true)
            } else {
                group.continuation.cancel()
            }
        }
        group.transitionCompletions.forEach { it(result) }
    }

    private fun rollback(group: Group) {
        group.operations.asReversed().forEach {
            if (it.previousValue == null) {
                it.attr.removePropCache(it.propertyKey)
            } else {
                it.attr.updatePropCache(it.propertyKey, it.previousValue)
            }
        }
        group.operations.clear()
    }

    private fun cancelActiveGroup() {
        val old = activeGroup ?: return
        rollback(old)
        activeGroup = null
        old.continuation?.cancel()
        old.transitionCompletions.forEach { it(false) }
    }

    private fun snapCommittedGroupToLogicalTarget(group: Group) {
        val snap = Animation.nativeSnap(0f, "composeNativeAnimationCancel_${group.id}")
        group.operations.groupBy { it.view }.forEach { (view, operations) ->
            view.syncProp(Attr.StyleConst.ANIMATION, snap.toString())
            operations.forEach {
                view.syncProp(it.propertyKey, it.targetValue)
            }
            view.syncProp(Attr.StyleConst.ANIMATION, "")
        }
    }

    override fun destroy() {
        destroyed = true
        cancelActiveGroup()
        runningGroups.values.toList().forEach {
            snapCommittedGroupToLogicalTarget(it)
            it.timeoutRef?.let { timeoutRef -> pagerScope.clearTimeout(timeoutRef) }
            it.timeoutRef = null
            if (it.continuation?.isActive == true) it.continuation.cancel()
            it.transitionCompletions.forEach { completion -> completion(false) }
        }
        runningGroups.clear()
        pager.setValueForKey(NativeAnimationBridge.PAGER_CACHE_KEY, null)
    }

    companion object {
        private val SUPPORTED_PROPERTIES = setOf(
            Attr.StyleConst.OPACITY,
            Attr.StyleConst.TRANSFORM,
            Attr.StyleConst.BACKGROUND_COLOR
        )

        fun currentOrNull(): NativeAnimationCoordinator? = try {
            getOrCreate(PagerManager.getCurrentPager())
        } catch (_: Throwable) {
            null
        }

        fun getOrCreate(pager: IPager): NativeAnimationCoordinator {
            val existing =
                pager.getValueForKey(NativeAnimationBridge.PAGER_CACHE_KEY)
                    as? NativeAnimationCoordinator
            if (existing != null) return existing
            return NativeAnimationCoordinator(pager).also {
                pager.setValueForKey(NativeAnimationBridge.PAGER_CACHE_KEY, it)
            }
        }
    }
}
