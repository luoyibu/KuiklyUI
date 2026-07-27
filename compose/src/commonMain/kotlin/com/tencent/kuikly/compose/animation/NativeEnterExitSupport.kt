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

    else -> spring()
}

internal fun FiniteAnimationSpec<IntOffset>.usesNativeGraphicsTranslation(): Boolean =
    nativePreferredOriginalOrNull() != null
