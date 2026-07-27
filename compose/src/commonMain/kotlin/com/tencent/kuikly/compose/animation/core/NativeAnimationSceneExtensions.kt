/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI.
 */

package com.tencent.kuikly.compose.animation.core

import com.tencent.kuikly.core.base.NativeAnimationBridge
import com.tencent.kuikly.core.manager.PagerManager

internal fun commitPendingNativeAnimationProperties() {
    NativeAnimationCoordinator.currentOrNull()?.commitStagedProperties()
}

internal fun destroyCurrentNativeAnimationCoordinator() {
    try {
        (PagerManager.getCurrentPager().getValueForKey(NativeAnimationBridge.PAGER_CACHE_KEY)
            as? NativeAnimationCoordinator)?.destroy()
    } catch (_: Throwable) {
        // The pager may already have been removed by its owner.
    }
}
