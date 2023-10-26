package pers.zhc.android.filesync

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import pers.zhc.android.filesync.databinding.ActivityConfigBinding

class ConfigActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = ActivityConfigBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }
    }
}
