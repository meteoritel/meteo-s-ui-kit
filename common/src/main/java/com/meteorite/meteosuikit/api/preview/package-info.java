/**
 * 图标场景的主体快照、异步编译、实例、转台取景与资源生命周期；单向依赖基础能力。
 *
 * <p>本目录的 public 类型是受支持的公开入口；包内 helper 由 Java 可见性保护。
 * library 表示受支持的公开库契约，具体用途与所有权见对应类型 JavaDoc。
 * 公开返回链随用途说明，嵌套公开契约随顶层类型维护。</p>
 * <ul>
 * <li>{@code PreviewBudget}: library</li>
 *   <li>{@code PreviewMotion}: library</li>
 *   <li>{@code PreviewScene}: library；场景句柄由 request 申请或 find 只读查询，资源释放由创建它的 PreviewScenes 统一管理</li>
 *   <li>{@code PreviewScenes}: library</li>
 *   <li>{@code PreviewState}: library</li>
 *   <li>{@code PreviewStructure}: library</li>
 *   <li>{@code PreviewSubject}: library</li>
 *   <li>{@code UiSceneCamera}: library；用途：单次绘制的相机角度，单位为弧度；缩放由视图按整圈包围计算；公开库用途</li>
 *   <li>{@code UiSceneStyle}: library</li>
 *   <li>{@code UiSceneView}: library；用途：唯一三维取景入口：整圈适框、裁剪、光照与 GUI 状态成对恢复；公开库用途</li>
 * <li>{@code PreviewPlatforms}: library；用途：默认平台 Geometry 取用</li>
 * </ul>
 */
package com.meteorite.meteosuikit.api.preview;
