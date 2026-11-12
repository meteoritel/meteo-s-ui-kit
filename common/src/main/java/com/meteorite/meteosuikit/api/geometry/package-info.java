/**
 * GUI 矩形与内容坐标变换；几何换算与内容命中各有明确入口。
 *
 * <p>本目录的 public 类型是受支持的公开入口；包内 helper 由 Java 可见性保护。
 * library 表示受支持的公开库契约，具体用途与所有权见对应类型 JavaDoc。
 * 公开返回链随用途说明，嵌套公开契约随顶层类型维护。</p>
 * <ul>
 *   <li>{@code UiRect}: library</li>
 *   <li>{@code UiViewport}: library；用途：根 Screen 共用绘制、输入与快照缩放</li>
 *   <li>{@code UiTransform}: library</li>
 * </ul>
 */
package com.meteorite.meteosuikit.api.geometry;
