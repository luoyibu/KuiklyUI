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

#import "KuiklyRenderViewController.h"
#import "KuiklyRenderViewControllerBaseDelegator.h"
#import "KuiklyRenderContextProtocol.h"
#import "KuiklyRenderCore.h"
#import "KRPerformanceDataProtocol.h"
#import "KRPerformanceManager.h"
//#import <Bugly/Bugly.h>

#import "KRConvertUtil.h"
#import "UINavigationController+FDFullscreenPopGesture.h"
#import "KuiklyRenderBridge.h"

/// TurboDisplay 专属测试页面名称，只有该页面启用 TurboDisplay AOT 渲染
static NSString * const kTurboDisplayTestPageName = @"TurboDisplayAppLoadTestPage";


@interface Delegator  : NSObject<KRControllerDelegatorLifeCycleProtocol>


@end

@implementation Delegator
@synthesize delegator;
/// 对齐所在VC的viewDidLoad时机
- (void)viewDidLoad {
    NSLog(@"delegator:%s", __FUNCTION__);
}
/// kuiklyRenderView将要创建时调用
- (void)willInitRenderView {
    NSLog(@"delegator:%s", __FUNCTION__);
}
/// kuiklyRenderView创建完成后调用
- (void)didInitRenderView {
    NSLog(@"delegator:%s", __FUNCTION__);
}
/// kuiklyRenderView被成功发送事件时调用
- (void)didSendEvent:(NSString *)event {
    NSLog(@"delegator:%s", __FUNCTION__);
}
/// 对齐所在VC的viewWillAppear时机
- (void)viewWillAppear {
    NSLog(@"delegator:%s", __FUNCTION__);
}
/// 对齐所在VC的viewDidAppear时机
- (void)viewDidAppear {
    NSLog(@"delegator:%s", __FUNCTION__);
}
/// 对齐所在VC的viewWillDisappear时机
- (void)viewWillDisappear {
    NSLog(@"delegator:%s", __FUNCTION__);
}
/// 对齐所在VC的viewDidDisappear时机
- (void)viewDidDisappear {
    NSLog(@"delegator:%s", __FUNCTION__);
}
/// 将要获取上下文代码时回调
- (void)willFetchContextCode {
    NSLog(@"delegator:%s", __FUNCTION__);
}
/// 完成获取上下文代码时回调
- (void)didFetchContextCode {
    NSLog(@"delegator:%s", __FUNCTION__);
}
/// 内容完成出来时回调用(已上屏)
- (void)contentViewDidLoad {
    NSLog(@"delegator:%s", __FUNCTION__);
}
/// delegator dealloc时调用
- (void)delegatorDealloc {
    NSLog(@"delegator:%s", __FUNCTION__);
}

@end


#define KRWeakSelf __weak typeof(self) weakSelf = self;
@interface KuiklyRenderViewController()<KuiklyRenderViewControllerBaseDelegatorDelegate>

@property (nonatomic, strong) KuiklyRenderViewControllerBaseDelegator *delegator;

@end

@implementation KuiklyRenderViewController {
    NSString *_pageName;
    NSDictionary *_pageData;
    CFTimeInterval _beginTime;
    Delegator * _delegatorProxy;
}

- (instancetype)initWithPageName:(NSString *)pageName pageData:(NSDictionary *)pageData {
    if (self = [super init]) {
        _pageName = pageName;
        pageData = [self p_mergeExtParamsWithOriditalParam:pageData];
        _pageData = pageData;
        _delegatorProxy = [Delegator new];
        _delegator = [[KuiklyRenderViewControllerBaseDelegator alloc] initWithPageName:pageName pageData:pageData];
        [_delegator.performanceManager setMonitorType:KRMonitorType_ALL];
        _delegator.delegate = self;
        [_delegator addDelegatorLifeCycleListener:_delegatorProxy];
        
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(handleKuiklyException:) name:kKuiklyFatalExceptionNotification object:nil];
    }
    return self;
}

- (void)handleKuiklyException:(NSNotification *)noti {
    if (noti.userInfo && noti.userInfo[@"exception"]) {
        NSString *exceptionString = noti.userInfo[@"exception"];
        NSArray *components = [exceptionString componentsSeparatedByString:@"\n"];
        NSString *exceptionName = [components firstObject];
        NSArray<NSString *> *callStackArray = [components subarrayWithRange:NSMakeRange(1, components.count - 1)];
        // bugly上报示例
        // [Bugly reportExceptionWithCategory:7 name:exceptionName reason:exceptionName callStack:callStackArray extraInfo:@{} terminateApp:YES];
        NSLog(@"report Kuikly Exception: %@, stacks:%@", exceptionName, callStackArray);
    }
}

