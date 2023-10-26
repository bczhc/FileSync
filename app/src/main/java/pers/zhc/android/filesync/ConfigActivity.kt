package pers.zhc.android.filesync

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pers.zhc.android.filesync.databinding.ActivityConfigBinding
import pers.zhc.android.filesync.databinding.DialogAddSyncDirBinding
import pers.zhc.android.filesync.databinding.ListItemDirectoryBinding
import java.io.File

class ConfigActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindings = ActivityConfigBinding.inflate(layoutInflater).also {
            setContentView(it.root)
        }
        val networkDestinationTV = bindings.networkDestinationTv

        val syncDirs = mutableListOf<SyncDir>()

        syncDirs.addAll(ConfigManager.savedDirPaths)
        networkDestinationTV.setText(ConfigManager.savedNetworkDestination)

        val listAdapter = ListAdapter(syncDirs, onLongClick = { self, view, position ->
            PopupMenu(this, view).apply {
                inflate(R.menu.popup_menu_delete)
                setOnMenuItemClickListener {
                    if (it.itemId != R.id.delete) return@setOnMenuItemClickListener false
                    self.notifyItemRemoved(position)
                    syncDirs.removeAt(position)
                    true
                }
            }.show()
        })
        bindings.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ConfigActivity)
            adapter = listAdapter
        }

        bindings.addBtn.setOnClickListener {
            val viewBindings = DialogAddSyncDirBinding.inflate(layoutInflater)

            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.title_dialog_add_path)
                .setView(viewBindings.root)
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.button_confirm) { _, _ ->
                    val path = viewBindings.editText.text.toString()
                    syncDirs += SyncDir(File(path))
                    listAdapter.notifyItemInserted(syncDirs.size)
                }
                .show()
        }

        onBackPressedDispatcher.addCallback {
            ConfigManager.savedDirPaths = syncDirs
            ConfigManager.savedNetworkDestination = networkDestinationTV.text.toString()
            finish()
        }
    }

    class ListAdapter(
        private val data: MutableList<SyncDir>,
        private val onLongClick: (self: ListAdapter, view: View, position: Int) -> Unit
    ) : RecyclerView.Adapter<ListAdapter.ViewHolder>() {
        class ViewHolder(bindings: ListItemDirectoryBinding) : RecyclerView.ViewHolder(bindings.root) {
            val textView = bindings.textView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val bindings = ListItemDirectoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(bindings)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textView.text = data[position].path.path
            holder.textView.setOnLongClickListener {
                onLongClick(this, holder.textView, holder.layoutPosition)
                true
            }
        }

        override fun getItemCount(): Int {
            return data.size
        }
    }
}
