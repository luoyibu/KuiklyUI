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

import router from '@ohos.router';

/**
 * Native 入口页：对齐 iOS RootViewController / Android FirstScreenLauncherActivity。
 * App 冷启动后先停在这里，再点按钮进入 LazyRowReuseDemo，首屏耗时不受冷启动干扰。
 *
 * A: firstScreenMode=beyond5
 * B: firstScreenMode=prefetch_cache
 */
@Entry
@Component
struct FirstScreenLauncher {
  private openKuiklyPage(pageName: string, pageData: Record<string, Object>) {
    router.pushUrl({
      url: 'pages/Index',
      params: {
        pageName,
        pageData
      }
    });
  }

  build() {
    Column() {
      Text('Kuikly Demo')
        .fontSize(22)
        .fontWeight(FontWeight.Bold)
        .fontColor('#222222')
        .width('100%')
        .margin({ top: 48, bottom: 16 })

      Text('先等 App 完全起来，再点下面按钮进入 LazyRowReuseDemo。\n首屏对比看 hilog：FirstScreenPerf / firstPaintCost。')
        .fontSize(13)
        .fontColor('#666666')
        .width('100%')
        .margin({ bottom: 32 })

      Button('Router 首页')
        .width('100%')
        .height(48)
        .fontSize(16)
        .backgroundColor('#2196F3')
        .margin({ bottom: 16 })
        .onClick(() => {
          this.openKuiklyPage('router', {});
        })

      Button('首屏 A · beyond=5')
        .width('100%')
        .height(48)
        .fontSize(16)
        .backgroundColor('#4CAF50')
        .margin({ bottom: 16 })
        .onClick(() => {
          this.openKuiklyPage('LazyRowReuseDemo', { 'firstScreenMode': 'beyond5' });
        })

      Button('首屏 B · prefetch + 1屏窗口')
        .width('100%')
        .height(48)
        .fontSize(16)
        .backgroundColor('#FF9800')
        .onClick(() => {
          this.openKuiklyPage('LazyRowReuseDemo', { 'firstScreenMode': 'prefetch_cache' });
        })
    }
    .width('100%')
    .height('100%')
    .padding(24)
    .backgroundColor(Color.White)
  }
}
