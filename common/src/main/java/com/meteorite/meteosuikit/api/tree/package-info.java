/**
 * 与业务节点无关的树编辑、编解码注入、投影、视图状态、导航与移动交互。
 *
 * <p>本目录的 public 类型是受支持的公开入口；包内 helper 由 Java 可见性保护。
 * library 表示受支持的公开库契约，具体用途与所有权见对应类型 JavaDoc。
 * 公开返回链随用途说明，嵌套公开契约随顶层类型维护。</p>
 * <ul>
 *   <li>{@code UiTreeCodec}: library</li>
 *   <li>{@code UiTreeEditor}: library；视图读取、整体恢复和通知共用 UiTreeViewState，结构变化另发 Change</li>
 *   <li>{@code UiTreeMoveInteraction}: library</li>
 *   <li>{@code UiTreeNavigation}: library</li>
 *   <li>{@code UiTreePath}: library</li>
 *   <li>{@code UiTreeProjection}: library</li>
 *   <li>{@code UiTreeRowMarker}: library</li>
 *   <li>{@code UiTreeViewState}: library</li>
 * </ul>
 */
package com.meteorite.meteosuikit.api.tree;
