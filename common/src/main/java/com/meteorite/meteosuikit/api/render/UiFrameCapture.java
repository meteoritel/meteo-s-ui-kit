package com.meteorite.meteosuikit.api.render;

import com.meteorite.meteosuikit.internal.client.UiFrameCaptureState;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/** 按需采集下一完成帧；客户端主线程调用，调用方异步等待，不阻塞渲染。 */
public final class UiFrameCapture {
    private UiFrameCapture() { }

    /** 已写入的原尺寸 PNG 及采样身份；frame 为本客户端帧末序号。 */
    public record Result(Path path, int width, int height, long frame, String screen, String dimension) { }

    /**
     * 请求采集当前 Screen、世界与尺寸的后续完成帧，输出文件须尚不存在。
     * 同时只接受一个请求，身份变化、停止、IO 错误均结清 future；取消只由图像所有者释放资源。
     */
    public static CompletableFuture<Result> capture(Path output) { return UiFrameCaptureState.capture(output); }
}
