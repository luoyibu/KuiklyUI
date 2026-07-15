/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.ComposeFoundationFlags
import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.gestures.scrollBy
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.lazy.enableLazyListPrefetch
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.semantics.semantics
import com.tencent.kuikly.compose.ui.semantics.testTag
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.log.KLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * LazyList 滑动性能压测页（可整文件复制到其他 Kuikly 工程的 demo 模块）。
 *
 * 路由：LazyListScrollPerfDemo
 *
 * 建议对比矩阵：
 * - prefetch ON（全局或 Modifier） vs OFF
 * - heavy OFF vs ON
 * - 手动快速 fling vs 自动匀速 scrollBy
 *
 * iOS FPS：`[KuiklyPerfReport]` 离页日志（见 docs/LazyListScrollPerf-接入说明.md）。
 *
 * 外工程接入：复制本文件 + 阅读 docs/LazyListScrollPerf-给外工程的Prompt.md
 */
private const val LOG_TAG = "ScrollPerf"
private const val DEFAULT_ITEM_COUNT = 500

/** Last UI toggles for exit logs (grep ScrollPerf + KuiklyPerfReport). */
internal object ScrollPerfSessionState {
    var prefetchOn: Boolean = true
    var heavyItems: Boolean = false
    var autoScroll: Boolean = false
    var scrollSpeedPxPerSec: Float = 300f
    var itemCount: Int = DEFAULT_ITEM_COUNT

    @OptIn(ExperimentalFoundationApi::class)
    fun logSnapshot(phase: String) {
        val globalPrefetch = ComposeFoundationFlags.isLazyListPrefetchEnabled
        val line =
            "[$LOG_TAG] phase=$phase items=$itemCount prefetch_modifier=$prefetchOn " +
                "prefetch_global=$globalPrefetch heavy=$heavyItems auto=$autoScroll " +
                "speed_px_s=${scrollSpeedPxPerSec.toInt()}"
        println(line)
        KLog.i(LOG_TAG, line)
    }
}

@Page("LazyListScrollPerfDemo")
@OptIn(ExperimentalFoundationApi::class)
class LazyListScrollPerfDemoPage : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        ScrollPerfSessionState.logSnapshot("page_enter")
        setContent {
            ComposeNavigationBar("LazyList 滑动性能") {
                LazyListScrollPerfContent()
            }
        }
    }

    override fun pageDidDisappear() {
        ScrollPerfSessionState.logSnapshot("page_did_disappear")
        super.pageDidDisappear()
    }

    override fun pageWillDestroy() {
        ScrollPerfSessionState.logSnapshot("page_will_destroy")
        super.pageWillDestroy()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyListScrollPerfContent() {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var itemCount by remember { mutableIntStateOf(DEFAULT_ITEM_COUNT) }
    var prefetchOn by remember { mutableStateOf(true) }
    var heavyItems by remember { mutableStateOf(false) }
    var autoScroll by remember { mutableStateOf(false) }
    var scrollSpeedPxPerSec by remember { mutableFloatStateOf(300f) }

    fun syncSessionAndLog(reason: String) {
        ScrollPerfSessionState.itemCount = itemCount
        ScrollPerfSessionState.prefetchOn = prefetchOn
        ScrollPerfSessionState.heavyItems = heavyItems
        ScrollPerfSessionState.autoScroll = autoScroll
        ScrollPerfSessionState.scrollSpeedPxPerSec = scrollSpeedPxPerSec
        ScrollPerfSessionState.logSnapshot(reason)
    }

    val items = remember(itemCount) { List(itemCount) { it } }

    LaunchedEffect(autoScroll, scrollSpeedPxPerSec, itemCount) {
        if (!autoScroll) return@LaunchedEffect
        println("$LOG_TAG autoScroll start speed=$scrollSpeedPxPerSec items=$itemCount prefetch=$prefetchOn heavy=$heavyItems")
        while (autoScroll) {
            delay(16)
            val pixelsPerFrame = scrollSpeedPxPerSec / 60f
            val layoutInfo = listState.layoutInfo
            val visible = layoutInfo.visibleItemsInfo
            if (visible.isNotEmpty() && visible.last().index >= layoutInfo.totalItemsCount - 1) {
                listState.scrollToItem(0)
            } else {
                listState.scroll { scrollBy(pixelsPerFrame) }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text(
            text = buildString {
                append("items=$itemCount prefetch=$prefetchOn heavy=$heavyItems ")
                append(if (autoScroll) "AUTO ${scrollSpeedPxPerSec.toInt()}px/s" else "手动 fling")
            },
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PerfChip("prefetch", prefetchOn, Color(0xFF4CAF50), Color(0xFF9E9E9E)) {
                prefetchOn = !prefetchOn
                syncSessionAndLog("prefetch_toggle")
            }
            PerfChip("heavy", heavyItems, Color(0xFFFF9800), Color(0xFFEEEEEE)) {
                heavyItems = !heavyItems
                syncSessionAndLog("heavy_toggle")
            }
            PerfChip("auto", autoScroll, Color(0xFF2196F3), Color(0xFFEEEEEE)) {
                autoScroll = !autoScroll
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(200f, 300f, 500f, 800f).forEach { speed ->
                Text(
                    text = "${speed.toInt()}",
                    modifier =
                        Modifier
                            .background(if (scrollSpeedPxPerSec == speed) Color(0xFF3F51B5) else Color(0xFFBDBDBD))
                            .clickable { scrollSpeedPxPerSec = speed }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Color.White,
                    fontSize = 11.sp,
                )
            }
            Text(
                text = "top",
                modifier =
                    Modifier
                        .background(Color(0xFF795548))
                        .clickable {
                            scope.launch {
                                listState.scrollToItem(0)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                color = Color.White,
                fontSize = 11.sp,
            )
        }

        val listModifier =
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .semantics { testTag = "scroll_perf_list" }
                .then(
                    if (prefetchOn) {
                        Modifier.enableLazyListPrefetch()
                    } else {
                        Modifier.enableLazyListPrefetch(false)
                    },
                )

        LazyColumn(
            state = listState,
            modifier = listModifier,
            beyondBoundsItemCount = 0,
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(items, key = { it }) { index ->
                ScrollPerfItem(index = index, heavy = heavyItems)
            }
        }
    }
}

@Composable
private fun PerfChip(
    label: String,
    on: Boolean,
    onColor: Color,
    offColor: Color,
    onClick: () -> Unit,
) {
    Text(
        text = "$label:${if (on) "ON" else "OFF"}",
        modifier =
            Modifier
                .background(if (on) onColor else offColor)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun ScrollPerfItem(
    index: Int,
    heavy: Boolean,
) {
    if (heavy) {
        LaunchedEffect(index) {
            var acc = 0
            repeat(300) { i ->
                acc += i * (index % 17 + 1)
            }
            delay(1)
            if (acc == Int.MIN_VALUE) {
                println("$LOG_TAG unreachable index=$index")
            }
        }
    }

    val hue = (index * 37) % 360
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(if (heavy) 96.dp else 64.dp)
                .background(Color.hsv(hue.toFloat(), 0.35f, 0.92f))
                .padding(12.dp),
    ) {
        Text(
            text = "Item $index${if (heavy) " [heavy]" else ""}",
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun Color.Companion.hsv(h: Float, s: Float, v: Float): Color {
    val c = v * s
    val x = c * (1 - kotlin.math.abs((h / 60f) % 2f - 1f))
    val m = v - c
    val (r, g, b) =
        when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
    return Color(r + m, g + m, b + m)
}
