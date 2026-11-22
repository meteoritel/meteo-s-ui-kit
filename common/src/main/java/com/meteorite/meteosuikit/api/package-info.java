/**
 * UI kit 的受支持公开能力入口。
 *
 * <p>input 负责作用域、交互与焦点；geometry 负责坐标；render 负责通用绘制；control 负责控件组合；
 * style 负责主题注入；number、text、layout、tree 分别拥有对应算法与契约；preview 拥有场景及资源。</p>
 * <p>宿主按所需能力导入公开类型，用途在各能力目录登记；library 表示受支持的公开库契约，宿主可直接消费。</p>
 * <p>依赖方向：geometry 只持有几何，不引用 input、control、render 或 internal；input 不依赖
 * control、render 或 style；layout 不依赖 control；style 不依赖 number。基础能力不反向引用 preview。</p>
 * <p>text 的 UiSingleLineEditor 组合 control 的 UiWidget，数值字段通过该契约使用编辑器的绘制、
 * 几何和输入。input 与 layout 共享成员的输入、命中及布局快照；style 与 render 共享皮肤、
 * 图像及提示呈现类型；layout 与 render 共用文档排版及绘制契约。</p>
 * <p>主题发布、输入、资源准备和共享渲染状态在客户端线程使用，GPU 绘制与释放在渲染线程执行。</p>
 */
package com.meteorite.meteosuikit.api;
