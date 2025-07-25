package com.simple.memo.ui.settings

import android.content.Context
import android.graphics.Rect
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.simple.memo.R
import com.simple.memo.util.CustomToastMessage
import kotlin.math.min

class ManageFolderAdapter(
    private var folders: MutableList<String>,
    private val onRename: (oldName: String, newName: String) -> Unit,
    private val onDelete: (folderName: String) -> Unit
) : RecyclerView.Adapter<ManageFolderAdapter.FolderViewHolder>() {

    inner class FolderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.tv_folder_name)
        val editBtn: TextView = view.findViewById(R.id.btn_edit)
        val deleteBtn: TextView = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manage_folder, parent, false)
        return FolderViewHolder(view)
    }

    fun updateData(folders: MutableList<String>) {
        this.folders = folders
        notifyDataSetChanged()
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
                .setCancelable(true)
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

            btnConfirm.setOnClickListener confirmClick@{
                val newName = folderEditText.text.toString().trim()
                val prefs = context.getSharedPreferences("folder_prefs", Context.MODE_PRIVATE)
                val folderSet = prefs.getStringSet("folder_list", emptySet()) ?: emptySet()

                // 이름만 추출해서 비교용 Set 생성
                val existingNames = folderSet.mapNotNull {
                    it.split("|||").getOrNull(0)
                }.toSet()

                val currentPosition = holder.bindingAdapterPosition
                if (currentPosition == RecyclerView.NO_POSITION || currentPosition >= folders.size) {
                    dialog.dismiss()
                    return@confirmClick
                }

                when {
                    newName.isEmpty() -> {
                        hideKeyboard(folderEditText)
                        CustomToastMessage.createToast(context,context.getString(R.string.input_folder_name)).show()
//                        folderEditText.error = context.getString(R.string.input_folder_name)
                    }

                    newName == folderName -> {
                        hideKeyboard(folderEditText)
                        dialog.dismiss()
                    }

                    existingNames.contains(newName) -> {
                        hideKeyboard(folderEditText)
                        CustomToastMessage.createToast(context,context.getString(R.string.error_folder_name_exists)).show()
//                        folderEditText.error = context.getString(R.string.error_folder_name_exists)
                    }

                    else -> {
                        onRename(folderName, newName)
                        folders[currentPosition] = newName
                        notifyItemChanged(currentPosition)
                        hideKeyboard(folderEditText)
                        dialog.dismiss()
                    }
                }
            }

            dialog.show()

            dialog.window?.let { window ->
                val params = window.attributes
                val screenWidth = holder.itemView.context.resources.displayMetrics.widthPixels
                val dialogWidth = (screenWidth * 1.0f).toInt() // 100퍼
                val maxDialogWidth =
                    holder.itemView.context.resources.getDimensionPixelSize(R.dimen.dialog_max_width)

                params.width = min(dialogWidth, maxDialogWidth)
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT

                window.attributes = params
            }

        }

        holder.deleteBtn.setOnClickListener {

            val context = holder.itemView.context
            val dialogView =
                LayoutInflater.from(context).inflate(R.layout.dialog_folder_delete, null)
            val btnCancel = dialogView.findViewById<TextView>(R.id.btn_cancel)
            val btnConfirm = dialogView.findViewById<TextView>(R.id.btn_confirm)

            val dialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
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
                    folders.removeAt(safePosition)
                    notifyItemRemoved(safePosition)
                    onDelete(deletedName)
                    dialog.dismiss()
                } else {
                    dialog.dismiss()
                }
            }

            dialog.show()
            dialog.window?.let { window ->
                val params = window.attributes
                val screenWidth = holder.itemView.context.resources.displayMetrics.widthPixels
                val dialogWidth = (screenWidth * 1.0f).toInt() // 100퍼
                val maxDialogWidth =
                    holder.itemView.context.resources.getDimensionPixelSize(R.dimen.dialog_max_width)

                params.width = min(dialogWidth, maxDialogWidth)
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT

                window.attributes = params
            }
        }
    }

    override fun getItemCount(): Int = folders.size

    private fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
