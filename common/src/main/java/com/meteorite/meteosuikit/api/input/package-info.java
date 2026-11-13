/**
 * 输入作用域、持续交互、焦点登记与导航历史；宿主选择活动域并注入内容、页面回退和退出动作。
 *
 * <p>本目录的 public 类型是受支持的公开入口；包内 helper 由 Java 可见性保护。
 * library 表示受支持的公开库契约，具体用途与所有权见对应类型 JavaDoc。
 * 公开返回链随用途说明，嵌套公开契约随顶层类型维护。</p>
 * <ul>
 *   <li>{@code UiPointer}: library</li>
 *   <li>{@code UiChildren}: library</li>
 *   <li>{@code UiInputContainer}: library</li>
 *   <li>{@code UiPointerHit}: library</li>
 *   <li>{@code UiFocusManager}: library</li>
 *   <li>{@code UiFocusState}: library</li>
 *   <li>{@code UiFocusTarget}: library</li>
 *   <li>{@code UiHistoryShortcut}: library</li>
 *   <li>{@code UiInputContext}: library</li>
 *   <li>{@code UiInputBridge}: library；用途：原版父类组合接入的回退裁决与根级按下配对，容器样例消费；公开库用途</li>
 *   <li>{@code UiInputRouter}: library；用途：选择稳定作用域并统一派发；宿主经 UiScreen.inputRouter 取得同一实例；公开库用途</li>
 *   <li>{@code UiInputScope}: library</li>
 *   <li>{@code UiInputTarget}: library</li>
 *   <li>{@code UiInteractionSession}: library</li>
 *   <li>{@code UiNavigationHistory}: library；用途：有界导航历史；宿主提供快照、有效性检查与恢复，输入和模态优先级由宿主决定；公开库用途</li>
 *   <li>{@code UiAction}: library</li>
 * </ul>
 */
package com.meteorite.meteosuikit.api.input;
