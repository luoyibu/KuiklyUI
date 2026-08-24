/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI.
 */

package com.tencent.kuikly.compose.animation.core

/**
 * Opt-in diagnostics for Native animation development.
 */
internal object NativeAnimationTrace {
    var enabled: Boolean = false

    fun log(message: () -> String) {
        if (enabled) {
            println("[NativeAnimation][Compose] ${message()}")
        }
    }
}
