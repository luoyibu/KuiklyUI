/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI.
 */

package com.tencent.kuikly.compose.animation.core

import com.tencent.kuikly.core.base.Attr

/**
 * Keeps the Native-specific lifecycle out of [Transition]. The transition itself only supplies
 * state mutations that must remain private to its animation state.
 */
internal class NativeTransitionAnimationState {
    var isActive: Boolean = false
        private set

    private var generation = 0L

    fun <T, V : AnimationVector> tryStart(
        transitionKey: Any,
        label: String,
        animationSpec: FiniteAnimationSpec<T>,
        initialValue: T,
        targetValue: T,
        initialVelocity: T,
        converter: TwoWayConverter<T, V>,
        isSeeking: Boolean,
        prepare: () -> Unit,
        commitTarget: () -> Unit,
        finish: (Boolean) -> Unit
    ): Boolean {
        val coordinator = NativeAnimationCoordinator.currentOrNull()
        val nativeAnimation = animationSpec.toNativeAnimationOrNull(
            initialValue = initialValue,
            targetValue = targetValue,
            initialVelocity = initialVelocity,
            converter = converter
        )
        if (nativeAnimation == null || coordinator == null || isSeeking) {
            coordinator?.rejectTransition(transitionKey)
            isActive = false
            return false
        }

        prepare()
        val currentGeneration = ++generation
        val accepted = coordinator.animateTransition(
            transitionKey = transitionKey,
            propertyHint = label.nativeAnimationPropertyHint(),
            animation = nativeAnimation,
            targetStateCommit = {
                commitTarget()
                isActive = true
            }
        ) { finished ->
            if (currentGeneration != generation) return@animateTransition
            isActive = false
            finish(finished)
        }
        if (!accepted) {
            isActive = false
        }
        return accepted
    }
}

private fun String.nativeAnimationPropertyHint(): String? {
    val normalizedLabel = lowercase()
    return when {
        "alpha" in normalizedLabel || "crossfade" in normalizedLabel ->
            Attr.StyleConst.OPACITY

        "scale" in normalizedLabel ||
            "transformorigin" in normalizedLabel ||
            "slide" in normalizedLabel ->
            Attr.StyleConst.TRANSFORM

        "background" in normalizedLabel && "color" in normalizedLabel ->
            Attr.StyleConst.BACKGROUND_COLOR

        else -> null
    }
}
