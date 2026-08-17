/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI.
 */

package com.tencent.kuikly.compose.animation.core

import com.tencent.kuikly.core.base.Attr
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NativeAnimationDefaultInitialValueTest {
    @Test
    fun unsetTransformStartsFromIdentity() {
        assertEquals(
            "0.0|1.0 1.0|0.0 0.0|0.5 0.5|0.0 0.0|0.0 0.0",
            nativeDefaultInitialValue(Attr.StyleConst.TRANSFORM)
        )
    }

    @Test
    fun unsetOpacityStartsOpaque() {
        assertEquals(1f, nativeDefaultInitialValue(Attr.StyleConst.OPACITY))
    }

    @Test
    fun propertiesWithoutAWellDefinedDefaultStayUnset() {
        assertNull(nativeDefaultInitialValue(Attr.StyleConst.BACKGROUND_COLOR))
    }
}
