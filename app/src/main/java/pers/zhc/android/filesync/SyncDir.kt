package pers.zhc.android.filesync

import java.io.File

data class SyncDir(
    var path: File,
    var enabled: Boolean = true,
)
