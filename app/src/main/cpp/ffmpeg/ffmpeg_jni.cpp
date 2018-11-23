//
// Created by 海建明 on 2018/11/22.
//

#include <jni.h>
#include "../utilbase/utilbase.h"

extern "C" {
#include "../ffmpeg/ffmpeg.h"
}

__attribute__((section (".mytext"))) static jstring
ffmpegCMD(JNIEnv *env, jclass obj, jstring commands) {
    LOGI("start ffmpeg");
    char info[40000] = {0};

    av_register_all();
    printf("start ffmpeg");
//
    AVCodec *c_temp = av_codec_next(NULL);
    while (c_temp != NULL) {
        if (c_temp->decode != NULL) {
            LOGI("%s[Dec]", info);
        } else {
            LOGI("%s[Enc]", info);
        }
        switch (c_temp->type) {
            case AVMEDIA_TYPE_VIDEO:
                LOGI("%s[Video]", info);
                break;
            case AVMEDIA_TYPE_AUDIO:
                LOGI("%s[Audio]", info);
                break;
            default:
                LOGI("%s[Other]", info);
                break;
        }
        LOGI("the infor:%s[%10s]", info, c_temp->name);
        c_temp = c_temp->next;
    }
    LOGE("The type:%s", info);
    return NULL;
}
__attribute__((section (".mytext"))) static jint
ffmpegInit(JNIEnv *env, jclass obj, jint code){
    LOGI("test log:%d",code);
    return code+4;
}
static JNINativeMethod methods[] = {
        {"ffmpegCMD", "(Ljava/lang/String;)Ljava/lang/String;", (void *) ffmpegCMD},
        {"ffmpegInit","(I)I",(void *)ffmpegInit}

};

jint registerNativeMethods(JNIEnv *env, const char *class_name, JNINativeMethod *methods,
                           int num_methods) {
    int result = 0;

    jclass clazz = env->FindClass(class_name);
    if (LIKELY(clazz)) {
        int result = env->RegisterNatives(clazz, methods, num_methods);
        if (UNLIKELY(result < 0)) {
            LOGE("registerNativeMethods failed(class=%s)", class_name);
        }
    } else {
        LOGE("registerNativeMethods: class'%s' not found", class_name);
    }
    return result;
}

int register_method(JNIEnv *env) {
    if (registerNativeMethods(env,
                              "com/llvision/streamdemo/ffmpeg/ffmpegNative",
                              methods, NUM_ARRAY_ELEMENTS(methods)) < 0) {
        return -1;
    }
    return 0;
}