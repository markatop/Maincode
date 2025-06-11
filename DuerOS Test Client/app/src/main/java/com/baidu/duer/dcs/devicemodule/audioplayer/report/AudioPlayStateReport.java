
package com.baidu.duer.dcs.devicemodule.audioplayer.report;

import com.baidu.duer.dcs.devicemodule.audioplayer.ApiConstants;
import com.baidu.duer.dcs.devicemodule.audioplayer.message.AudioPlayerPayload;
import com.baidu.duer.dcs.devicemodule.audioplayer.message.PlaybackFailedPayload;
import com.baidu.duer.dcs.devicemodule.audioplayer.message.PlaybackStatePayload;
import com.baidu.duer.dcs.devicemodule.audioplayer.message.PlaybackStutterFinishedPayload;
import com.baidu.duer.dcs.framework.IMessageSender;
import com.baidu.duer.dcs.framework.message.Event;
import com.baidu.duer.dcs.framework.message.Header;
import com.baidu.duer.dcs.framework.message.MessageIdHeader;
import com.baidu.duer.dcs.framework.message.Payload;
import com.baidu.duer.dcs.systeminterface.IMediaPlayer;

public class AudioPlayStateReport {
    public enum AudioPlayerState {
        IDLE,
        PLAYING,
        PAUSED,
        FINISHED,
        STOPPED,
        BUFFER_UNDERRUN
    }

    private AudioPlayerState currentState = AudioPlayerState.FINISHED;
    private IMessageSender messageSender;
    private String namespace;
    private AudioPlayStateReportListener audioPlayStateReportListener;

    public AudioPlayStateReport(String namespace, IMessageSender messageSender,
                                AudioPlayStateReportListener audioPlayStateReportListener) {
        this.namespace = namespace;
        this.messageSender = messageSender;
        this.audioPlayStateReportListener = audioPlayStateReportListener;

    }

    public AudioPlayerState getState() {
        return currentState;
    }

    public void playbackResumed() {
        currentState = AudioPlayerState.PLAYING;
        Event event = createAudioPlayerEvent(ApiConstants.Events.PlaybackResumed.NAME,
                audioPlayStateReportListener.getCurrentStreamToken(),
                audioPlayStateReportListener.getMediaPlayerCurrentOffsetInMilliseconds());
        messageSender.sendEvent(event);
    }

    public void playbackFinished() {
        currentState = AudioPlayerState.FINISHED;
        Event event = createAudioPlayerEvent(ApiConstants.Events.PlaybackFinished.NAME,
                audioPlayStateReportListener.getCurrentStreamToken(),
                audioPlayStateReportListener.getMediaPlayerCurrentOffsetInMilliseconds());
        messageSender.sendEvent(event);
    }

    public void playbackStarted() {
        currentState = AudioPlayerState.PLAYING;
        Event event = createAudioPlayerEvent(ApiConstants.Events.PlaybackStarted.NAME,
                audioPlayStateReportListener.getCurrentStreamToken(),
                audioPlayStateReportListener.getMediaPlayerCurrentOffsetInMilliseconds());
        messageSender.sendEvent(event);
    }

    public void playbackFailed(IMediaPlayer.ErrorType errorType) {
        currentState = AudioPlayerState.STOPPED;
        long offset = audioPlayStateReportListener.getMediaPlayerCurrentOffsetInMilliseconds();
        PlaybackStatePayload playbackStatePayload =
                new PlaybackStatePayload(audioPlayStateReportListener.getCurrentStreamToken(),
                        offset, currentState.toString());

        Header header = new MessageIdHeader(namespace,
                ApiConstants.Events.PlaybackFailed.NAME);
        Event event = new Event(header,
                new PlaybackFailedPayload(audioPlayStateReportListener.getCurrentStreamToken(),
                        playbackStatePayload, errorType));

        messageSender.sendEvent(event);
    }

    public void playbackPaused() {
        currentState = AudioPlayerState.PAUSED;
        Event event = createAudioPlayerEvent(ApiConstants.Events.PlaybackPaused.NAME,
                audioPlayStateReportListener.getCurrentStreamToken(),
                audioPlayStateReportListener.getMediaPlayerCurrentOffsetInMilliseconds());
        messageSender.sendEvent(event);
    }

