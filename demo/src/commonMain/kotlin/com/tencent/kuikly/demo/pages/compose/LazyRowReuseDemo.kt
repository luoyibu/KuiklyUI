package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyRow
import com.tencent.kuikly.compose.foundation.lazy.enableLazyListPrefetch
import com.tencent.kuikly.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.foundation.lazy.grid.GridCells
import com.tencent.kuikly.compose.foundation.lazy.grid.LazyHorizontalGrid
import com.tencent.kuikly.compose.foundation.pager.HorizontalPager
import com.tencent.kuikly.compose.foundation.pager.rememberPagerState
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.compose.extension.bouncesEnable
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.log.KLog

/**
 * ScrollerView 复用测试：覆盖 LazyRow / LazyHorizontalGrid / HorizontalPager 跨类型复用。
 *
 * 外层 LazyColumn 的 item 不设 contentType，三种容器按 rowIndex % 3 交替出现，
 * 滚动时 Compose 会在它们之间跨类型复用同一个 ScrollerView 原生实例。
 *
 * 每张卡片显示 "ROW.COL"，若数字与行号不匹配则说明复用出了问题。
 */
private const val LOG_TAG = "FirstScreenPerf"

/**
 * 首屏对比（iOS `pageLoadTime.firstPaintCost` / `[KuiklyFirstPaint]`）：
 * - A [FirstScreenMode.Beyond5]：外层 LazyColumn `beyondBoundsItemCount=5`
 * - B [FirstScreenMode.PrefetchCacheWindow]：`beyond=0` + prefetch + 一屏 CacheWindow
 *
 * 从 iOS **RootViewController** 两个按钮进入（App 已热身后跳转，避免冷启动污染）。
 * 也可 router 传 pageData `firstScreenMode`：`beyond5` | `prefetch_cache`。
 */
private enum class FirstScreenMode(val key: String) {
    Beyond5("beyond5"),
    PrefetchCacheWindow("prefetch_cache"),
    ;

    companion object {
        fun fromKey(key: String): FirstScreenMode =
            entries.find { it.key == key } ?: Beyond5
    }
}

@Page("LazyRowReuseDemo")
@OptIn(ExperimentalFoundationApi::class)
class LazyRowReuseDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                ScrollerReuseTest()
            }
        }
    }
}

// ——————————————— 辅助 ———————————————

private val bgHexColors = listOf("E53935", "43A047", "1E88E5", "FB8C00", "8E24AA", "00ACC1")

private fun numberImageUrl(row: Int, col: Int): String {
    val bg = bgHexColors[row % bgHexColors.size]
    return "https://dummyimage.com/120x120/$bg/ffffff.png&text=$row.$col"
}

private enum class ScrollType { ROW, GRID, PAGER }

private fun scrollTypeOf(rowIndex: Int): ScrollType = when (rowIndex % 3) {
    0 -> ScrollType.ROW
    1 -> ScrollType.GRID
    else -> ScrollType.PAGER
}

// ——————————————— 主体 ———————————————

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ScrollerReuseTest() {
    val pager = LocalActivity.current.getPager()
    val modeKey = pager.pageData.params.optString("firstScreenMode", FirstScreenMode.Beyond5.key)
    val mode = FirstScreenMode.fromKey(modeKey)
    val beyondBounds = if (mode == FirstScreenMode.Beyond5) 5 else 0
    val cacheWindow =
        remember {
            // 一屏 ahead：viewport 高度的 100%，behind 不预取
            LazyLayoutCacheWindow(aheadFraction = 1f, behindFraction = 0f)
        }
    val defaultListState = rememberLazyListState()
    val cacheWindowListState = rememberLazyListState(cacheWindow = cacheWindow)
    val listState =
        if (mode == FirstScreenMode.PrefetchCacheWindow) cacheWindowListState else defaultListState
    val columnModifier =
        Modifier
            .fillMaxSize()
            .then(
                if (mode == FirstScreenMode.PrefetchCacheWindow) {
                    Modifier.enableLazyListPrefetch()
                } else {
                    Modifier
                },
            )

    SideEffect {
        val line =
            "[$LOG_TAG] page=LazyRowReuseDemo mode=$modeKey " +
                "beyondBounds=$beyondBounds prefetch=${mode == FirstScreenMode.PrefetchCacheWindow} " +
                "cacheWindow=${if (mode == FirstScreenMode.PrefetchCacheWindow) "ahead1x" else "off"}"
        println(line)
        KLog.i(LOG_TAG, line)
    }

    LazyColumn(
        modifier = columnModifier,
        state = listState,
        beyondBoundsItemCount = beyondBounds,
    ) {
        item {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "首屏模式（进入前在 RootViewController 选择，改模式请返回重进）",
                    fontSize = 11.sp,
                    color = Color.Gray,
                )
                Text(
                    text = "mode=$modeKey beyond=$beyondBounds prefetch=${mode == FirstScreenMode.PrefetchCacheWindow}",
                    fontSize = 12.sp,
                    color = Color(0xFF333333),
                )
            }
        }

        // 不设 contentType → 默认 null → 三种类型之间会互相复用
        items(30) { rowIndex ->
            val type = scrollTypeOf(rowIndex)
            val bg = if (rowIndex % 2 == 0) Color.White else Color(0xFFF5F5F5)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bg)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 行号 + 类型标签
                Column(
                    modifier = Modifier.width(50.dp).padding(start = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "$rowIndex",
                        fontSize = 20.sp,
                        color = Color(0xFF333333),
                    )
                    Text(
                        text = type.name,
                        fontSize = 9.sp,
                        color = Color(0xFF999999),
                    )
                }

                when (type) {
                    ScrollType.ROW -> LazyRowSection(rowIndex)
                    ScrollType.GRID -> GridSection(rowIndex)
                    ScrollType.PAGER -> PagerSection(rowIndex)
                }
            }
        }
    }
}

// ——————————————— LazyRow ———————————————

@Composable
private fun LazyRowSection(rowIndex: Int) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().height(110.dp).bouncesEnable(false),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        items(20) { colIndex ->
            CardItem(rowIndex, colIndex)
        }
    }
}

// ——————————————— LazyHorizontalGrid ———————————————

@Composable
private fun GridSection(rowIndex: Int) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth().height(220.dp).bouncesEnable(false),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        items(20) { colIndex ->
            CardItem(rowIndex, colIndex)
        }
    }
}

// ——————————————— HorizontalPager ———————————————

@Composable
private fun PagerSection(rowIndex: Int) {
    val pagerState = rememberPagerState(pageCount = { 10 })
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth().height(110.dp).bouncesEnable(false),
        pageSpacing = 8.dp,
    ) { page ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CardItem(rowIndex, page)
        }
    }
}

// ——————————————— 通用卡片 ———————————————

@Composable
private fun CardItem(rowIndex: Int, colIndex: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = rememberAsyncImagePainter(numberImageUrl(rowIndex, colIndex)),
            contentDescription = "$rowIndex.$colIndex",
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
        Text(
            text = "$rowIndex.$colIndex",
            fontSize = 12.sp,
            color = Color(0xFF666666),
            modifier = Modifier.width(80.dp).padding(top = 4.dp),
            textAlign = TextAlign.Center,
        )
    }
}
