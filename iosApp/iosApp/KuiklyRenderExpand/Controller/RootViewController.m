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

#import "RootViewController.h"
#import "KuiklyRenderViewController.h"

static NSString * const kFirstScreenModeKey = @"firstScreenMode";
static NSString * const kModeBeyond5 = @"beyond5";
static NSString * const kModePrefetchCache = @"prefetch_cache";

@interface RootViewController ()
@property (nonatomic, weak) UIViewController *presentedKuiklyVc;
@end

@implementation RootViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    self.view.backgroundColor = [UIColor whiteColor];
    self.title = @"Kuikly Demo";

    UILabel *hint = [[UILabel alloc] initWithFrame:CGRectMake(20, 100, CGRectGetWidth(self.view.bounds) - 40, 80)];
    hint.numberOfLines = 0;
    hint.font = [UIFont systemFontOfSize:13];
    hint.textColor = [UIColor darkGrayColor];
    hint.text = @"先等 App 完全起来，再点下面按钮进入 LazyRowReuseDemo。\n首屏对比看日志 [KuiklyFirstPaint] / pageLoadTime.firstPaintCost。";
    [self.view addSubview:hint];

    [self addButton:@"Router 首页" y:200 color:[UIColor systemBlueColor] action:@selector(openRouter)];
    [self addButton:@"首屏 A · beyond=5" y:270 color:[UIColor systemGreenColor] action:@selector(openFirstScreenBeyond5)];
    [self addButton:@"首屏 B · prefetch + 1屏窗口" y:340 color:[UIColor systemOrangeColor] action:@selector(openFirstScreenPrefetch)];
}

- (void)addButton:(NSString *)title y:(CGFloat)y color:(UIColor *)color action:(SEL)action {
    CGFloat width = CGRectGetWidth(self.view.bounds) - 40;
    UIButton *button = [[UIButton alloc] initWithFrame:CGRectMake(20, y, width, 48)];
    [button setTitle:title forState:UIControlStateNormal];
    [button setTitleColor:[UIColor whiteColor] forState:UIControlStateNormal];
    button.backgroundColor = color;
    button.layer.cornerRadius = 8;
    button.titleLabel.font = [UIFont boldSystemFontOfSize:16];
    [button addTarget:self action:action forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:button];
}

- (void)openKuiklyPage:(NSString *)pageName pageData:(NSDictionary *)pageData {
    KuiklyRenderViewController *kuiklyVc =
        [[KuiklyRenderViewController alloc] initWithPageName:pageName pageData:pageData ?: @{}];
    kuiklyVc.modalPresentationStyle = UIModalPresentationFullScreen;
    self.presentedKuiklyVc = kuiklyVc;

    __weak typeof(self) weakSelf = self;
    [self presentViewController:kuiklyVc animated:YES completion:^{
        [weakSelf addNativeBackButtonOnViewController:kuiklyVc];
    }];
}

/// 原生返回按钮：叠在 Kuikly 页面右上角，dismiss 回 RootViewController。
- (void)addNativeBackButtonOnViewController:(UIViewController *)vc {
    CGFloat topInset = 0;
    if (@available(iOS 11.0, *)) {
        topInset = vc.view.safeAreaInsets.top;
    }
    if (topInset < 20) {
        topInset = 44; // 兜底：刘海/状态栏高度
    }

    UIButton *backBtn = [UIButton buttonWithType:UIButtonTypeSystem];
    backBtn.frame = CGRectMake(CGRectGetWidth(vc.view.bounds) - 88, topInset + 8, 72, 36);
    backBtn.autoresizingMask = UIViewAutoresizingFlexibleLeftMargin;
    [backBtn setTitle:@"返回" forState:UIControlStateNormal];
    [backBtn setTitleColor:[UIColor whiteColor] forState:UIControlStateNormal];
    backBtn.backgroundColor = [[UIColor blackColor] colorWithAlphaComponent:0.65];
    backBtn.layer.cornerRadius = 8;
    backBtn.titleLabel.font = [UIFont boldSystemFontOfSize:15];
    [backBtn addTarget:self action:@selector(dismissKuiklyPage) forControlEvents:UIControlEventTouchUpInside];
    backBtn.accessibilityIdentifier = @"native_back_button";
    [vc.view addSubview:backBtn];
    [vc.view bringSubviewToFront:backBtn];
}

- (void)dismissKuiklyPage {
    [self dismissViewControllerAnimated:YES completion:^{
        self.presentedKuiklyVc = nil;
    }];
}

- (void)openRouter {
    [self openKuiklyPage:@"router" pageData:@{}];
}

- (void)openFirstScreenBeyond5 {
    [self openKuiklyPage:@"LazyRowReuseDemo" pageData:@{ kFirstScreenModeKey: kModeBeyond5 }];
}

- (void)openFirstScreenPrefetch {
    [self openKuiklyPage:@"LazyRowReuseDemo" pageData:@{ kFirstScreenModeKey: kModePrefetchCache }];
}

@end
