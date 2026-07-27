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

package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.animation.AnimatedVisibility
import com.tencent.kuikly.compose.animation.Crossfade
import com.tencent.kuikly.compose.animation.animateColor
import com.tencent.kuikly.compose.animation.core.Animatable
import com.tencent.kuikly.compose.animation.core.CubicBezierEasing
import com.tencent.kuikly.compose.animation.core.ExperimentalKuiklyNativeAnimationApi
import com.tencent.kuikly.compose.animation.core.Spring
import com.tencent.kuikly.compose.animation.core.animateFloat
import com.tencent.kuikly.compose.animation.core.animateFloatAsState
import com.tencent.kuikly.compose.animation.core.preferNative
import com.tencent.kuikly.compose.animation.core.spring
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.animation.core.updateTransition
import com.tencent.kuikly.compose.animation.fadeIn
import com.tencent.kuikly.compose.animation.fadeOut
import com.tencent.kuikly.compose.animation.scaleIn
import com.tencent.kuikly.compose.animation.scaleOut
import com.tencent.kuikly.compose.animation.slideInVertically
import com.tencent.kuikly.compose.animation.slideOutVertically
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.graphicsLayer
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Page("NativeAnimationDemo")
@OptIn(ExperimentalKuiklyNativeAnimationApi::class)
class NativeAnimationDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar("Native 属性动画") {
                NativeAnimationDemoContent()
            }
        }
    }
}

@OptIn(ExperimentalKuiklyNativeAnimationApi::class)
@Composable
private fun NativeAnimationDemoContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F5F8))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Native 属性动画验证",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "每组都可重复快速点击，用于观察完成回调、中断连续性和重组繁忙时的流畅度。",
                fontSize = 13.sp,
                color = Color.DarkGray,
                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
            )
        }
        item { ComposeNativeComparisonDemo() }
        item { NativeTransitionDemo() }
        item { NativeVisibilityDemo() }
        item { NativeAnimatableDemo() }
        item { NativeCrossfadeDemo() }
        item { NativeAnimationStressDemo() }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@OptIn(ExperimentalKuiklyNativeAnimationApi::class)
@Composable
private fun ComposeNativeComparisonDemo() {
    DemoSection(
        title = "1. animateAsState：Compose / Native 对照",
        description = "两块同时执行同一 alpha 动画；Native 完成次数来自 finishedListener。"
    ) {
        var opaque by remember { mutableStateOf(true) }
        var nativeFinishedCount by remember { mutableIntStateOf(0) }
        val composeAlpha by animateFloatAsState(
            targetValue = if (opaque) 1f else 0.15f,
            animationSpec = tween(1000, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)),
            label = "composeAlpha"
        )
        val nativeAlpha by animateFloatAsState(
            targetValue = if (opaque) 1f else 0.15f,
            animationSpec = tween<Float>(
                1000,
                easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
            ).preferNative(),
            label = "nativeAlpha",
            finishedListener = { nativeFinishedCount++ }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LabeledAnimatedBox("Compose", composeAlpha)
            LabeledAnimatedBox("Native", nativeAlpha)
        }
        DemoButton("切换 alpha") { opaque = !opaque }
        Text("Native finishedListener：$nativeFinishedCount 次", fontSize = 12.sp)
    }
}

private enum class NativeCardState { Start, End }

@OptIn(ExperimentalKuiklyNativeAnimationApi::class)
@Composable
private fun NativeTransitionDemo() {
    DemoSection(
        title = "2. updateTransition：同 View 多属性",
        description = "alpha、scale、rotation、纯色背景共享同一 Native 动画组。"
    ) {
        var state by remember { mutableStateOf(NativeCardState.Start) }
        val transition = updateTransition(state, label = "nativeCardTransition")
        val spec = tween<Float>(
            durationMillis = 900,
            easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
        ).preferNative()
        val alpha by transition.animateFloat({ spec }, label = "alpha") {
            if (it == NativeCardState.End) 0.45f else 1f
        }
        val scale by transition.animateFloat({ spec }, label = "scale") {
            if (it == NativeCardState.End) 1.35f else 0.8f
        }
        val rotation by transition.animateFloat({ spec }, label = "rotation") {
            if (it == NativeCardState.End) 135f else 0f
        }
        val backgroundColor by transition.animateColor(
            transitionSpec = {
                tween<Color>(
                    900,
                    easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
                ).preferNative()
            },
            label = "backgroundColor"
        ) {
            if (it == NativeCardState.End) Color(0xFFE91E63) else Color(0xFF2196F3)
        }

        Box(
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer {
                    this.alpha = alpha
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation
                }
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Text("Transition", color = Color.White, fontSize = 12.sp)
        }
        DemoButton("切换多属性") {
            state = if (state == NativeCardState.Start) {
                NativeCardState.End
            } else {
                NativeCardState.Start
            }
        }
    }
}

