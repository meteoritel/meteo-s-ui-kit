package com.meteorite.meteosuikit.api.preview;

/** 单次绘制的相机角度，单位为弧度；缩放由视图按整圈包围计算。 */
public record UiSceneCamera(float yaw, float pitch) { }
