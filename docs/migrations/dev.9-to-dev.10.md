# dev.9 → dev.10

本版本统一控件状态入口、主题覆盖政策和文档阅读能力。dev 坐标固定消费，升级时同步编译和内嵌版本。

## 控件状态与组合扩展

`UiShellTarget` / `UiShellWidget` 替换为 `UiStatefulTarget` / `UiStatefulWidget`，内置控件收回 `shell()`。普通消费直接调用 setter：

```java
editor.setEnabled(editable);
editor.setVisible(visible);
editor.setBounds(x, y, width, height);
```

这些入口负责结清失效交互并同步原版编辑模型；禁用与只读政策独立。自定义控件私有组合 `UiWidgetShell`，显式实现语义操作，文本 Model 与选择会话的装配参考 [KitTextModelExample](../../common/src/dev/java/com/meteorite/meteosuikit/samples/KitTextModelExample.java)。

包装已有控件时直接共享核心状态：

```java
@Override public UiFocusState focusState() { return control.focusState(); }
@Override public boolean isEnabled() { return control.isEnabled(); }
@Override public KitControlExample setEnabled(boolean enabled) {
    control.setEnabled(enabled);
    return this;
}
```

几何、布局身份及其余资格也转发到核心，命中保留核心 owner 链。完整消费者见 [KitControlExample](../../common/src/dev/java/com/meteorite/meteosuikit/samples/KitControlExample.java)，在 Models 样例工具栏使用。

## 主题覆盖

组换主题保留显式结构覆盖，旧成员、复用成员与新成员使用同一默认政策。`setStyle` 与 `clearStyleOverride` 显式广播当前成员，并设置后续默认值。成员自己的 skin 和文字覆盖仍归成员所有；组的 `style()` 查询后续默认值。

单控件 `clearStyleOverride()` 只清结构覆盖；`clearSkinOverride()` 同时清结构与完整 skin，独立文字覆盖保持。动态 Supplier 的身份和 revision 沿现有主题缓存刷新。

## 文档阅读

自定义 `TextMeasurer` 实现补充 `wrap(Component, int)`，保留 Component 样式，极窄宽度仍向前分行。`UiDocument` 默认保留悬停滚动，折行使用 `TextOverflow.WRAP`。视觉续行保留同一逻辑 Row，图标只出现一次，实际命中使用片段几何。

`UiDocumentTarget` 增加祖先 Frame 的 `contentClip()`；手工构造快照的消费者补充该字段，根目标传 null。其余内容/屏幕几何和叶子文档归属沿既有契约。

页内组合 `UiDocumentView`，灯箱组合 `UiDocumentLightboxContent`。宿主提供逻辑节点、业务动作、名称和背景，删除自行维护的折行列表、动作索引及阅读手势。页内 bounds 只包含正文，边框由宿主绘制；持久 offset 归宿主，灯箱变换归自己的 Content。Enter/Space 和鼠标均在有效配对释放时执行一次动作，内容/排版失效、隐藏、禁用和切域取消旧按压。样例见 [灯箱装配](../../common/src/dev/java/com/meteorite/meteosuikit/samples/KitDocumentLightboxContent.java)。
