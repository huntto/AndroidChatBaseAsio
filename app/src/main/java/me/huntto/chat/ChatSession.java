package me.huntto.chat;


import android.os.Handler;

import me.huntto.chat.utils.LogUtil;


public class ChatSession {
    private static final int MSG_CONNECTED = 0x01;
    private final static int MSG_CONNECT_FAILED = 0x02;

    private final static int MSG_ACCEPTED = 0x03;
    private final static int MSG_ACCEPT_FAILED = 0x04;

    private final static int MSG_SEND_MSG_SUCCESS = 0x05;
    private final static int MSG_SEND_MSG_FILED = 0x06;

    private final static int MSG_RECEIVE_MSG = 0x07;

    private final int mID;
    private final boolean mAsServer;
    private final String mIP;
    private final int mPort;
    private OnChatListener mListener;
    private Handler mHandler;

    public ChatSession(boolean asServer, String ip, int port) {
        mID = hashCode();
        mAsServer = asServer;
        mIP = ip;
        mPort = port;
        mHandler = new Handler();
    }

    public void init() {
        nativeNewInstance(this);
    }

    public void release() {
        nativeRelease(mID);
    }

    public int getPort() {
        return mPort;
    }

    public String getIP() {
        return mIP;
    }

    public void sendMessage(String msg) {
        nativeSendMessage(mID, msg);
    }

    public void setOnChatListener(OnChatListener listener) {
        mListener = listener;
    }

    public interface OnChatListener {
        void onConnected();

        void onConnectFailed();

        void onAccepted();

        void onAcceptFailed();

        void onSendMessageSuccess(String msg);

        void onSendMessageFailed(String msg);

        void onReceiveMessage(String msg);
    }

    private static native boolean nativeNewInstance(ChatSession session);

    private static native boolean nativeRelease(int id);

    private static native boolean nativeSendMessage(int id, String msg);


    private class HandleNativeCall implements Runnable {
        private int code;
        private String msg;

        HandleNativeCall(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        @Override
        public void run() {
            switch (code) {
                case MSG_CONNECTED:
                    if (mListener != null) {
                        mListener.onConnected();
                    }
                    break;
                case MSG_CONNECT_FAILED:
                    if (mListener != null) {
                        mListener.onConnectFailed();
                    }
                    break;
                case MSG_ACCEPTED:
                    if (mListener != null) {
                        mListener.onAccepted();
                    }
                    break;
                case MSG_ACCEPT_FAILED:
                    if (mListener != null) {
                        mListener.onAcceptFailed();
                    }
                    break;
                case MSG_SEND_MSG_SUCCESS:
                    if (mListener != null) {
                        mListener.onSendMessageSuccess(msg);
                    }
                    break;
                case MSG_SEND_MSG_FILED:
                    if (mListener != null) {
                        mListener.onSendMessageFailed(msg);
                    }
                    break;
                case MSG_RECEIVE_MSG:
                    if (mListener != null) {
                        mListener.onReceiveMessage(msg);
                    }
                    break;
                default:
                    LogUtil.e("not handle code:" + code + " msg:" + msg);
                    break;
            }
        }
    }

    /********* call from native *********/
    private void onSuccess(int code, String msg) {
        LogUtil.v("MSG:" + code + " msg - " + msg);
        mHandler.post(new HandleNativeCall(code, msg));
    }

    private void onError(int code, String msg) {
        LogUtil.v("MSG:" + code + " msg - " + msg);
        mHandler.post(new HandleNativeCall(code, msg));
    }

    private void onReceive(String msg) {
        LogUtil.v("msg - " + msg);
        mHandler.post(new HandleNativeCall(MSG_RECEIVE_MSG, msg));
    }

    static {
        System.loadLibrary("chat");
    }
}
