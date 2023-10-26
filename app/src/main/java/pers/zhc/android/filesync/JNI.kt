package pers.zhc.android.filesync

object JNI {
    init {
        System.loadLibrary("jni_lib")
    }

    interface Callback {
        fun call(path: String)
    }

    @JvmStatic
    external fun send(networkDest: String, dirs: Array<String>, callback: Callback)
}
