/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI.
 */

#import "UIView+CSS.h"

NS_ASSUME_NONNULL_BEGIN

FOUNDATION_EXPORT BOOL KRParseNativeAnimationV2(
    NSArray<NSString *> *parts,
    NSString * _Nullable * _Nullable kind,
    NSArray<NSNumber *> * _Nullable * _Nullable values
);

FOUNDATION_EXPORT CGFloat KRNativeAnimationV2Progress(
    NSString *kind,
    NSArray<NSNumber *> *values,
    CGFloat fraction
);

FOUNDATION_EXPORT NSUInteger KRNativeAnimationV2TransformSampleCount(NSTimeInterval duration);

FOUNDATION_EXPORT BOOL KRPerformNativeAnimationV2(
    NSString *kind,
    NSArray<NSNumber *> *values,
    NSTimeInterval duration,
    NSTimeInterval delay,
    void (^animations)(void),
    void (^ _Nullable completion)(BOOL finished)
);

NS_ASSUME_NONNULL_END
