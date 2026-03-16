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

package com.tencent.kuikly.android.demo

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tencent.kuikly.android.demo.adapter.KRAPNGViewAdapter
import com.tencent.kuikly.android.demo.adapter.KRColorParserAdapter
import com.tencent.kuikly.android.demo.adapter.KRFontAdapter
import com.tencent.kuikly.android.demo.adapter.KRImageAdapter
import com.tencent.kuikly.android.demo.adapter.KRLogAdapter
import com.tencent.kuikly.android.demo.adapter.KRRouterAdapter
import com.tencent.kuikly.android.demo.adapter.KRTextPostProcessorAdapter
import com.tencent.kuikly.android.demo.adapter.KRThreadAdapter
import com.tencent.kuikly.android.demo.adapter.KRUncaughtExceptionHandlerAdapter
import com.tencent.kuikly.android.demo.adapter.PAGViewAdapter
import com.tencent.kuikly.android.demo.adapter.VideoViewAdapter
import com.tencent.kuikly.core.render.android.KuiklyRenderView
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegator

/**
 * Activity that hosts a KuiklyRender page in a drag-resizable container.
 * Drag the blue corner handle or edge handles to resize in real-time,
 * simulating iPad 26 window drag/resize to reproduce VForLazy blank issue.
 */
class ResizableKuiklyActivity : AppCompatActivity() {

    private lateinit var hrContainerView: ViewGroup
    private lateinit var resizableContainer: FrameLayout
    private lateinit var sizeLabel: TextView
    private lateinit var cornerHandle: View
    private lateinit var rightHandle: View
    private lateinit var bottomHandle: View
    private lateinit var kuiklyRenderViewDelegator: KuiklyRenderViewBaseDelegator
    private lateinit var contextCodeHandler: ContextCodeHandler

    private val pageName = "VforLazyDragIssue"

    // Current container size in px
    private var containerW = 0
    private var containerH = 0

    // Constraints in dp
    private val minWidthDp = 250f
    private val minHeightDp = 350f
    private val defaultWidthDp = 300f
    private val defaultHeightDp = 500f

    // Drag state
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var dragStartW = 0
    private var dragStartH = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KuiklyRenderView.enableLazyClipChildren()
        contextCodeHandler = ContextCodeHandler(this, pageName)
        kuiklyRenderViewDelegator = contextCodeHandler.initContextHandler()

        setupAdapterManager()
        buildUI()
        setupImmersiveMode()

