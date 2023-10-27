package pers.zhc.android.filesync

object JNI {
    init {
        System.loadLibrary("jni_lib")
    }

    interface Callback {
        fun message(message: String)

        fun progress(path: String, n: Int, total: Int)
    }

    @JvmStatic
    external fun send(networkDest: String, dir: String, callback: Callback)

    @JvmStatic
    external fun joinWordJoiner(s: String): String
}