- (void)viewDidLoad {
    [super viewDidLoad];
    
    self.fd_prefersNavigationBarHidden = YES;
    self.view.backgroundColor = [UIColor whiteColor];
    [_delegator viewDidLoadWithView:self.view];
    [self.navigationController setNavigationBarHidden:YES animated:NO];

}


- (void)viewDidLayoutSubviews {
    [super viewDidLayoutSubviews];
    [_delegator viewDidLayoutSubviews];
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    [_delegator viewWillAppear];
    [self.navigationController setNavigationBarHidden:YES animated:NO];
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    [_delegator viewDidAppear];
    [self.navigationController setNavigationBarHidden:YES animated:NO];
}

- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
    [_delegator viewWillDisappear];
}

- (void)viewDidDisappear:(BOOL)animated {
    [super viewDidDisappear:animated];
    [_delegator viewDidDisappear];
    [self p_logPerformanceReportOnPageExit];
}

/// Structured FPS report for manual scroll perf tests (grep: KuiklyPerfReport).
- (void)p_logPerformanceReportOnPageExit {
    KRPerformanceManager *manager = [_delegator performanceManager];
    if (!manager) {
        NSLog(@"[KuiklyPerfReport] page=%@ error=performanceManager_nil", _pageName ?: @"");
        return;
    }

    KRFPSMonitor *mainFpsMonitor = [manager mainFPS];
    KRFPSMonitor *kotlinFpsMonitor = [manager kotlinFPS];
    KRMemoryMonitor *memMonitor = [manager memoryMonitor];
    NSDictionary *perfData = [manager performanceData];

    NSLog(@"[KuiklyPerfReport] event=page_exit page=%@ pageExistTimeMs=%.0f",
          _pageName ?: @"",
          [manager pageExistTime]);

    if (mainFpsMonitor) {
        NSLog(@"[KuiklyPerfReport] thread=main avg=%lu min=%lu max=%lu cur=%lu",
              (unsigned long)mainFpsMonitor.avgFPS,
              (unsigned long)mainFpsMonitor.minFPS,
              (unsigned long)mainFpsMonitor.maxFPS,
              (unsigned long)mainFpsMonitor.curFPS);
    } else {
        NSLog(@"[KuiklyPerfReport] thread=main error=monitor_nil");
    }

    if (kotlinFpsMonitor) {
        NSLog(@"[KuiklyPerfReport] thread=kotlin avg=%lu min=%lu max=%lu cur=%lu",
              (unsigned long)kotlinFpsMonitor.avgFPS,
              (unsigned long)kotlinFpsMonitor.minFPS,
              (unsigned long)kotlinFpsMonitor.maxFPS,
              (unsigned long)kotlinFpsMonitor.curFPS);
    } else {
        NSLog(@"[KuiklyPerfReport] thread=kotlin error=monitor_nil");
    }

    if (memMonitor) {
        NSLog(@"[KuiklyPerfReport] memory avgIncrementMB=%.2f peakIncrementMB=%.2f appAvgMB=%.2f appPeakMB=%.2f",
              [memMonitor avgIncrementMemory] / 1024.0 / 1024.0,
              [memMonitor peakIncrementMemory] / 1024.0 / 1024.0,
              [memMonitor appAvgMemory] / 1024.0 / 1024.0,
              [memMonitor appPeakMemory] / 1024.0 / 1024.0);
    }

    NSLog(@"[KuiklyPerfReport] performanceData=%@", perfData ?: @{});
    NSLog(@"[KuiklyPerfReport] stageStartTimes=%@", manager.stageStartTimes ?: @{});
    NSLog(@"[KuiklyPerfReport] stageDurations=%@", manager.stageDurations ?: @{});
}


- (void)renderViewDidCreated {
    _beginTime = CFAbsoluteTimeGetCurrent();
}

- (void)onUnhandledException:(NSString *)exReason stack:(NSString *)callstackStr mode:(KuiklyContextMode)mode
{
    // report to bugly
}

- (void)onGetPerformanceData{
    id<KRPerformanceDataProtocol> performance = _delegator.performanceManager;
    // 获取performance相关信息
    NSDictionary *data = [performance performanceData];
    NSLog(@"onGetPerformanceData performance data:%@", data);
}

