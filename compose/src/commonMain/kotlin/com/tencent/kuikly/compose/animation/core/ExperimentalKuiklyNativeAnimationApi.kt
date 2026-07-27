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

package com.tencent.kuikly.compose.animation.core

/**
 * Marks the opt-in API that delegates supported visual property animations to the native renderer.
 *
 * The API is deliberately opt-in. An animation that is not wrapped with [preferNative] continues
 * to use the existing Compose frame-by-frame animation implementation.
 */
@RequiresOptIn(
    message = "Native property animation is experimental and its supported property set may change."
)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalKuiklyNativeAnimationApi
