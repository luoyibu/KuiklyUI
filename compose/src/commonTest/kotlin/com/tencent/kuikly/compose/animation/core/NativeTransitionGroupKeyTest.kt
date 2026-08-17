/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI.
 */

package com.tencent.kuikly.compose.animation.core

import kotlin.test.Test
import kotlin.test.assertSame

class NativeTransitionGroupKeyTest {
    @Test
    fun childTransitionsShareTheRootNativeGroupKey() {
        val root = Transition(MutableTransitionState(false), label = "root")
        val child = Transition(MutableTransitionState(0), root, "child")
        val grandchild = Transition(MutableTransitionState("initial"), child, "grandchild")

        assertSame(root, child.nativeAnimationGroupKey)
        assertSame(root, grandchild.nativeAnimationGroupKey)
    }
}
