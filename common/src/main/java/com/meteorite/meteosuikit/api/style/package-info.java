/**
 * 宿主样式注入契约、控件角色与不可变结构样式；滑块皮肤由 number 能力提供。
 *
 * <p>本目录的 public 类型是受支持的公开入口；包内 helper 由 Java 可见性保护。
 * library 表示受支持的公开库契约，具体用途与所有权见对应类型 JavaDoc。
 * 公开返回链随用途说明，嵌套公开契约随顶层类型维护。</p>
 * <ul>
 *   <li>{@code UiControlRole}: library</li>
 *   <li>{@code UiControlStyle}: library</li>
 *   <li>{@code UiStyleSource}: library</li>
 *   <li>{@code UiControlPainter}: library</li>
 *   <li>{@code UiControlSkin}: library</li>
 *   <li>{@code UiSurfaceStyle}: library</li>
 *   <li>{@code UiTooltipSkin}: library</li>
 *   <li>{@code UiThemeMetrics}: library</li>
 *   <li>{@code UiThemeContext}: library</li>
 *   <li>{@code UiMinecraftTheme}: library</li>
 *   <li>{@code UiSurfaceRole}: library</li>
 *   <li>{@code UiSurfaceSkin}: library</li>
 *   <li>{@code UiThemes}: library；用途：宿主组合完整主题；公开库用途</li>
 *   <li>{@code UiSkinImages}: library；用途：sprite/九宫格状态皮肤工厂；公开库用途</li>
 *   <li>{@code UiThemePublication}: library</li>
 * </ul>
 */
package com.meteorite.meteosuikit.api.style;
