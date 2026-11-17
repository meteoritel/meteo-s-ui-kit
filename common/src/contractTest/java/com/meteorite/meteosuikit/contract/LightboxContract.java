package com.meteorite.meteosuikit.contract;

import com.meteorite.meteosuikit.api.control.UiIconLightboxContent;
import com.meteorite.meteosuikit.api.control.UiLightbox;
import com.meteorite.meteosuikit.api.input.UiFocusTarget;
import com.meteorite.meteosuikit.api.layout.UiLayoutNode;
import com.meteorite.meteosuikit.api.render.UiIcon;
import com.meteorite.meteosuikit.api.render.UiNineSlice;
import com.meteorite.meteosuikit.api.style.UiMinecraftTheme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** 从真实布局与焦点出口验证灯箱，不依赖 GPU。 */
final class LightboxContract extends ContractSupport {
    static void run() {
        UiLightbox.Labels labels = new UiLightbox.Labels() {
            public Component close() { return Component.literal("close"); }
            public Component zoomIn() { return Component.literal("in"); }
            public Component zoomOut() { return Component.literal("out"); }
            public Component fit() { return Component.literal("fit"); }
            public Component previous() { return Component.literal("previous"); }
            public Component next() { return Component.literal("next"); }
            public Component position(int index, int total) { return Component.literal(index + "/" + total); }
            public Component panLock() { return Component.literal("lock"); }
            public Component panUnlock() { return Component.literal("unlock"); }
        };
        UiLightbox box = new UiLightbox(labels, new UiIconLightboxContent(
                new UiIcon.Rendered(64, 64, (graphics, rect) -> {}), () -> UiMinecraftTheme.INSTANCE), () -> {});
        MetricFont font = new MetricFont();
        for (boolean surface : List.of(false, true)) {
            box.setSurface(surface ? new UiNineSlice(ResourceLocation.withDefaultNamespace("test"), 16, 4) : null);
            for (boolean lock : List.of(false, true)) {
                box.setPanLockControlVisible(lock);
                for (boolean gallery : List.of(false, true)) {
                    box.setGallery(gallery ? new UiLightbox.Gallery() {
                        public int index() { return 0; }
                        public int total() { return 3; }
                        public void navigate(int delta) { }
                    } : null);
                    for (int width : List.of(320, 160, 100)) {
                        box.prepare(font, width, 180);
                        List<UiLayoutNode> controls = box.layoutNodes().getFirst().children().stream()
                                .filter(node -> node.kind() == UiLayoutNode.Kind.CONTROL && node.visible()).toList();
                        check(controls.stream().anyMatch(node -> node.text().getString().equals("close")), "close reachable");
                        check(controls.stream().anyMatch(node -> node.text().getString().equals("fit")), "fit reachable");
                        for (int i = 0; i < controls.size(); i++) {
                            UiLayoutNode node = controls.get(i);
                            check(node.rect().x() >= 0 && node.rect().right() <= width, "control within viewport");
                            check(box.hitTest(node.rect().x() + 1, node.rect().y() + 1).owns(node.target()), "layout and hit share target");
                            for (int j = i + 1; j < controls.size(); j++) {
                                var intersection = node.rect().intersect(controls.get(j).rect());
                                check(intersection.width() == 0 || intersection.height() == 0, "controls do not overlap");
                            }
                        }
                        List<UiFocusTarget> focus = new ArrayList<>();
                        box.addFocusTargets(focus::add);
                        List<String> names = focus.stream().map(target -> target.accessibleName().getString()).toList();
                        List<String> expected = new ArrayList<>(List.of("close"));
                        if (gallery) expected.addAll(List.of("previous", "next"));
                        expected.addAll(List.of("out", "in"));
                        if (lock) expected.add("lock");
                        expected.add("fit");
                        check(names.equals(expected), "semantic Tab order survives narrow layout");
                    }
                }
            }
        }
        box.setPanLocked(true);
        box.invalidateResources();
        box.prepare(font, 320, 180);
        check(box.layoutNodes().getFirst().children().stream().anyMatch(node -> node.text().getString().equals("unlock")),
                "lock label follows current state after invalidation");
        box.onClosed();
    }
}