        // Open the Kuikly page
        val pageData = mutableMapOf<String, Any>(
            "appId" to 1,
            "sysLang" to resources.configuration.locale.language,
            "debug" to if (BuildConfig.DEBUG) 1 else 0
        )
        contextCodeHandler.openPage(hrContainerView, pageName, pageData)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildUI() {
        val density = resources.displayMetrics.density
        containerW = dpToPx(defaultWidthDp)
        containerH = dpToPx(defaultHeightDp)

        // Root layout
        val root = FrameLayout(this).apply {
            setBackgroundColor(0xFFF2F2F7.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Top bar
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(16f), dpToPx(48f), dpToPx(16f), dpToPx(8f))
        }

        sizeLabel = TextView(this).apply {
            typeface = android.graphics.Typeface.MONOSPACE
            textSize = 13f
            setTextColor(0xFF888888.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        topBar.addView(sizeLabel)

        val resetBtn = Button(this).apply {
            text = "Reset"
            setTextColor(Color.WHITE)
            isAllCaps = false
            backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2196F3.toInt())
            setPadding(dpToPx(16f), 0, dpToPx(16f), 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dpToPx(36f)
            )
            setOnClickListener { resetSize() }
        }
        topBar.addView(resetBtn)

        root.addView(topBar, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ))

        // Resizable container
        resizableContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.WHITE)
            elevation = dpToPx(4f).toFloat()
            clipChildren = true
            clipToPadding = true
        }

        // Kuikly render container inside
        hrContainerView = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        resizableContainer.addView(hrContainerView)

        root.addView(resizableContainer)

        // Corner drag handle (bottom-right, diagonal resize)
        cornerHandle = View(this).apply {
            val size = dpToPx(40f)
            layoutParams = FrameLayout.LayoutParams(size, size)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(0xFF2196F3.toInt())
            }
            alpha = 0.9f
        }
        root.addView(cornerHandle)

        // Right edge handle (horizontal resize)
        rightHandle = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(dpToPx(12f), dpToPx(60f))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dpToPx(6f).toFloat()
                setColor(0xFF5C6BC0.toInt())
            }
            alpha = 0.7f
        }
        root.addView(rightHandle)

        // Bottom edge handle (vertical resize)
        bottomHandle = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(dpToPx(60f), dpToPx(12f))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dpToPx(6f).toFloat()
                setColor(0xFF5C6BC0.toInt())
            }
            alpha = 0.7f
        }
        root.addView(bottomHandle)

        setContentView(root)

        // Setup drag gestures
        setupDrag(cornerHandle, dragBoth = true)
        setupDrag(rightHandle, dragWidth = true)
        setupDrag(bottomHandle, dragHeight = true)

        // Initial layout after view is laid out
        root.post { layoutAll() }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDrag(handle: View, dragWidth: Boolean = false, dragHeight: Boolean = false, dragBoth: Boolean = false) {
        handle.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dragStartX = event.rawX
                    dragStartY = event.rawY
                    dragStartW = containerW
                    dragStartH = containerH
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - dragStartX).toInt()
                    val dy = (event.rawY - dragStartY).toInt()
                    val newW = if (dragWidth || dragBoth) dragStartW + dx else containerW
                    val newH = if (dragHeight || dragBoth) dragStartH + dy else containerH
                    resizeTo(newW, newH)
                    true
                }
                else -> false
            }
        }
    }

    private fun resizeTo(w: Int, h: Int) {
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels
        val maxW = screenW - dpToPx(20f)
        val maxH = screenH - dpToPx(120f)

        containerW = w.coerceIn(dpToPx(minWidthDp), maxW)
        containerH = h.coerceIn(dpToPx(minHeightDp), maxH)
        layoutAll()
    }

    private fun resetSize() {
        containerW = dpToPx(defaultWidthDp)
        containerH = dpToPx(defaultHeightDp)
        layoutAll()
    }

    private fun layoutAll() {
        val screenW = resources.displayMetrics.widthPixels
        val topOffset = dpToPx(90f)

        // Container centered horizontally
        val containerX = (screenW - containerW) / 2
        resizableContainer.apply {
            val lp = layoutParams as FrameLayout.LayoutParams
            lp.width = containerW
            lp.height = containerH
            lp.leftMargin = containerX
            lp.topMargin = topOffset
            layoutParams = lp
        }

        // Corner handle at bottom-right of container
        val handleSize = dpToPx(40f)
        cornerHandle.apply {
            val lp = layoutParams as FrameLayout.LayoutParams
            lp.leftMargin = containerX + containerW - handleSize / 2
            lp.topMargin = topOffset + containerH - handleSize / 2
            layoutParams = lp
        }

        // Right handle at middle of right edge
        rightHandle.apply {
            val lp = layoutParams as FrameLayout.LayoutParams
            lp.leftMargin = containerX + containerW + dpToPx(4f)
            lp.topMargin = topOffset + containerH / 2 - dpToPx(30f)
            layoutParams = lp
        }

        // Bottom handle at middle of bottom edge
        bottomHandle.apply {
            val lp = layoutParams as FrameLayout.LayoutParams
            lp.leftMargin = containerX + containerW / 2 - dpToPx(30f)
            lp.topMargin = topOffset + containerH + dpToPx(4f)
            layoutParams = lp
        }

        sizeLabel.text = "Container: ${pxToDp(containerW)} × ${pxToDp(containerH)}"
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
    }

    private fun pxToDp(px: Int): Int {
        return (px / resources.displayMetrics.density).toInt()
    }

    override fun onResume() {
        super.onResume()
        kuiklyRenderViewDelegator.onResume()
    }

    override fun onPause() {
        super.onPause()
        kuiklyRenderViewDelegator.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        kuiklyRenderViewDelegator.onDetach()
    }

    private fun setupAdapterManager() {
        if (KuiklyRenderAdapterManager.krImageAdapter == null) {
            KuiklyRenderAdapterManager.krImageAdapter = KRImageAdapter(applicationContext)
        }
        if (KuiklyRenderAdapterManager.krLogAdapter == null) {
            KuiklyRenderAdapterManager.krLogAdapter = KRLogAdapter
        }
        if (KuiklyRenderAdapterManager.krUncaughtExceptionHandlerAdapter == null) {
            KuiklyRenderAdapterManager.krUncaughtExceptionHandlerAdapter = KRUncaughtExceptionHandlerAdapter
        }
        if (KuiklyRenderAdapterManager.krFontAdapter == null) {
            KuiklyRenderAdapterManager.krFontAdapter = KRFontAdapter
        }
        if (KuiklyRenderAdapterManager.krColorParseAdapter == null) {
            KuiklyRenderAdapterManager.krColorParseAdapter = KRColorParserAdapter(KRApplication.application)
        }
        if (KuiklyRenderAdapterManager.krRouterAdapter == null) {
            KuiklyRenderAdapterManager.krRouterAdapter = KRRouterAdapter()
        }
        if (KuiklyRenderAdapterManager.krThreadAdapter == null) {
            KuiklyRenderAdapterManager.krThreadAdapter = KRThreadAdapter()
        }
        if (KuiklyRenderAdapterManager.krPagViewAdapter == null) {
            KuiklyRenderAdapterManager.krPagViewAdapter = PAGViewAdapter()
        }
        if (KuiklyRenderAdapterManager.krAPNGViewAdapter == null) {
            KuiklyRenderAdapterManager.krAPNGViewAdapter = KRAPNGViewAdapter()
        }
        if (KuiklyRenderAdapterManager.krVideoViewAdapter == null) {
            KuiklyRenderAdapterManager.krVideoViewAdapter = VideoViewAdapter()
        }
        if (KuiklyRenderAdapterManager.krTextPostProcessorAdapter == null) {
            KuiklyRenderAdapterManager.krTextPostProcessorAdapter = KRTextPostProcessorAdapter()
        }
    }

    private fun setupImmersiveMode() {
        window.statusBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else if (Build.VERSION.SDK_INT >= 23) {
            window.decorView.apply {
                systemUiVisibility = systemUiVisibility or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }
}
