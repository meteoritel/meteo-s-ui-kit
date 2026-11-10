/**
 * 只读文档渲染器、声明式节点与布局算法，以及供宿主读取的排版快照和度量。
 *
 * <p>本目录的 public 类型是受支持的公开入口；包内 helper 由 Java 可见性保护。
 * library 表示受支持的公开库契约，具体用途与所有权见对应类型 JavaDoc。
 * 公开返回链随用途说明，嵌套公开契约随顶层类型维护。</p>
 * <ul>
 *   <li>{@code UiDocumentTarget}: library；用途：真实排版节点、动作、出现路径和内容/屏幕几何快照</li>
 *   <li>{@code UiDocumentHit}: library；用途：文档行和图标的内容坐标命中、payload 与动作；公开库用途</li>
 *   <li>{@code UiDocument}: library；用途：只读文档排版、渲染与命中查询；内容版本和脏标记门控缓存，绘制与指针解析复用几何；公开库用途</li>
 *   <li>{@code UiFlowLayout}: library</li>
 *   <li>{@code UiFormLayout}: library</li>
 *   <li>{@code UiGridMetrics}: library</li>
 *   <li>{@code UiLayoutNode}: library</li>
 *   <li>{@code UiLayoutSource}: library</li>
 *   <li>{@code UiLinearLayout}: library；用途：有界横纵布局：把子项按显式尺寸规则排成一行或一列，产出逻辑矩形，供宿主摆放控件与原生输入框；公开库用途</li>
 *   <li>{@code UiMetrics}: library；用途：可选开发计时出口；生产关闭时不读取时钟，计数与最近耗时不创建逐帧快照；公开库用途</li>
 *   <li>{@code UiNode}: library；用途：声明式文档节点与单层 Frame 内容；公开库用途</li>
 *   <li>{@code UiRowMetrics}: library</li>
 *   <li>{@code UiTidyTreeLayout}: library</li>
 * </ul>
 */
package com.meteorite.meteosuikit.api.layout;
