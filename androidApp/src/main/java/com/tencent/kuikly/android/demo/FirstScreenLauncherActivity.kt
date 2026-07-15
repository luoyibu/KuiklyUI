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

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

/**
 * Native 入口页：对齐 iOS RootViewController。
 * App 冷启动后先停在这里，再点按钮进入 LazyRowReuseDemo，首屏耗时不受冷启动干扰。
 *
 * A: firstScreenMode=beyond5
 * B: firstScreenMode=prefetch_cache
 */
class FirstScreenLauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_screen_launcher)
        title = "Kuikly Demo"

        findViewById<Button>(R.id.btn_router).setOnClickListener {
            openKuiklyPage("router", JSONObject())
        }
        findViewById<Button>(R.id.btn_first_screen_a).setOnClickListener {
            openKuiklyPage(
                "LazyRowReuseDemo",
                JSONObject().put(KEY_FIRST_SCREEN_MODE, MODE_BEYOND5),
            )
        }
        findViewById<Button>(R.id.btn_first_screen_b).setOnClickListener {
            openKuiklyPage(
                "LazyRowReuseDemo",
                JSONObject().put(KEY_FIRST_SCREEN_MODE, MODE_PREFETCH_CACHE),
            )
        }
    }

    private fun openKuiklyPage(pageName: String, pageData: JSONObject) {
        KuiklyRenderActivity.start(this, pageName, pageData)
    }

    companion object {
        const val KEY_FIRST_SCREEN_MODE = "firstScreenMode"
        const val MODE_BEYOND5 = "beyond5"
        const val MODE_PREFETCH_CACHE = "prefetch_cache"
    }
}
