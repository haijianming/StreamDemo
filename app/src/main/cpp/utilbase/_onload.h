//
// Created by 海建明 on 2018/11/22.
//
#ifndef ONLOAD_H_
#define ONLOAD_H_

#pragma interface

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

jint JNI_OnLoad(JavaVM *vm, void *reserved);

#ifdef __cplusplus
}
#endif

#endif /* ONLOAD_H_ */
