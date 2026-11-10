package com.meteorite.meteosuikit.neoforge.client;

import com.meteorite.meteosuikit.api.preview.PreviewPlatforms;
import com.meteorite.meteosuikit.api.preview.PreviewScenes;
import com.meteorite.meteosuikit.samples.KitContainerExample;
import com.meteorite.meteosuikit.samples.KitShowcaseScreen;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 独立开发样例入口；该源集与演示资源均不进入发布产物。 */
@EventBusSubscriber(modid = "meteos_ui_kit", value = Dist.CLIENT)
public final class SampleKeys {
    private static final ExecutorService WORKERS = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "meteos-ui-kit-samples");
        thread.setDaemon(true);
        return thread;
    });

    private SampleKeys() { }

    /** F8 打开独立样例，Shift+F8 在玩家背包 Menu 内演示原版容器组合。 */
    @SubscribeEvent
    public static void key(InputEvent.Key event) {
        if (event.getKey() != GLFW.GLFW_KEY_F8 || event.getAction() != GLFW.GLFW_PRESS) return;
        Minecraft minecraft = Minecraft.getInstance();
        Screen previous = minecraft.screen;
        Runnable back = () -> minecraft.setScreen(previous);
        if (Screen.hasShiftDown()) {
            if (minecraft.player != null && minecraft.player.containerMenu == minecraft.player.inventoryMenu) {
                minecraft.setScreen(new KitContainerExample(minecraft.player.inventoryMenu,
                        minecraft.player.getInventory(), back));
            }
        } else {
            minecraft.setScreen(new KitShowcaseScreen(back, budget -> new PreviewScenes(WORKERS, Util::getMillis, PreviewPlatforms.geometry(),
                            (id, failure) -> failure.printStackTrace(System.err), true, budget)));
        }
    }

    /** 线程池独立于场景所有者，在开发客户端终止后结束。 */
    @SubscribeEvent
    public static void stopping(GameShuttingDownEvent event) {
        WORKERS.shutdownNow();
    }
}
