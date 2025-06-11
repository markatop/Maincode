
package com.baidu.duer.dcs.devicemodule.voiceoutput;

import com.baidu.duer.dcs.devicemodule.system.HandleDirectiveException;
import com.baidu.duer.dcs.devicemodule.voiceoutput.message.SpeakPayload;
import com.baidu.duer.dcs.devicemodule.voiceoutput.message.SpeechLifecyclePayload;
import com.baidu.duer.dcs.devicemodule.voiceoutput.message.VoiceOutputStatePayload;
import com.baidu.duer.dcs.framework.BaseDeviceModule;
import com.baidu.duer.dcs.framework.IMessageSender;
import com.baidu.duer.dcs.framework.IResponseListener;
import com.baidu.duer.dcs.framework.message.ClientContext;
import com.baidu.duer.dcs.framework.message.Directive;
import com.baidu.duer.dcs.framework.message.Event;
import com.baidu.duer.dcs.framework.message.Header;
import com.baidu.duer.dcs.framework.message.MessageIdHeader;
import com.baidu.duer.dcs.systeminterface.IMediaPlayer;
import com.baidu.duer.dcs.util.LogUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class VoiceOutputDeviceModule extends BaseDeviceModule {
    private static final String TAG = VoiceOutputDeviceModule.class.getSimpleName();
    private final List<IVoiceOutputListener> voiceOutputListeners;
    private final LinkedList<SpeakPayload> speakQueue = new LinkedList<>();
    private final IMediaPlayer mediaPlayer;
    private SpeechState speechState = SpeechState.FINISHED;
    private String lastSpeakToken = "";

    // 当前播放状态
    private enum SpeechState {
        PLAYING,
        FINISHED
    }

    public VoiceOutputDeviceModule(IMediaPlayer mediaPlayer,
                                   IMessageSender messageSender) {
        super(ApiConstants.NAMESPACE, messageSender);
        this.mediaPlayer = mediaPlayer;
        this.mediaPlayer.addMediaPlayerListener(mediaPlayerListener);
        this.voiceOutputListeners = Collections.synchronizedList(new ArrayList<IVoiceOutputListener>());
    }

    @Override
    public ClientContext clientContext() {
        String namespace = ApiConstants.NAMESPACE;
        String name = ApiConstants.Events.SpeechState.NAME;
        Header header = new Header(namespace, name);
        VoiceOutputStatePayload payload = new VoiceOutputStatePayload(lastSpeakToken,
                mediaPlayer.getCurrentPosition(),
                speechState.name());
        return new ClientContext(header, payload);
    }

    @Override
    public void handleDirective(Directive directive) throws HandleDirectiveException {
        String directiveName = directive.getName();
        LogUtil.d(TAG, "rawMessage:" + directive.rawMessage);
        LogUtil.d(TAG, "directiveName:" + directiveName);
        if (directiveName.equals(ApiConstants.Directives.Speak.NAME)) {
            SpeakPayload speak = (SpeakPayload) directive.payload;
            handleSpeak(speak);
        } else {
            String message = "VoiceOutput cannot handle the directive";
            throw (new HandleDirectiveException(
                    HandleDirectiveException.ExceptionType.UNSUPPORTED_OPERATION, message));
        }
    }

    private void handleSpeak(SpeakPayload speak) {
        speakQueue.add(speak);
        if (speakQueue.size() == 1) {
            startSpeech();
        }
    }

    private void startSpeech() {
        final SpeakPayload speak = speakQueue.getFirst();
        if (null != speak) {
            lastSpeakToken = speak.token;
            mediaPlayer.play(new IMediaPlayer.MediaResource(speak.dcsStream));
        }
    }

    private IMediaPlayer.IMediaPlayerListener mediaPlayerListener = new IMediaPlayer.SimpleMediaPlayerListener() {
        @Override
        public void onPrepared() {
            super.onPrepared();
            speechState = SpeechState.PLAYING;
            sendStartedEvent(lastSpeakToken);
            fireOnVoiceOutputStarted();
        }

        @Override
        public void onStopped() {
            super.onStopped();
            speakQueue.clear();
        }

        @Override
        public void onError(String error, IMediaPlayer.ErrorType errorType) {
            super.onError(error, errorType);
            finishedSpeechItem();
        }

        @Override
        public void onCompletion() {
            LogUtil.d(TAG, " IMediaPlayer onCompletion");
            finishedSpeechItem();
        }
    };

    private void finishedSpeechItem() {
        speakQueue.poll();
        LogUtil.d(TAG, "finishedSpeechItem speakQueue size :" + speakQueue.size());
        if (speakQueue.isEmpty()) {
            speechState = SpeechState.FINISHED;
            sendFinishedEvent(lastSpeakToken);
            fireOnVoiceOutputFinished();
        } else {
            startSpeech();
        }
    }

    @Override
    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer.removeMediaPlayerListener(mediaPlayerListener);
        }
        if (isSpeaking()) {
            speechState = SpeechState.FINISHED;
        }
        speakQueue.clear();
        voiceOutputListeners.clear();
    }

    public boolean isSpeaking() {
        return speechState == SpeechState.PLAYING;
    }

    private void sendStartedEvent(String token) {
        String namespace = ApiConstants.NAMESPACE;
        String name = ApiConstants.Events.SpeechStarted.NAME;
        MessageIdHeader header = new MessageIdHeader(namespace, name);
        Event event = new Event(header, new SpeechLifecyclePayload(token));
        messageSender.sendEvent(event);
    }

    private void sendFinishedEvent(String token) {
        String namespace = ApiConstants.NAMESPACE;
        String name = ApiConstants.Events.SpeechFinished.NAME;
        MessageIdHeader header = new MessageIdHeader(namespace, name);
        Event event = new Event(header, new SpeechLifecyclePayload(token));
        messageSender.sendEvent(event, new IResponseListener() {
            @Override
            public void onSucceed(int statusCode) {
                // 没有新的语音speak-stream
                if (statusCode == 204) {
                    mediaPlayer.setActive(false);
                } else {
                    mediaPlayer.setActive(true);
                }
            }

            @Override
            public void onFailed(String errorMessage) {
                mediaPlayer.setActive(false);
            }
        });
    }

    private void fireOnVoiceOutputStarted() {
        for (IVoiceOutputListener listener : voiceOutputListeners) {
            listener.onVoiceOutputStarted();
        }
    }

    private void fireOnVoiceOutputFinished() {
        for (IVoiceOutputListener listener : voiceOutputListeners) {
            listener.onVoiceOutputFinished();
        }
    }


    public void addVoiceOutputListener(IVoiceOutputListener listener) {
        this.voiceOutputListeners.add(listener);
    }


    public interface IVoiceOutputListener {

        void onVoiceOutputStarted();

        void onVoiceOutputFinished();
    }
}