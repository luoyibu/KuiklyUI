/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI.
 */

package com.tencent.kuikly.compose.animation.core

/**
 * Temporary diagnostics for the Native animation bring-up. Keep every message on one line so a
 * group can be reconstructed by filtering `[NativeAnimation]` from the platform console.
 */
internal object NativeAnimationTrace {
    var enabled: Boolean = true

    fun log(message: () -> String) {
        if (enabled) {
            println("[NativeAnimation][Compose] ${message()}")
        }
    }
}
