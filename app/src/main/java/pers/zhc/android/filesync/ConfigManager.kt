package pers.zhc.android.filesync

import pers.zhc.android.filesync.MyApplication.Companion.GSON
import java.io.File
import java.lang.RuntimeException

object ConfigManager {
    var savedDirPaths: List<SyncDir>
        get() {
            return runCatching {
                GSON.fromJson(configFile.readText(), Array<SyncDir>::class.java)!!.toList()
            }.getOrElse { emptyList() }
        }
        set(value) {
            configFile.writeText(GSON.toJson(value.toTypedArray()))
        }

    private val configFile by lazy {
        File(MyApplication.appContext.filesDir, "config.json").also {
            if (!it.exists()) {
                if (!it.createNewFile()) {
                    throw RuntimeException("File creation failed")
                }
            }
        }
    }
}
