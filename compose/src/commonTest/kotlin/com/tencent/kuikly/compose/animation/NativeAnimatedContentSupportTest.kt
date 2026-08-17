/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI.
 */

package com.tencent.kuikly.compose.animation

import com.tencent.kuikly.compose.animation.core.nativePreferredOriginalOrNull
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.ui.unit.IntOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NativeAnimatedContentSupportTest {
    @Test
    fun navHostPromotesPureComposeSlideToNative() {
        val slideSpec = tween<IntOffset>(300)
        val transform = slideInHorizontally(slideSpec) togetherWith
            slideOutHorizontally(slideSpec)

        val nativeTransform = transform.preferNativeNavHostSlide()

        assertNull(nativeTransform.sizeTransform)
        assertNotNull(
            nativeTransform.targetContentEnter.data.slide
                ?.animationSpec
                ?.nativePreferredOriginalOrNull()
        )
        assertNotNull(
            nativeTransform.initialContentExit.data.slide
                ?.animationSpec
                ?.nativePreferredOriginalOrNull()
        )
    }

    @Test
    fun navHostPromotesSlideAndFadeTogether() {
        val slideSpec = tween<IntOffset>(300)
        val transform =
            (slideInHorizontally(slideSpec) + fadeIn(tween(300))) togetherWith
                (slideOutHorizontally(slideSpec) + fadeOut(tween(300)))

        val nativeTransform = transform.preferNativeNavHostSlide()

        assertNull(nativeTransform.sizeTransform)
        assertNotNull(
            nativeTransform.targetContentEnter.data.slide
                ?.animationSpec
                ?.nativePreferredOriginalOrNull()
        )
        assertNotNull(
            nativeTransform.targetContentEnter.data.fade
                ?.animationSpec
                ?.nativePreferredOriginalOrNull()
        )
        assertNotNull(
            nativeTransform.initialContentExit.data.fade
                ?.animationSpec
                ?.nativePreferredOriginalOrNull()
        )
    }

    @Test
    fun navHostLeavesSlideAndScaleOnComposeClock() {
        val slideSpec = tween<IntOffset>(300)
        val transform =
            (slideInHorizontally(slideSpec) + scaleIn(tween(300))) togetherWith
                slideOutHorizontally(slideSpec)

        val unchanged = transform.preferNativeNavHostSlide()

        assertNotNull(unchanged.sizeTransform)
        assertNull(
            unchanged.targetContentEnter.data.slide
                ?.animationSpec
                ?.nativePreferredOriginalOrNull()
        )
    }

    @Test
    fun navHostSlideUsesBackStackZIndex() {
        val transform = slideInHorizontally() togetherWith slideOutHorizontally()

        transform.applyNavHostSlideZIndex(targetZIndex = 7f, initialTargetZIndex = 6f)

        assertEquals(7f, transform.targetContentZIndex)
        assertEquals(6f, transform.initialTargetContentZIndex)
        assertEquals(
            6f,
            transform.preferNativeNavHostSlide().initialTargetContentZIndex
        )
    }

    @Test
    fun navHostFadeKeepsAnimatedContentDefaultZIndex() {
        val transform = fadeIn() togetherWith fadeOut()

        transform.applyNavHostSlideZIndex(7f)

        assertEquals(0f, transform.targetContentZIndex)
        assertNull(transform.initialTargetContentZIndex)
    }
}
