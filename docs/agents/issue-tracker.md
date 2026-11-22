# 本地 Markdown issue 追踪器

每项功能使用 `.scratch/<feature-slug>/` 下的独立目录。发布时按需创建目录。Git 忽略 `.scratch/`。

- Spec 写入 `spec.md`，实现票据每张一份，写入 `issues/<NN>-<slug>.md`，从 `01` 开始按依赖顺序编号。
- 读取票据时使用文件路径。给定票号时，在对应的功能目录下查找。
- 在文档顶部附近用 `Status:` 记录分诊状态，取值见 [分诊标签](triage-labels.md)。分诊 issue 时用 `Category:` 记录 `bug` 或 `enhancement`。以 `wontfix` 关闭时保留文件。
- 用 `Blocked by:` 记录同一功能下的前置票据。评论按顺序追加到 `## Comments`。

## Wayfinding operations

- `/wayfinder` 将地图写入 `.scratch/<effort>/map.md`，决策票据写入同目录下的 `issues/<NN>-<slug>.md`，从 `01` 开始编号。
- 决策票据正文写明待解决的问题，`Type:` 取 `research`、`prototype`、`grilling` 或 `task`。这类票据使用 Wayfinder 状态。
- `Blocked by:` 列出前置票号。所有前置票据均为 `resolved` 后，按编号选取尚未认领的最早票据。
- 开始处理前保存 `Status: claimed`。解决后将答案追加到 `## Answer`，保存 `Status: resolved`，并在地图的 `## Decisions so far` 追加摘要与票据链接。
