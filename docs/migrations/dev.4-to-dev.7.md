# dev.4 → dev.7 迁移

本版本采用 [dev 兼容约定](../integration.md#固定版本消费)。宿主和库一起升级，编译、内嵌与运行元数据使用同一精确 dev 坐标。

## 包归属与状态壳

| dev.4 类型 | dev.7 入口 |
|---|---|
| `control.UiAction` | `input.UiAction` |
| `input.UiFocusFollow` | `control.UiFocusFollow` |
| `number.UiSliderRole`、`UiSliderSkin`、`UiSliderStyle`、`UiSliderPainter`、`PixelSliderPainter` | `style` 下同名类型 |
| `number.UiRangeControl` | 共同状态使用 `control.UiShellTarget`，端点行为使用具体区间类型 |
| `number.UiValueTextBinding` | 包内 Implementation；消费者使用 `UiNumberField` 与值交互入口 |

`UiShellWidget` 组合 `UiWidget` 和 `UiShellTarget`，区间核心与标量滑块复用后者。具体 setter 的返回形状和持续交互契约保持。旧包转发类型已退出。

自定义输入目标自己实现资格。实现 `UiWidget` 的可见目标通过默认方法贡献焦点；复合控件覆写 `addFocusTargets` 时应按自己的可见状态贡献实际子树。禁用目标可登记，由 `canFocus` 过滤。

## 主题读取

`UiThemeContext` 只持有当前主题和发布版本，退出 `UiStyleSource` 继承。主题化控件、图标与绘制入口接收 Supplier：

```java
UiThemeContext<UiStyleSource> context = UiThemeContext.minecraft();
Supplier<? extends UiStyleSource> theme = context::current;
UiTextInput editor = new UiTextInput(theme, font, hint);
UiControlGroup actions = new UiControlGroup();
actions.setTheme(theme);
```

固定主题使用 `() -> fixedTheme`；主动绘制先从 Supplier 取当前样式。根布局按 Context 的发布版本刷新，控件缓存按实际样式身份及其 revision 刷新。既有手势发布队列继续使用 `UiThemePublication`。

## 组合入口

- 共同文本色使用 `UiTextScheme`；`UiSurfaceStyle.withText(scheme)` 保留其他表面槽位。书页专属强调由宿主配方提供。
- 动态页数与合法页码使用 `UiPaginationState`。翻页后的选择、悬停和业务投影由调用方维护。
- 屏幕坐标滚动内容使用 `UiScrollContainer(scroll, content)`。内容仍按 scroll 偏移布局，绘制时裁剪到视口；持续拖动及配对释放由 scope 派发。
- 有限行格式化文本使用 `TextWrap.fit(font, text, width, maxLines)`；规则图集使用既有 `UiIconSheet`。

默认 preview Geometry 的获取与场景关闭方式保持。Display Mixin 使用 common 共享实现；共享绘制工作区的释放由库客户端生命周期接入。
