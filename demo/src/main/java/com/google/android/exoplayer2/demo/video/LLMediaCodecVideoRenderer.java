package com.google.android.exoplayer2.demo.video;

import android.content.Context;
import android.media.MediaCodec;
import android.os.Handler;
import android.os.SystemClock;

import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import java.nio.ByteBuffer;

/**
 * Created by A00480 on 2017/6/12.
 */

public class LLMediaCodecVideoRenderer extends MediaCodecVideoRenderer {
    /**
     * @param context            A context.
     * @param mediaCodecSelector A decoder selector.
     */
    public LLMediaCodecVideoRenderer(Context context, MediaCodecSelector mediaCodecSelector) {
        super(context, mediaCodecSelector);
    }

    /**
     * @param context              A context.
     * @param mediaCodecSelector   A decoder selector.
     * @param allowedJoiningTimeMs The maximum duration in milliseconds for which this video renderer
     */
    public LLMediaCodecVideoRenderer(Context context, MediaCodecSelector mediaCodecSelector, long allowedJoiningTimeMs) {
        super(context, mediaCodecSelector, allowedJoiningTimeMs);
    }

    /**
     * @param context                      A context.
     * @param mediaCodecSelector           A decoder selector.
     * @param allowedJoiningTimeMs         The maximum duration in milliseconds for which this video renderer
     *                                     can attempt to seamlessly join an ongoing playback.
     * @param eventHandler                 A handler to use when delivering events to {@code eventListener}. May be
     *                                     null if delivery of events is not required.
     * @param eventListener                A listener of events. May be null if delivery of events is not required.
     * @param maxDroppedFrameCountToNotify The maximum number of frames that can be dropped between
     *                                     invocations of {@link VideoRendererEventListener#onDroppedFrames(int, long)}.
     */
    public LLMediaCodecVideoRenderer(Context context, MediaCodecSelector mediaCodecSelector, long allowedJoiningTimeMs, Handler eventHandler, VideoRendererEventListener eventListener, int maxDroppedFrameCountToNotify) {
        super(context, mediaCodecSelector, allowedJoiningTimeMs, eventHandler, eventListener, maxDroppedFrameCountToNotify);
    }

    /**
     * @param context                     A context.
     * @param mediaCodecSelector          A decoder selector.
     * @param allowedJoiningTimeMs        The maximum duration in milliseconds for which this video renderer
     *                                    can attempt to seamlessly join an ongoing playback.
     * @param drmSessionManager           For use with encrypted content. May be null if support for encrypted
     *                                    content is not required.
     * @param playClearSamplesWithoutKeys Encrypted media may contain clear (un-encrypted) regions.
     *                                    For example a media file may start with a short clear region so as to allow playback to
     *                                    begin in parallel with key acquisition. This parameter specifies whether the renderer is
     *                                    permitted to play clear regions of encrypted media files before {@code drmSessionManager}
     *                                    has obtained the keys necessary to decrypt encrypted regions of the media.
     * @param eventHandler                A handler to use when delivering events to {@code eventListener}. May be
     *                                    null if delivery of events is not required.
     * @param eventListener               A listener of events. May be null if delivery of events is not required.
     * @param maxDroppedFramesToNotify    The maximum number of frames that can be dropped between
     *                                    invocations of {@link VideoRendererEventListener#onDroppedFrames(int, long)}.
     */
    public LLMediaCodecVideoRenderer(Context context, MediaCodecSelector mediaCodecSelector, long allowedJoiningTimeMs, DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, boolean playClearSamplesWithoutKeys, Handler eventHandler, VideoRendererEventListener eventListener, int maxDroppedFramesToNotify) {
        super(context, mediaCodecSelector, allowedJoiningTimeMs, drmSessionManager, playClearSamplesWithoutKeys, eventHandler, eventListener, maxDroppedFramesToNotify);
    }

    /**
     * This method is same with original processOutputBuffer() but disabled video renderer delay to minimum latency.
     *
     * @param positionUs
     * @param elapsedRealtimeUs
     * @param codec
     * @param buffer
     * @param bufferIndex
     * @param bufferFlags
     * @param bufferPresentationTimeUs
     * @param shouldSkip
     * @return
     */
    @Override
    protected boolean processOutputBuffer(long positionUs, long elapsedRealtimeUs, MediaCodec codec,
                                          ByteBuffer buffer, int bufferIndex, int bufferFlags, long bufferPresentationTimeUs,
                                          boolean shouldSkip) {
        if (shouldSkip) {
            skipOutputBuffer(codec, bufferIndex);
            return true;
        }

        if (!renderedFirstFrame) {
            if (Util.SDK_INT >= 21) {
                renderOutputBufferV21(codec, bufferIndex, System.nanoTime(), bufferPresentationTimeUs);
            } else {
                renderOutputBuffer(codec, bufferIndex);
            }
            return true;
        }

        if (getState() != STATE_STARTED) {
            return false;
        }

        // Compute how many microseconds it is until the buffer's presentation time.
        long elapsedSinceStartOfLoopUs = (SystemClock.elapsedRealtime() * 1000) - elapsedRealtimeUs;
        long earlyUs = bufferPresentationTimeUs - positionUs - elapsedSinceStartOfLoopUs;

        // Compute the buffer's desired release time in nanoseconds.
        long systemTimeNs = System.nanoTime();
        long unadjustedFrameReleaseTimeNs = systemTimeNs + (earlyUs * 1000);

        // Apply a timestamp adjustment, if there is one.
        long adjustedReleaseTimeNs = frameReleaseTimeHelper.adjustReleaseTime(
                bufferPresentationTimeUs, unadjustedFrameReleaseTimeNs);
        earlyUs = (adjustedReleaseTimeNs - systemTimeNs) / 1000;

        if (shouldDropOutputBuffer(earlyUs, elapsedRealtimeUs)) {
            // We're more than 30ms late rendering the frame.
            dropOutputBuffer(codec, bufferIndex);
            return true;
        }

        if (Util.SDK_INT >= 21) {
            // Let the underlying framework time the release.
            if (earlyUs < 50000) {
                renderOutputBufferV21(codec, bufferIndex, adjustedReleaseTimeNs, bufferPresentationTimeUs);
                return true;
            }
        } else {
            // We need to time the release ourselves.
            if (earlyUs < 30000) {
//                if (earlyUs > 11000) {
                    // We're a little too early to render the frame. Sleep until the frame can be rendered.
                    // Note: The 11ms threshold was chosen fairly arbitrarily.
//                    try {
//                        // Subtracting 10000 rather than 11000 ensures the sleep time will be at least 1ms.
//                        Thread.sleep((earlyUs - 10000) / 1000);
//                    } catch (InterruptedException e) {
//                        Thread.currentThread().interrupt();
//                    }
//                }
                renderOutputBuffer(codec, bufferIndex);
                return true;
            }
        }

        // We're either not playing, or it's not time to render the frame yet.
        return false;
    }
}