    public void playbackNearlyFinished() {
        currentState = AudioPlayerState.FINISHED;
        Event event = createAudioPlayerEvent(ApiConstants.Events.PlaybackNearlyFinished.NAME,
                audioPlayStateReportListener.getCurrentStreamToken(),
                audioPlayStateReportListener.getMediaPlayerCurrentOffsetInMilliseconds());
        messageSender.sendEvent(event);
    }

    public void playbackStutterStarted() {
        currentState = AudioPlayerState.BUFFER_UNDERRUN;
        Event event = createAudioPlayerEvent(ApiConstants.Events.PlaybackStutterStarted.NAME,
                audioPlayStateReportListener.getCurrentStreamToken(),
                audioPlayStateReportListener.getCurrentOffsetInMilliseconds());
        messageSender.sendEvent(event);
    }

    public void playbackStutterFinished() {
        currentState = AudioPlayerState.PLAYING;
        Event event = createAudioPlayerPlaybackStutterFinishedEvent(
                audioPlayStateReportListener.getCurrentStreamToken(),
                audioPlayStateReportListener.getCurrentOffsetInMilliseconds(),
                audioPlayStateReportListener.getStutterDurationInMilliseconds()
        );
        messageSender.sendEvent(event);
    }

    public void playbackStopped() {
        currentState = AudioPlayerState.STOPPED;
        Event event = createAudioPlayerEvent(ApiConstants.Events.PlaybackStopped.NAME,
                audioPlayStateReportListener.getCurrentStreamToken(),
                audioPlayStateReportListener.getMediaPlayerCurrentOffsetInMilliseconds());
        messageSender.sendEvent(event);
    }

    public void clearQueueAll() {
        Event event = createAudioPlayerPlaybackQueueClearedEvent();
        messageSender.sendEvent(event);
        if (currentState == AudioPlayerState.PLAYING || currentState == AudioPlayerState.PAUSED
                || currentState == AudioPlayerState.BUFFER_UNDERRUN) {
            currentState = AudioPlayerState.STOPPED;
            Event eventStopped = createAudioPlayerEvent(ApiConstants.Events.PlaybackStopped.NAME,
                    audioPlayStateReportListener.getCurrentStreamToken(),
                    audioPlayStateReportListener.getMediaPlayerCurrentOffsetInMilliseconds());
            messageSender.sendEvent(eventStopped);
        }
    }

    public void clearQueueEnqueued() {
        Event event = createAudioPlayerPlaybackQueueClearedEvent();
        messageSender.sendEvent(event);
    }

    private Event createAudioPlayerEvent(String name, String streamToken,
                                         long offsetInMilliseconds) {
        Header header = new MessageIdHeader(namespace, name);
        Payload payload = new AudioPlayerPayload(streamToken, offsetInMilliseconds);
        return new Event(header, payload);
    }

    private Event createAudioPlayerPlaybackQueueClearedEvent() {
        Header header = new MessageIdHeader(ApiConstants.NAMESPACE,
                ApiConstants.Events.PlaybackQueueCleared.NAME);
        return new Event(header, new Payload());
    }

    private Event createAudioPlayerPlaybackStutterFinishedEvent(String streamToken,
                                                                long offsetInMilliseconds,
                                                                long stutterDurationInMilliseconds) {
        Header header = new MessageIdHeader(ApiConstants.NAMESPACE,
                ApiConstants.Events.PlaybackStutterFinished.NAME);
        return new Event(header, new PlaybackStutterFinishedPayload(streamToken,
                offsetInMilliseconds, stutterDurationInMilliseconds));
    }

    public void reportProgressDelay() {
        currentState = AudioPlayerState.PLAYING;
        Event event = createAudioPlayerEvent(ApiConstants.Events.ProgressReportDelayElapsed.NAME,
                audioPlayStateReportListener.getCurrentStreamToken(),
                audioPlayStateReportListener.getCurrentOffsetInMilliseconds());
        messageSender.sendEvent(event);
    }

    public void reportProgressInterval() {
        currentState = AudioPlayerState.PLAYING;
        Event event = createAudioPlayerEvent(ApiConstants.Events.ProgressReportIntervalElapsed.NAME,
                audioPlayStateReportListener.getCurrentStreamToken(),
                audioPlayStateReportListener.getCurrentOffsetInMilliseconds());
        messageSender.sendEvent(event);
    }

    public interface AudioPlayStateReportListener {
        String getCurrentStreamToken();

        long getCurrentOffsetInMilliseconds();

        long getMediaPlayerCurrentOffsetInMilliseconds();

        long getStutterDurationInMilliseconds();
    }
}
