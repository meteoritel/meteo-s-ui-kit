package com.meteorite.meteosuikit.api.layout;

import com.meteorite.meteosuikit.api.geometry.UiRect;
import net.minecraft.network.chat.Component;
import java.util.List;

/**
 * 文档排版中的行或图标快照；动作、payload 与点命中共享同一个 UiDocumentHit。
 *
 * @param document 实际所属叶子文档；Frame 内目标保留子文档归属
 * @param path 从查询文档到叶子的块序号及叶子槽位；行是 0，前置图标是 1，尾随图标从 2 开始
 * @param hit 叶子内容坐标中的真实命中数据；重复动作按每次出现分别保留
 * @param label 行文案或图标首条提示，供宿主命名键盘动作
 * @param contentBounds 查询文档的内容坐标矩形，包含嵌套 Frame 变换
 * @param screenBounds 调用方 GUI 坐标矩形，包含查询文档和 Frame 变换，外部 pose 由宿主转换
 */
public record UiDocumentTarget(UiDocument document, List<Integer> path, UiDocumentHit hit, Component label,
                               UiRect contentBounds, UiRect screenBounds) {
    /** 冻结出现路径；内容或排版失效后由调用方重新取得快照。 */
    public UiDocumentTarget { path = List.copyOf(path); }
}
