//
// Created by 海建明 on 2018/11/22.
//
#include "jni.h"
#include "_onload.h"
#include "utilbase.h"

#define LOCAL_DEBUG 1

/**
 * Implement the register_method function in jni to register the native method in the VM.
 *
 * @param env Android jni
 * @return {0: register native success; -1: register native fail;}
 */
extern int register_method(JNIEnv *env);

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
#if LOCAL_DEBUG
    LOGI("JNI_OnLoad");
#endif

    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    // register native methods
    int result = register_method(env);
	setVM(vm);
#if LOCAL_DEBUG
    LOGI("JNI_OnLoad:finshed:result=%d", result);
#endif
    return JNI_VERSION_1_6;
}
