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

package com.tencent.kuikly.core.render.android.css.ktx

import android.util.ArrayMap
import android.view.View
import com.tencent.kuikly.core.render.android.const.KRCssConst
import com.tencent.kuikly.core.render.android.css.animation.KRCSSAnimation

/**
 * Commits queued descriptors together while preserving running animations on unrelated
 * properties of the same View.
 */
internal fun View.commitHRAnimationsWithPropertyIsolation() {
    val queue =
        getViewData<ArrayMap<Int, KRCSSAnimation>>(KRCssConst.ANIMATION_QUEUE)?.toMap()
            ?: return
    val propertiesBeingStarted = queue.values
        .filterNot { it.isPlaying() }
        .flatMapTo(mutableSetOf()) { it.animatedPropertyKeys() }
    queue.forEach { (_, animation) ->
        if (animation.isPlaying()) {
            if (animation.animatedPropertyKeys().any { it in propertiesBeingStarted }) {
                animation.cancelAnimation()
                animation.removeFromAnimationQueue()
            }
        } else {
            animation.commitAnimation()
        }
    }
}

