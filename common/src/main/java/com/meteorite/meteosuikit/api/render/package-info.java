/**
 * 通用图标、文本、绘制层、画布绘制原语与有界预览缓存；资源和文案由宿主注入。
 *
 * <p>本目录的 public 类型是受支持的公开入口；包内 helper 由 Java 可见性保护。
 * library 表示受支持的公开库契约，具体用途与所有权见对应类型 JavaDoc。
 * 公开返回链随用途说明，嵌套公开契约随顶层类型维护。</p>
 * <ul>
 *   <li>{@code UiTooltipPresenter}: library；用途：普通与富提示的 GUI 坐标换算和单次层级呈现</li>
 *   <li>{@code UiDotGrid}: library</li>
 *   <li>{@code UiFrameCapture}: library；用途：后续完成帧的原尺寸 PNG 与异步采样结果</li>
 *   <li>{@code UiFlatText}: library</li>
 *   <li>{@code UiIcon}: library</li>
 *   <li>{@code UiIconSheet}: library</li>
 *   <li>{@code UiLayer}: library</li>
 *   <li>{@code UiLayers}: library</li>
 *   <li>{@code UiModelBounds}: library；用途：以 renderer 实际提交的顶点测量模型，不使用实体碰撞框或方块形状推断可视范围；公开库用途</li>
 *   <li>{@code UiNineSlice}: library</li>
 *   <li>{@code UiOrthoLink}: library；用途：正交折线连接：父节点底部到子节点顶部，按「竖 → 横 → 竖」三段绘制，屏幕像素取整；公开库用途</li>
 *   <li>{@code UiPreviewCache}: library</li>
 *   <li>{@code UiRenderLayers}: library</li>
 *   <li>{@code UiRubberBand}: library</li>
 *   <li>{@code UiSpinner}: library</li>
 *   <li>{@code UiTooltipPlacement}: library；用途：为提示面板提供显式定位偏好与越界翻转，默认宿主通过 UiTooltipPanel 使用；公开库用途</li>
 *   <li>{@code UiTooltipPanel}: library</li>
 * </ul>
 */
package com.meteorite.meteosuikit.api.render;
