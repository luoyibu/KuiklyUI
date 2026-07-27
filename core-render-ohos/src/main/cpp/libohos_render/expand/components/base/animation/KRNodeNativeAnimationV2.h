/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI.
 */

#ifndef CORE_RENDER_OHOS_KRNODENATIVEANIMATIONV2_H
#define CORE_RENDER_OHOS_KRNODENATIVEANIMATIONV2_H

#include <arkui/native_animate.h>
#include <cmath>
#include <string>
#include <vector>
#include "libohos_render/utils/KRConvertUtil.h"

struct KRNodeNativeAnimationV2 {
    std::string kind;
    std::vector<float> values;

    static KRNodeNativeAnimationV2 Parse(const std::vector<std::string> &tokens) {
        KRNodeNativeAnimationV2 descriptor;
        for (const auto &token : tokens) {
            if (token.rfind("v2,", 0) != 0) {
                continue;
            }
            auto payload = kuikly::util::ConvertSplit(token, ",");
            if (payload.size() < 2) {
                continue;
            }
            descriptor.kind = payload[1];
            for (size_t index = 2; index < payload.size(); index++) {
                descriptor.values.push_back(std::stof(payload[index]));
            }
            break;
        }
        return descriptor;
    }
};

inline ArkUI_CurveHandle KRCreateNativeCubicBezierCurve(const std::vector<float> &values) {
    if (values.size() != 4) {
        return nullptr;
    }
    return OH_ArkUI_Curve_CreateCubicBezierCurve(values[0], values[1], values[2], values[3]);
}

inline ArkUI_CurveHandle KRCreateNativeInterpolatingSpringCurve(
    float velocity,
    float stiffness,
    float dampingRatio
) {
    return OH_ArkUI_Curve_CreateInterpolatingSpring(
        velocity,
        1.0,
        stiffness,
        2.0 * dampingRatio * std::sqrt(stiffness)
    );
}

#endif  // CORE_RENDER_OHOS_KRNODENATIVEANIMATIONV2_H
