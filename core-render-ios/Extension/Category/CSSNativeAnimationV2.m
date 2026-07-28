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
        NSLog(
            @"[NativeAnimation][iOS] parse kind=%@ values=%@",
            kind ? *kind : payload[1],
            values ? *values : @[]
        );
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

static void KRCancelNativePropertyAnimator(
    UIView *view,
    NSString *propertyKey,
    NSString *animationKey,
    BOOL keepCurrentValue
) {
    NSMutableDictionary *animators = KRNativePropertyAnimators(view);
    UIViewPropertyAnimator *previousAnimator = animators[propertyKey];
    NSLog(
        @"[NativeAnimation][iOS] cancel key=%@ view=%p property=%@ animator=%p state=%ld keepCurrent=%d",
        animationKey,
        view,
        propertyKey,
        previousAnimator,
        (long)previousAnimator.state,
        keepCurrentValue
    );
    if (previousAnimator && previousAnimator.state == UIViewAnimatingStateActive) {
        CALayer *presentationLayer = view.layer.presentationLayer;
        CATransform3D model = view.layer.transform;
        CATransform3D presentation =
            presentationLayer ? presentationLayer.transform : model;
        NSLog(
            @"[NativeAnimation][iOS] interruptState key=%@ property=%@ "
             "model=(sx=%.4f sy=%.4f tx=%.2f ty=%.2f alpha=%.4f) "
             "presentation=(sx=%.4f sy=%.4f tx=%.2f ty=%.2f alpha=%.4f)",
            animationKey,
            propertyKey,
            model.m11,
            model.m22,
            model.m41,
            model.m42,
            view.alpha,
            presentation.m11,
            presentation.m22,
            presentation.m41,
            presentation.m42,
            presentationLayer ? presentationLayer.opacity : view.alpha
        );
        [previousAnimator stopAnimation:!keepCurrentValue];
        if (keepCurrentValue) {
            [previousAnimator finishAnimationAtPosition:UIViewAnimatingPositionCurrent];
        }
    }
    [animators removeObjectForKey:propertyKey];
}

static void KRLogNativeAnimationState(
    UIView *view,
    NSString *propertyKey,
    NSString *animationKey,
    NSString *phase
) {
    if (![propertyKey isEqualToString:@"transform"]) {
        return;
    }
    CALayer *presentationLayer = view.layer.presentationLayer;
    CATransform3D model = view.layer.transform;
    CATransform3D presentation =
        presentationLayer ? presentationLayer.transform : model;
    NSLog(
        @"[NativeAnimation][iOS] %@ key=%@ property=%@ "
         "model=(m11=%.4f m12=%.4f m21=%.4f m22=%.4f tx=%.2f ty=%.2f) "
         "presentation=(m11=%.4f m12=%.4f m21=%.4f m22=%.4f tx=%.2f ty=%.2f) "
         "anchor=(%.3f,%.3f) position=(%.2f,%.2f)",
        phase,
        animationKey,
        propertyKey,
        model.m11,
        model.m12,
        model.m21,
        model.m22,
        model.m41,
        model.m42,
        presentation.m11,
        presentation.m12,
        presentation.m21,
        presentation.m22,
        presentation.m41,
        presentation.m42,
        view.layer.anchorPoint.x,
        view.layer.anchorPoint.y,
        view.layer.position.x,
        view.layer.position.y
    );
}

#endif

BOOL KRPerformNativeAnimationV2(
    UIView *view,
    NSString *propertyKey,
    NSString *animationKey,
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
    NSLog(
        @"[NativeAnimation][iOS] start key=%@ view=%p property=%@ kind=%@ duration=%.3f delay=%.3f",
        animationKey,
        view,
        propertyKey,
        kind,
        duration,
        delay
    );
    if ([kind isEqualToString:@"snap"]) {
        KRCancelNativePropertyAnimator(view, propertyKey, animationKey, NO);
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

    KRCancelNativePropertyAnimator(view, propertyKey, animationKey, YES);
    KRLogNativeAnimationState(
        view,
        propertyKey,
        animationKey,
        @"initialState"
    );
    NSMutableDictionary *animators = KRNativePropertyAnimators(view);
    UIViewPropertyAnimator *propertyAnimator = [[UIViewPropertyAnimator alloc]
        initWithDuration:duration timingParameters:timingParameters];
    animators[propertyKey] = propertyAnimator;
    [propertyAnimator addAnimations:^{
        animations();
        CATransform3D target = view.layer.transform;
        NSLog(
            @"[NativeAnimation][iOS] targetState key=%@ property=%@ "
             "sx=%.4f sy=%.4f tx=%.2f ty=%.2f alpha=%.4f",
            animationKey,
            propertyKey,
            target.m11,
            target.m22,
            target.m41,
            target.m42,
            view.alpha
        );
    }];
    __weak UIView *weakView = view;
    __weak UIViewPropertyAnimator *weakAnimator = propertyAnimator;
    [propertyAnimator addCompletion:^(UIViewAnimatingPosition finalPosition) {
        UIView *strongView = weakView;
        NSMutableDictionary *currentAnimators =
            strongView ? KRNativePropertyAnimators(strongView) : nil;
        BOOL ownsProperty = strongView && currentAnimators[propertyKey] == weakAnimator;
        NSLog(
            @"[NativeAnimation][iOS] completion key=%@ view=%p property=%@ animator=%p position=%ld ownsProperty=%d",
            animationKey,
            strongView,
            propertyKey,
            weakAnimator,
            (long)finalPosition,
            ownsProperty
        );
        if (ownsProperty) {
            [currentAnimators removeObjectForKey:propertyKey];
        }
        if (completion) {
            completion(strongView && finalPosition == UIViewAnimatingPositionEnd);
        }
    }];
    if (delay > 0) {
        [propertyAnimator startAnimationAfterDelay:delay];
    } else {
        [propertyAnimator startAnimation];
    }
    KRLogNativeAnimationState(
        view,
        propertyKey,
        animationKey,
        @"startedState"
    );
    return YES;
#endif
}
