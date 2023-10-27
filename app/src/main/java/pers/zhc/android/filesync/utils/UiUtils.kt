package pers.zhc.android.filesync.utils

import android.os.Handler
import android.os.Looper

fun checkedRunOnUiThread(block: () -> Unit) {
    if (Looper.myLooper() != Looper.getMainLooper()) {
        Handler(Looper.getMainLooper()).post {
            block()
        }
    } else block()
}
