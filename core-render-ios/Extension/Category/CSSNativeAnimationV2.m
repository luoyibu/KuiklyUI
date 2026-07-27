/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI.
 */

#import "CSSNativeAnimationV2.h"
#import <objc/runtime.h>

BOOL KRParseNativeAnimationV2(
    NSArray<NSString *> *parts,
    NSString **kind,
    NSArray<NSNumber *> **values
) {
    for (NSString *part in parts) {
        if (![part hasPrefix:@"v2,"]) {
            continue;
        }
        NSArray<NSString *> *payload = [part componentsSeparatedByString:@","];
        if (payload.count < 2) {
            continue;
        }
        if (kind) {
            *kind = payload[1];
        }
        if (values) {
            NSMutableArray<NSNumber *> *parsedValues = [NSMutableArray array];
            for (NSUInteger index = 2; index < payload.count; index++) {
                [parsedValues addObject:@([payload[index] doubleValue])];
            }
            *values = parsedValues;
        }
        return YES;
    }
    return NO;
}

#if !TARGET_OS_OSX

static const void *KRNativePropertyAnimatorsKey = &KRNativePropertyAnimatorsKey;

static NSMutableDictionary<NSString *, UIViewPropertyAnimator *> *
KRNativePropertyAnimators(UIView *view) {
    NSMutableDictionary *animators =
        objc_getAssociatedObject(view, KRNativePropertyAnimatorsKey);
    if (!animators) {
        animators = [NSMutableDictionary dictionary];
        objc_setAssociatedObject(
            view,
            KRNativePropertyAnimatorsKey,
            animators,
            OBJC_ASSOCIATION_RETAIN_NONATOMIC
        );
    }
    return animators;
}

static void KRCancelNativePropertyAnimator(UIView *view, NSString *propertyKey, BOOL keepCurrentValue) {
    NSMutableDictionary *animators = KRNativePropertyAnimators(view);
    UIViewPropertyAnimator *previousAnimator = animators[propertyKey];
    if (previousAnimator && previousAnimator.state == UIViewAnimatingStateActive) {
        [previousAnimator stopAnimation:!keepCurrentValue];
        if (keepCurrentValue) {
            [previousAnimator finishAnimationAtPosition:UIViewAnimatingPositionCurrent];
        }
    }
    [animators removeObjectForKey:propertyKey];
}

#endif

BOOL KRPerformNativeAnimationV2(
    UIView *view,
    NSString *propertyKey,
    NSString *kind,
    NSArray<NSNumber *> *values,
    NSTimeInterval duration,
    NSTimeInterval delay,
    void (^animations)(void),
    void (^completion)(BOOL finished)
) {
#if TARGET_OS_OSX
    return NO;
#else
    if ([kind isEqualToString:@"snap"]) {
        KRCancelNativePropertyAnimator(view, propertyKey, NO);
        [UIView animateWithDuration:0
                             delay:delay
                           options:UIViewAnimationOptionAllowUserInteraction
                        animations:animations
                        completion:completion];
        return YES;
    }

    id<UITimingCurveProvider> timingParameters = nil;
    if ([kind isEqualToString:@"cubic"] && values.count == 4) {
        timingParameters = [[UICubicTimingParameters alloc]
            initWithControlPoint1:CGPointMake(values[0].doubleValue, values[1].doubleValue)
                    controlPoint2:CGPointMake(values[2].doubleValue, values[3].doubleValue)];
    } else if ([kind isEqualToString:@"spring"] && values.count >= 3) {
        CGFloat stiffness = values[0].doubleValue;
        CGFloat dampingRatio = values[1].doubleValue;
        CGFloat initialVelocity = values[2].doubleValue;
        CGFloat damping = 2.0 * dampingRatio * sqrt(stiffness);
        timingParameters = [[UISpringTimingParameters alloc]
            initWithMass:1.0
               stiffness:stiffness
                 damping:damping
         initialVelocity:CGVectorMake(initialVelocity, initialVelocity)];
    }
    if (!timingParameters) {
        return NO;
    }

    KRCancelNativePropertyAnimator(view, propertyKey, YES);
    NSMutableDictionary *animators = KRNativePropertyAnimators(view);
    UIViewPropertyAnimator *propertyAnimator = [[UIViewPropertyAnimator alloc]
        initWithDuration:duration timingParameters:timingParameters];
    animators[propertyKey] = propertyAnimator;
    [propertyAnimator addAnimations:animations];
    __weak UIView *weakView = view;
    __weak UIViewPropertyAnimator *weakAnimator = propertyAnimator;
    [propertyAnimator addCompletion:^(UIViewAnimatingPosition finalPosition) {
        NSMutableDictionary *currentAnimators = KRNativePropertyAnimators(weakView);
        if (currentAnimators[propertyKey] == weakAnimator) {
            [currentAnimators removeObjectForKey:propertyKey];
        }
        if (completion) {
            completion(finalPosition == UIViewAnimatingPositionEnd);
        }
    }];
    if (delay > 0) {
        [propertyAnimator startAnimationAfterDelay:delay];
    } else {
        [propertyAnimator startAnimation];
    }
    return YES;
#endif
}
