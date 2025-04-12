package com.simple.memo.ui.settings

import android.content.Context
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.simple.memo.R

class ManageFolderAdapter(
    private val folders: MutableList<String>,
    private val onRename: (oldName: String, newName: String) -> Unit,
    private val onDelete: (folderName: String) -> Unit
) : RecyclerView.Adapter<ManageFolderAdapter.FolderViewHolder>() {

    inner class FolderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.tv_folder_name)
        val editBtn: ImageView = view.findViewById(R.id.btn_edit)
        val deleteBtn: ImageView = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manage_folder, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folderName = folders[position]

        holder.nameText.text = folderName

        holder.editBtn.setOnClickListener {

            val context = holder.itemView.context
            val dialogView =
                LayoutInflater.from(context).inflate(R.layout.dialog_edit_folder_name, null)
            val folderEditText = dialogView.findViewById<EditText>(R.id.et_folder_name)
            val btnCancel = dialogView.findViewById<TextView>(R.id.btn_cancel)
            val btnConfirm = dialogView.findViewById<TextView>(R.id.btn_confirm)

            folderEditText.setText(folderName)

            val dialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            dialog.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    hideKeyboard(folderEditText)
                    dialog.dismiss()
                    true
                } else {
                    false
                }
            }

            btnCancel.setOnClickListener {
                hideKeyboard(folderEditText)
                dialog.dismiss()
            }


            btnConfirm.setOnClickListener {
                val newName = folderEditText.text.toString().trim()

                val prefs = context.getSharedPreferences("folder_prefs", Context.MODE_PRIVATE)
                val folderSet = prefs.getStringSet("folder_list", emptySet()) ?: emptySet()

                when {
                    newName.isEmpty() -> {
                        hideKeyboard(folderEditText)
                        folderEditText.error = context.getString(R.string.input_folder_name)
                    }

                    newName == folderName -> {
                        // 변경 없음
                        hideKeyboard(folderEditText)
                        dialog.dismiss()
                    }

                    folderSet.contains(newName) -> {
                        hideKeyboard(folderEditText)
                        folderEditText.error = "이미 존재하는 폴더 이름"
                    }

                    else -> {
                        onRename(folderName, newName)
                        folders[position] = newName
                        notifyItemChanged(position)
                        hideKeyboard(folderEditText)
                        dialog.dismiss()
                    }
                }
            }
            dialog.show()

            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        holder.deleteBtn.setOnClickListener {

            val context = holder.itemView.context
            val dialogView =
                LayoutInflater.from(context).inflate(R.layout.dialog_folder_delete, null)
            val btnCancel = dialogView.findViewById<TextView>(R.id.btn_cancel)
            val btnConfirm = dialogView.findViewById<TextView>(R.id.btn_confirm)

            val dialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            dialog.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    dialog.dismiss()
                    true
                } else {
                    false
                }
            }

            btnCancel.setOnClickListener {
                dialog.dismiss()
            }

            btnConfirm.setOnClickListener {
                val safePosition = holder.bindingAdapterPosition
                if (safePosition != RecyclerView.NO_POSITION && safePosition < folders.size) {
                    val deletedName = folders[safePosition]
                    onDelete(deletedName)
                    folders.removeAt(safePosition)
                    notifyItemRemoved(safePosition)
                    dialog.dismiss()
                } else {
                    dialog.dismiss()
                }
            }

            dialog.show()

            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

        }
    }

    override fun getItemCount(): Int = folders.size

    private fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