- (void)onPageLoadComplete:(BOOL)isSucceed error:(NSError *)error mode:(KuiklyContextMode)mode {
    if (error) {
        
    }
    
    id<KRPerformanceDataProtocol> performance = _delegator.performanceManager;
    // 获取performance相关信息
    NSDictionary *data = [performance performanceData];
    NSDictionary *pageLoadTime = data[@"pageLoadTime"];
    NSLog(@"onPageLoadComplete performance data:%@", data);
    NSLog(@"[KuiklyFirstPaint] event=page_load_complete page=%@ succeed=%d "
          @"firstPaintCost=%@ renderCost=%@ pageBuildCost=%@ pageLayoutCost=%@ createPageCost=%@ "
          @"initRenderContextCost=%@ newPageCost=%@ initViewCost=%@",
          _pageName ?: @"",
          isSucceed ? 1 : 0,
          pageLoadTime[@"firstPaintCost"] ?: @(-1),
          pageLoadTime[@"renderCost"] ?: @(-1),
          pageLoadTime[@"pageBuildCost"] ?: @(-1),
          pageLoadTime[@"pageLayoutCost"] ?: @(-1),
          pageLoadTime[@"createPageCost"] ?: @(-1),
          pageLoadTime[@"initRenderContextCost"] ?: @(-1),
          pageLoadTime[@"newPageCost"] ?: @(-1),
          pageLoadTime[@"initViewCost"] ?: @(-1));
}

#pragma mark - private

- (NSDictionary *)p_mergeExtParamsWithOriditalParam:(NSDictionary *)pageParam {
    NSMutableDictionary *mParam = [(pageParam ?: @{}) mutableCopy];
 
    return mParam;
}

#pragma mark - KuiklyRenderViewControllerDelegatorDelegate

- (UIView *)createLoadingView {
    UIView *loadingView = [[UIView alloc] init];
    loadingView.backgroundColor = [UIColor whiteColor];
    return loadingView;
}


- (UIView *)createErrorView {
    UIView *errorView = [[UIView alloc] init];
    errorView.backgroundColor = [UIColor whiteColor];
    return errorView;
}

- (void)fetchContextCodeWithPageName:(NSString *)pageName resultCallback:(KuiklyContextCodeCallback)callback {
    if (callback) {
        // 返回对应framework名字
        callback(@"shared", nil);
    }
}


- (void)contentViewDidLoad {
    CFTimeInterval pageCostMs = (CFAbsoluteTimeGetCurrent() - _beginTime) * 1000.0;
    NSLog(@"pageCostTime:%.2lf", pageCostMs);
    // firstPaintCost 在 performanceManager contentViewDidLoad 之后才写入，以 page_load_complete 为准。
    NSLog(@"[KuiklyFirstPaint] event=content_view_did_load page=%@ pageCostTimeMs=%.0f",
          _pageName ?: @"",
          pageCostMs);
}

- (void)dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}


- (NSDictionary<NSString *,NSObject *> *)contextPageData {
    NSMutableDictionary *pageData = [NSMutableDictionary dictionary];
    pageData[@"appId"] = @"1";
    pageData[@"sysLang"] = [[NSLocale preferredLanguages] firstObject];
    return pageData;
}


// 仅允许指定的测试页面走 TurboDisplay 渲染路径
// 避免新安装无缓存时弹出错误弹窗影响其他业务页面的体验
- (NSString *)turboDisplayKey {
    if ([_pageName isEqualToString:kTurboDisplayTestPageName]) {
        return _pageName;
    }
    return nil;
}

- (KRTurboDisplayConfig *)configureTurboDisplay {
    if ([_pageName isEqualToString:kTurboDisplayTestPageName]) {
        KRTurboDisplayConfig *config = [[KRTurboDisplayConfig alloc] init];
        // Demo 页面支持滚动位置恢复，则需要打开 延迟Diff
        [config enableDelayedDiff];
    //    [config enableAutoUpdateTurboDisplay];
    //    [config disablePersistentRealTree];
        return config;
      
    }
    return nil;
}


/// 控制 Native -> Kotlin 事件是否以同步方式发送，避免事件异步派发后晚于当前原生布局/上屏时机生效。
/// 典型场景：页面旋转、分屏或宿主容器尺寸突变时，可对 `rootViewSizeDidChanged` 返回 YES，避免尺寸变化晚一帧生效导致白屏。
- (BOOL)syncSendEvent:(NSString *)event {
    // 若页面尺寸发生变化时，出现白屏，可恢复以下被注释的代码
    if ([event isEqualToString:@"rootViewSizeDidChanged"]) {
        return YES;
    }
    
    return NO;
}


@end
