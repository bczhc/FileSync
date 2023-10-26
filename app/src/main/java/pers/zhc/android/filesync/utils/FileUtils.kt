package pers.zhc.android.filesync.utils

import java.io.File

fun File.assertCreateFile() {
    if (!this.exists()) {
        if (!this.createNewFile()) {
            throw RuntimeException("File creation failed")
        }
    }
}

fun File.assertCreateDir() {
    if (!this.exists()) {
        if (!this.mkdirs()) {
            throw RuntimeException("File creation failed")
        }
    }
}
