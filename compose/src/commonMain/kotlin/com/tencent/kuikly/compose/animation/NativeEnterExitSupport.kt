/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI.
 */

package com.tencent.kuikly.compose.animation

import com.tencent.kuikly.compose.animation.core.FiniteAnimationSpec
import com.tencent.kuikly.compose.animation.core.Transition
import com.tencent.kuikly.compose.animation.core.nativePreferredOriginalOrNull
import com.tencent.kuikly.compose.animation.core.retargetNativeCurve
import com.tencent.kuikly.compose.animation.core.spring
import com.tencent.kuikly.compose.ui.graphics.TransformOrigin
import com.tencent.kuikly.compose.ui.unit.IntOffset

internal fun Transition.Segment<EnterExitState>.nativeTransformOriginSpec(
    enter: EnterTransition,
    exit: ExitTransition
): FiniteAnimationSpec<TransformOrigin> = when {
    EnterExitState.PreEnter isTransitioningTo EnterExitState.Visible ->
        enter.data.scale?.animationSpec
            ?.retargetNativeCurve<Float, TransformOrigin>()
            ?: spring()

    EnterExitState.Visible isTransitioningTo EnterExitState.PostExit ->
        exit.data.scale?.animationSpec
            ?.retargetNativeCurve<Float, TransformOrigin>()
            ?: spring()

    else -> {
        val directionalSpec =
            if (targetState == EnterExitState.Visible) {
                enter.data.scale?.animationSpec
            } else {
                exit.data.scale?.animationSpec
            }
        directionalSpec
            ?.retargetNativeCurve<Float, TransformOrigin>()
            ?: spring()
    }
}

internal fun FiniteAnimationSpec<IntOffset>.usesNativeGraphicsTranslation(): Boolean =
    nativePreferredOriginalOrNull() != null

/**
 * Compose normally switches a reversing Enter/Exit animation to an internal spring. Native Render
 * already continues from the platform presentation value, so use the explicitly opted-in curve
 * for the new direction. This also keeps scale and slide on one descriptor for their shared
 * transform property.
 */
internal fun <T> Transition.Segment<EnterExitState>.nativeInterruptionSpec(
    enterSpec: FiniteAnimationSpec<T>?,
    exitSpec: FiniteAnimationSpec<T>?,
    interruptionSpec: FiniteAnimationSpec<T>
): FiniteAnimationSpec<T> {
    val directionalSpec =
        if (targetState == EnterExitState.Visible) enterSpec else exitSpec
    return if (directionalSpec?.nativePreferredOriginalOrNull() != null) {
        directionalSpec
    } else {
        interruptionSpec
    }
}

/**
 * Keep an explicitly native enter transition available after it has settled.
 *
 * Compose normally clears this metadata at the stable Visible state. A quick Exit -> Enter
 * reversal can then observe the default interruption spec for alpha/scale before the enter
 * metadata is restored, while slide (resolved later during measurement) already sees the native
 * spec. Retaining only the fully supported native subset keeps the reverse group on one clock.
 */
internal fun EnterTransition.retainForNativeInterruptionOrNone(): EnterTransition {
    val hasVisualEffect =
        data.fade != null || data.scale != null || data.slide != null
    val allVisualEffectsAreNative =
        (data.fade?.animationSpec?.nativePreferredOriginalOrNull() != null || data.fade == null) &&
            (data.scale?.animationSpec?.nativePreferredOriginalOrNull() != null ||
                data.scale == null) &&
            (data.slide?.animationSpec?.nativePreferredOriginalOrNull() != null ||
                data.slide == null)
    return if (
        hasVisualEffect &&
        data.changeSize == null &&
        allVisualEffectsAreNative
    ) {
        this
    } else {
        EnterTransition.None
    }
}
