
package com.baidu.duer.dcs.androidsystemimpl.player;

import org.json.JSONObject;

import java.io.InputStream;


public class AudioStreamStoreImpl implements IAudioStreamStore {
    private AudioStoreThread mAudioStoreThread;
    private OnStoreListener onStoreListener;

    @Override
    public void save(InputStream inputStream) {
        mAudioStoreThread = new AudioStoreThread(inputStream);
        mAudioStoreThread.setOnDownListener(simpleOnDownListener);
        mAudioStoreThread.start();
    }

    @Override
    public void cancel() {
        if (mAudioStoreThread != null) {
            mAudioStoreThread.stopDown();
        }
    }

    @Override
    public void speakAfter() {
        if (mAudioStoreThread != null) {
            mAudioStoreThread.delDownFile();
        }
    }

    @Override
    public void setOnStoreListener(OnStoreListener listener) {
        onStoreListener = listener;
    }

    private AudioStoreThread.SimpleOnDownListener simpleOnDownListener = new AudioStoreThread.SimpleOnDownListener() {
        @Override
        public void onDownComplete(String path) {
            super.onDownComplete(path);
            if (onStoreListener != null) {
                onStoreListener.onComplete(path);
            }
        }

        @Override
        public void onDownError(JSONObject jsonObject) {
            super.onDownError(jsonObject);
            if (onStoreListener != null) {
                onStoreListener.onError(jsonObject.toString());
            }
        }
    };
}
