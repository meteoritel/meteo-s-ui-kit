/**
 * 通用控件、滚动视口、灯箱与浮动面板；使用输入、几何与样式契约组合交互。
 * 滚动视口统一持有阻尼状态，成员帧推进后发布同源几何；单行输入保留原版编辑模型并以主题无阴影绘制。
 *
 * <p>本目录的 public 类型是受支持的公开入口；包内 helper 由 Java 可见性保护。
 * library 表示受支持的公开库契约，具体用途与所有权见对应类型 JavaDoc。
 * 公开返回链随用途说明，嵌套公开契约随顶层类型维护。</p>
 * <ul>
 *   <li>{@code LightboxImage}: library；用途：灯箱要展示的一张图：稳定 ID、贴图区域（原始宽高显式给出）与可本地化标题/描述；公开库用途</li>
 *   <li>{@code UiCarousel}: library</li>
 *   <li>{@code UiControl}: library</li>
 *   <li>{@code UiControlGroup}: library；用途：复用控件实例、按层序命中并收集焦点目标；公开库用途</li>
 *   <li>{@code UiFloatingPanel}: library；用途：通用浮动调板：标题栏可拖动、可折叠，支持贴边吸附与贴底抽屉，内容由宿主注入；公开库用途</li>
 *   <li>{@code UiIconStrip}: library</li>
 *   <li>{@code UiIconLightboxContent}: library；用途：任意 UiIcon 的灯箱适配，按目标尺寸重绘并共享缩放与平移；公开库用途</li>
 *   <li>{@code UiImageView}: library；用途：灯箱的图片内容适配，负责适框、缩放与平移；公开库用途</li>
 *   <li>{@code UiLightbox}: library；用途：灯箱外壳：统一覆盖作用域、prepare/layer、内容视口、控制栏与真实布局快照；公开库用途</li>
 *   <li>{@code UiProgressBar}: library</li>
 *   <li>{@code UiScrollView}: library</li>
 *   <li>{@code UiWidget}: library</li>
 *   <li>{@code UiWidgetShell}: library</li>
 *   <li>{@code UiStatefulWidget}: library</li>
 *   <li>{@code UiDocumentView}: library</li>
 *   <li>{@code UiDocumentLightboxContent}: library</li>
 *   <li>{@code UiTextInput}: library</li>
 *   <li>{@code UiTextArea}: library</li>
 *   <li>{@code UiListView}: library</li>
 *   <li>{@code UiOverlay}: library</li>
 *   <li>{@code UiModal}: library</li>
 *   <li>{@code UiModalStack}: library</li>
 *   <li>{@code UiFooterHost}: library</li>
 *   <li>{@code UiScreen}: library；用途：保留自绘屏幕的事件与生命周期桥；公开库用途</li>
 *   <li>{@code UiThemedScreen}: library</li>
 *   <li>{@code UiButtonWidget}: library</li>
 *   <li>{@code UiFocusFollow}: library</li>
 *   <li>{@code UiStatefulTarget}: library</li>
 *   <li>{@code UiPaginationState}: library</li>
 *   <li>{@code UiScrollContainer}: library</li>
 * <li>{@code UiFormBody}: library；组合真实字段的滚动正文与焦点可见</li>
 * </ul>
 */
package com.meteorite.meteosuikit.api.control;
