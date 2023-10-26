package pers.zhc.android.filesync

import android.content.Intent
import android.os.Bundle
import android.text.method.ReplacementTransformationMethod
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import pers.zhc.android.filesync.databinding.ActivityMainBinding

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

        requestPermissionLauncher.launch(android.Manifest.permission.INTERNET)
        requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        requestPermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        val logET = bindings.logEt
        val appendLog = { line: String ->
            logET.append(JNI.joinWordJoiner(line))
            logET.append("\n")
        }

        bindings.syncBtn.setOnClickListener {
            logET.text.clear()

            JNI.send(ConfigManager.savedNetworkDestination,
                ConfigManager.savedDirPaths.map { it.path.path }.toTypedArray(),
                object : JNI.Callback {
                    override fun call(path: String) {
                        appendLog(path)
                    }
                })
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
