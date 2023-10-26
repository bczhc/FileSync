package pers.zhc.android.filesync

object JNI {
    init {
        System.loadLibrary("jni_lib")
    }

    @JvmStatic
    external fun test(): Int
}
