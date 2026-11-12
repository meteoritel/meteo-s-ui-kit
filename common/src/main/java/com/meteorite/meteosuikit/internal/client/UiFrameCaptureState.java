package com.meteorite.meteosuikit.internal.client;

import com.meteorite.meteosuikit.api.render.UiFrameCapture;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;

/** 平台帧末 Adapter 共用采样状态；未请求时仅推进帧号。 */
public final class UiFrameCaptureState {
    private static @Nullable Request pending;
    private static long frame;

    private UiFrameCaptureState() { }

    private record Request(Path output, Screen screen, ClientLevel level, int width, int height,
                           long requestedFrame, CompletableFuture<UiFrameCapture.Result> future) { }

    /** 采集请求归客户端主线程，同一时间只保留一个未结清的请求。 */
    public static CompletableFuture<UiFrameCapture.Result> capture(Path output) {
        Minecraft client = Minecraft.getInstance();
        if (!client.isSameThread()) throw new IllegalStateException("Capture requires client thread");
        if (pending != null && !pending.future().isDone()) throw new IllegalStateException("Capture already pending");
        Path path = output.toAbsolutePath().normalize();
        if (client.level == null || client.screen == null || client.getWindow().getWidth() <= 0
                || client.getWindow().getHeight() <= 0) throw new IllegalStateException("World, screen and window required");
        if (Files.exists(path)) throw new IllegalArgumentException("Capture output already exists: " + path);
        CompletableFuture<UiFrameCapture.Result> result = new CompletableFuture<>();
        pending = new Request(path, client.screen, client.level, client.getWindow().getWidth(),
                client.getWindow().getHeight(), frame, result);
        return result;
    }

    /** 只在完整 UI 绘制后调用；读取像素与 NativeImage 所有权一次性转交 IO 任务。 */
    public static void frameEnded() {
        frame++;
        Request request = pending;
        if (request == null) return;
        if (request.future().isDone()) { pending = null; return; }
        if (request.requestedFrame() < 0) return;
        if (frame <= request.requestedFrame()) return;
        RenderSystem.assertOnRenderThread();
        Minecraft client = Minecraft.getInstance();
        if (request.screen() != client.screen || request.level() != client.level
                || request.width() != client.getWindow().getWidth() || request.height() != client.getWindow().getHeight()) {
            request.future().completeExceptionally(new IllegalStateException("Capture context changed"));
            pending = null;
            return;
        }
        // requestedFrame 取负值标记已交 IO，保留 pending 至完成以拒绝并发采样。
        try {
            NativeImage pixels = Screenshot.takeScreenshot(client.getMainRenderTarget());
            long sampledFrame = frame;
            UiFrameCapture.Result result = new UiFrameCapture.Result(request.output(), pixels.getWidth(), pixels.getHeight(),
                    sampledFrame, request.screen().getClass().getName(), request.level().dimension().location().toString());
            pending = new Request(request.output(), request.screen(), request.level(), request.width(), request.height(), -1, request.future());
            try {
                Util.ioPool().execute(() -> writeImage(request, pixels, result));
            } catch (RuntimeException error) {
                pixels.close();
                throw error;
            }
        } catch (RuntimeException error) {
            request.future().completeExceptionally(error);
            pending = null;
        }
    }

    private static void writeImage(Request request, NativeImage pixels, UiFrameCapture.Result result) {
        Path temporary = null;
        try (pixels) {
            if (request.future().isDone()) return;
            Files.createDirectories(request.output().getParent());
            temporary = Files.createTempFile(request.output().getParent(), ".ui-frame-", ".png");
            pixels.writeToFile(temporary);
            if (!request.future().isDone()) {
                Files.move(temporary, request.output());
                request.future().complete(result);
            }
        } catch (IOException | RuntimeException error) {
            request.future().completeExceptionally(error);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); }
                catch (IOException error) { request.future().completeExceptionally(error); }
            }
        }
    }

    /** 停止客户端时结清请求，已交 IO 的图像仍由其任务关闭。 */
    public static void release() {
        if (pending != null) pending.future().completeExceptionally(new IllegalStateException("Client stopped"));
        pending = null;
    }
}
