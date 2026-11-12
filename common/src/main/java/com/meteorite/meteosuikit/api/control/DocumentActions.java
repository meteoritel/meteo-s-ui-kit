package com.meteorite.meteosuikit.api.control;

import com.meteorite.meteosuikit.api.layout.UiDocument;
import com.meteorite.meteosuikit.api.layout.UiDocumentHit;
import com.meteorite.meteosuikit.api.layout.UiDocumentTarget;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/** 阅读控件共享的出现目标和配对激活，代次变化使旧按压失效。 */
final class DocumentActions {
    private final UiDocument document;
    private long revision = -1;
    private List<UiDocumentTarget> targets = List.of();
    private int index;
    private @Nullable UiDocumentHit pressed;

    DocumentActions(UiDocument document) { this.document = document; }
    void refresh() {
        long next = document.layoutRevision();
        if (revision != next) {
            revision = next;
            cancel();
        }
        targets = document.targets().stream().filter(target -> target.hit().action() != null
                && (target.contentClip() == null || target.contentBounds().intersect(target.contentClip()).height() > 0
                && target.contentBounds().intersect(target.contentClip()).width() > 0)).toList();
        index = Math.clamp(index, 0, Math.max(0, targets.size() - 1));
    }
    @Nullable UiDocumentTarget selected() { refresh(); return targets.isEmpty() ? null : targets.get(index); }
    void press(@Nullable UiDocumentHit hit) {
        refresh();
        pressed = hit;
        for (int i = 0; i < targets.size(); i++) if (targets.get(i).hit() == hit) index = i;
    }
    boolean hasPress() { return pressed != null && pressed.action() != null; }
    boolean release(@Nullable UiDocumentHit hit) {
        refresh();
        UiDocumentHit action = pressed;
        cancel();
        if (action == null || action != hit || action.action() == null) return false;
        action.action().run();
        return true;
    }
    void cancel() { pressed = null; }
    boolean navigate(int key) {
        refresh();
        if (targets.isEmpty()) return false;
        if (key == GLFW.GLFW_KEY_UP) index--;
        else if (key == GLFW.GLFW_KEY_DOWN) index++;
        else if (key == GLFW.GLFW_KEY_HOME) index = 0;
        else if (key == GLFW.GLFW_KEY_END) index = targets.size() - 1;
        else return false;
        cancel();
        index = Math.clamp(index, 0, Math.max(0, targets.size() - 1));
        return true;
    }
    boolean activate() {
        UiDocumentTarget target = selected();
        if (target == null) return false;
        target.hit().action().run();
        return true;
    }
}
