package me.huntto.chat.utils;

import android.util.Log;

import me.huntto.chat.BuildConfig;

public class LogUtil {
    private static final boolean D = BuildConfig.DEBUG;
    private static final int STACK_INDEX = 5;

    public static void v(String msg) {
        if (D) {
            Log.v(getFinalTag(), getFinalMsg(msg));
        }
    }

    public static void d(String msg) {
        if (D) {
            Log.d(getFinalTag(), getFinalMsg(msg));
        }
    }

    public static void i(String msg) {
        Log.i(getFinalTag(), getFinalMsg(msg));
    }

    public static void w(String msg) {
        Log.w(getFinalTag(), getFinalMsg(msg));
    }

    public static void e(String msg) {
        Log.e(getFinalTag(), getFinalMsg(msg));
    }

    private static String getFinalTag() {
        return getClassName();
    }

    private static String getFinalMsg(String msg) {
        final StringBuilder sb = new StringBuilder();
        sb.append(getMethodName());
        sb.append(":");
        sb.append(getLineNumber());
        if (msg != null && !msg.isEmpty()) {
            sb.append(" >> ");
            sb.append(msg);
        }
        return sb.toString();
    }

    private static String getMethodName() {
        return Thread.currentThread().getStackTrace()[STACK_INDEX].getMethodName();
    }

    private static String getClassName() {
        return Thread.currentThread().getStackTrace()[STACK_INDEX].getClassName();
    }

    private static int getLineNumber() {
        return Thread.currentThread().getStackTrace()[STACK_INDEX].getLineNumber();
    }
}