@OptIn(ExperimentalKuiklyNativeAnimationApi::class)
@Composable
private fun NativeVisibilityDemo() {
    DemoSection(
        title = "3. AnimatedVisibility：fade + scale + slide",
        description = "快速执行 show / hide / show，检查退出节点保留和反向中断。"
    ) {
        var visible by remember { mutableStateOf(true) }
        val enterFloatSpec = tween<Float>(800).preferNative()
        val exitFloatSpec = tween<Float>(800).preferNative()
        val enterOffsetSpec = tween<com.tencent.kuikly.compose.ui.unit.IntOffset>(800).preferNative()
        val exitOffsetSpec = tween<com.tencent.kuikly.compose.ui.unit.IntOffset>(800).preferNative()

        Box(
            modifier = Modifier.fillMaxWidth().height(130.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(enterFloatSpec) +
                    scaleIn(enterFloatSpec, initialScale = 0.6f) +
                    slideInVertically(enterOffsetSpec) { it / 2 },
                exit = fadeOut(exitFloatSpec) +
                    scaleOut(exitFloatSpec, targetScale = 0.6f) +
                    slideOutVertically(exitOffsetSpec) { it / 2 }
            ) {
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(96.dp)
                        .background(Color(0xFF4CAF50)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Native 弹窗内容", color = Color.White)
                }
            }
        }
        DemoButton(if (visible) "Hide" else "Show") { visible = !visible }
    }
}

@OptIn(ExperimentalKuiklyNativeAnimationApi::class)
@Composable
private fun NativeAnimatableDemo() {
    DemoSection(
        title = "4. Animatable.animateTo：Spring 与中断",
        description = "连续点击会替换同属性动画，并从 Native 当前呈现位置继续。"
    ) {
        val translation = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()
        var moveRight by remember { mutableStateOf(true) }
        Box(modifier = Modifier.fillMaxWidth().height(70.dp)) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .graphicsLayer { translationX = translation.value }
                    .background(Color(0xFFFF9800))
            )
        }
        DemoButton("Spring 移动 / 反向") {
            val target = if (moveRight) 220f else 0f
            moveRight = !moveRight
            scope.launch {
                translation.animateTo(
                    targetValue = target,
                    animationSpec = spring<Float>(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ).preferNative()
                )
            }
        }
    }
}

@OptIn(ExperimentalKuiklyNativeAnimationApi::class)
@Composable
private fun NativeCrossfadeDemo() {
    DemoSection(
        title = "5. Crossfade",
        description = "Crossfade 的内容 alpha 由 Native View 执行。"
    ) {
        var page by remember { mutableIntStateOf(0) }
        Crossfade(
            targetState = page,
            animationSpec = tween<Float>(700).preferNative(),
            label = "nativeCrossfade"
        ) { current ->
            Box(
                modifier = Modifier.fillMaxWidth().height(90.dp).background(
                    if (current % 2 == 0) Color(0xFF673AB7) else Color(0xFF009688)
                ),
                contentAlignment = Alignment.Center
            ) {
                Text("Page ${current % 2 + 1}", color = Color.White, fontSize = 20.sp)
            }
        }
        DemoButton("切换内容") { page++ }
    }
}

@OptIn(ExperimentalKuiklyNativeAnimationApi::class)
@Composable
private fun NativeAnimationStressDemo() {
    DemoSection(
        title = "6. 重组压力测试",
        description = "开启负载后持续制造 Compose 重组，再启动 1600ms Native 位移动画。"
    ) {
        var loadEnabled by remember { mutableStateOf(false) }
        var targetRight by remember { mutableStateOf(false) }
        var stressTick by remember { mutableIntStateOf(0) }
        LaunchedEffect(loadEnabled) {
            while (loadEnabled) {
                stressTick++
                delay(16)
            }
        }

        var checksum = stressTick
        if (loadEnabled) {
            repeat(180_000) { index ->
                checksum = (checksum * 1_664_525 + 1_013_904_223) xor index
            }
        }
        val translation by animateFloatAsState(
            targetValue = if (targetRight) 220f else 0f,
            animationSpec = tween<Float>(1600).preferNative(),
            label = "nativeStressSlide"
        )

        Text(
            text = "负载：${if (loadEnabled) "ON" else "OFF"}，校验值：${checksum and 0xFFFF}",
            fontSize = 12.sp
        )
        Box(modifier = Modifier.fillMaxWidth().height(66.dp)) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .graphicsLayer { translationX = translation }
                    .background(Color(0xFFF44336))
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { loadEnabled = !loadEnabled }) {
                Text(if (loadEnabled) "关闭负载" else "开启负载")
            }
            Button(onClick = { targetRight = !targetRight }) {
                Text("启动动画")
            }
        }
    }
}

@Composable
private fun DemoSection(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(description, fontSize = 12.sp, color = Color.DarkGray)
        content()
    }
}

@Composable
private fun LabeledAnimatedBox(label: String, alpha: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer { this.alpha = alpha }
                .background(Color(0xFF03A9F4))
        )
        Text(label, fontSize = 12.sp)
    }
}

@Composable
private fun DemoButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text)
    }
}
