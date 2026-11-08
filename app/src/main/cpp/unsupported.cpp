#include <jni.h>
extern "C" JNIEXPORT jboolean JNICALL Java_com_devicespooflab_hooks_utils_NativeIdentity_nativeInstall(JNIEnv *, jclass) { return JNI_FALSE; }
extern "C" JNIEXPORT void JNICALL Java_com_devicespooflab_hooks_utils_NativeIdentity_nativePublish(JNIEnv *, jclass, jobjectArray, jobjectArray, jobjectArray, jobjectArray) { }
extern "C" JNIEXPORT void JNICALL Java_com_devicespooflab_hooks_utils_NativeIdentity_nativeBypassRuntimeVersion(JNIEnv *, jclass, jboolean) { }
