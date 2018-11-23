package com.llvision.streamdemo.ffmpeg;

/**
 * @Project: StreamDemo
 * @Description:
 * @Author: haijianming
 * @Time: 2018/11/21 下午3:53
 */
public class ffmpegNative {
    static {
        System.loadLibrary("ffmpeg");
    }
    public static native String ffmpegCMD(String cmd);
    public static native int ffmpegInit(int code);
}
