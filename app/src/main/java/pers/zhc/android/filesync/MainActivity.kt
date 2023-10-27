package pers.zhc.android.filesync

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pers.zhc.android.filesync.databinding.ActivityMainBinding
import pers.zhc.android.filesync.utils.SpinLatch
import pers.zhc.android.filesync.utils.checkedRunOnUiThread
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (!it) {
            ToastUtils.show(this, R.string.permission_denied_toast)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bindings = ActivityMainBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }
        val syncBtn = bindings.syncBtn
        val logET = bindings.logEt

        requestPermissionLauncher.launch(android.Manifest.permission.INTERNET)
        requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        requestPermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        val uiFinishedLatch = SpinLatch()
        val appendLog = { line: String ->
            uiFinishedLatch.prepare()
            checkedRunOnUiThread {
                logET.append(JNI.joinWordJoiner(line))
                logET.append("\n")
                bindings.scrollView.scrollTo(0, logET.bottom + 1)
                uiFinishedLatch.stop()
            }
            uiFinishedLatch.await()
        }

        syncBtn.setOnClickListener {
            syncBtn.isEnabled = false
            logET.text.clear()
            thread {
                val errorMsg = StringBuilder()
                for (dir in ConfigManager.savedDirPaths) {
                    appendLog("Syncing directory: ${dir.path.path}...")
                    runCatching {
                        JNI.send(ConfigManager.savedNetworkDestination,
                            dir.path.path,
                            object : JNI.Callback {
                                override fun message(message: String) {
                                    appendLog(message)
                                }

                                override fun progress(path: String, n: Int, total: Int) {
                                    if (n % (total / 100) == 0) {
                                        // slow log mode; divide the total outputs into just 100
                                        appendLog("Sending progress: [$n/$total], file: $path")
                                    }
                                }
                            })
                    }.onFailure {
                        appendLog("Error: $it")
                        errorMsg.appendLine(it)
                    }
                    appendLog("")
                }
                runOnUiThread {
                    syncBtn.isEnabled = true
                    if (errorMsg.isNotEmpty()) {
                        MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.error_occurred_dialog_title)
                            .setMessage(errorMsg.toString())
                            .setPositiveButton(R.string.button_confirm, null)
                            .create().apply {
                                setCanceledOnTouchOutside(false)
                                setCancelable(false)
                            }.show()
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.config -> {
                startActivity(Intent(this, ConfigActivity::class.java))
            }

            else -> {
                return false
            }
        }
        return true
    }
}
