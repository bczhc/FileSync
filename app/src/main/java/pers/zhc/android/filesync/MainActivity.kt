package pers.zhc.android.filesync

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import pers.zhc.android.filesync.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bindings = ActivityMainBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }

        bindings.syncBtn.setOnClickListener {

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
