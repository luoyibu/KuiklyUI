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

package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.directives.vforLazy
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * Demo page to reproduce VForLazy blank issue on iPad 26 after app drag/resize.
 *
 * Steps to reproduce:
 * 1. Run on iPad with iPadOS 26
 * 2. Scroll the list to see initial items
 * 3. Tap "Load More" button to add more items
 * 4. Drag the app window to resize (iPad 26 multitasking drag)
 * 5. Observe blank areas in the list
 */
@Page("VforLazyDragIssue")
internal class VforLazyDragIssuePage : BasePager() {

    private val itemList by observableList<String>()
    private var loadCount by observable(0)

    companion object {
        private const val ITEMS_PER_PAGE = 30
        private val COLORS = longArrayOf(
            0xFFE3F2FD, 0xFFFCE4EC, 0xFFF3E5F5, 0xFFE8F5E9,
            0xFFFFF3E0, 0xFFE0F7FA, 0xFFFFF9C4, 0xFFEFEBE9
        )
    }

    override fun created() {
        super.created()
        // Load initial batch of items
        loadMoreItems()
    }

    private fun loadMoreItems() {
        val startIndex = itemList.size
        val newItems = mutableListOf<String>()
        for (i in 0 until ITEMS_PER_PAGE) {
            newItems.add("Item ${startIndex + i}")
        }
        itemList.addAll(newItems)
        loadCount++
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = "VForLazy Drag Issue"
                    backDisable = false
                }
            }

            // Info bar showing current state
            View {
                attr {
                    flexDirectionRow()
                    justifyContentSpaceBetween()
                    alignItemsCenter()
                    padding(12f)
                    backgroundColor(0xFFF5F5F5)
                }
                Text {
                    attr {
                        fontSize(14f)
                        color(Color(0xFF666666))
                        text("Total: ${ctx.itemList.size} items | Loaded: ${ctx.loadCount} batches")
                    }
                }
                Button {
                    attr {
                        size(140f, 36f)
                        borderRadius(18f)
                        backgroundColor(0xFF2196F3)
                        titleAttr {
                            text("Load More (+$ITEMS_PER_PAGE)")
                            fontSize(13f)
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            ctx.loadMoreItems()
                        }
                    }
                }
            }

            // Main list with vforLazy
            List {
                attr {
                    flex(1f)
                }
                vforLazy({ ctx.itemList }) { item, index, count ->
                    View {
                        attr {
                            height(80f)
                            margin(left = 12f, right = 12f, top = 6f, bottom = 6f)
                            borderRadius(8f)
                            backgroundColor(Color(COLORS[index % COLORS.size]))
                            padding(12f)
                            justifyContentCenter()
                        }
                        Text {
                            attr {
                                fontSize(16f)
                                color(Color(0xFF333333))
                                text(item)
                            }
                        }
                        Text {
                            attr {
                                fontSize(12f)
                                color(Color(0xFF999999))
                                text("index=$index / total=$count")
                            }
                        }
                    }
                }
            }
        }
    }
}
