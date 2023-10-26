package pers.zhc.android.filesync

import pers.zhc.android.filesync.MyApplication.Companion.GSON
import pers.zhc.android.filesync.utils.assertCreateDir
import pers.zhc.android.filesync.utils.assertCreateFile
import java.io.File

object ConfigManager {
    var savedDirPaths: List<SyncDir>
        get() {
            return runCatching {
                GSON.fromJson(configFiles.dirPaths.readText(), Array<SyncDir>::class.java)!!.toList()
            }.getOrElse { emptyList() }
        }
        set(value) {
            configFiles.dirPaths.writeText(GSON.toJson(value.toTypedArray()))
        }

    var savedNetworkDestination: String
        get() {
            return runCatching {
                GSON.fromJson(configFiles.networkDestination.readText(), String::class.java)!!
            }.getOrElse { "" }
        }
        set(value) {
            configFiles.networkDestination.writeText(GSON.toJson(value))
        }

    private val configFiles = object {
        private fun createAndGet(name: String): File {
            val configsDir = File(MyApplication.appContext.filesDir, "configs").also {
                it.assertCreateDir()
            }

            return File(configsDir, name).also { it.assertCreateFile() }
        }

        val networkDestination by lazy { createAndGet("network-destination") }
        val dirPaths by lazy { createAndGet("dir-paths") }
    }
}
