package com.simple.memo.ui.settings

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.simple.memo.R
import com.simple.memo.databinding.FragmentSettingsBinding
import androidx.core.content.edit
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simple.memo.ui.main.MainActivity
import com.simple.memo.util.CustomToastMessage
import com.simple.memo.viewModel.MemoViewModel

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val PREFS_NAME = "settings_prefs"
        private const val KEY_TEXT_SIZE = "key_text_size"
        private const val KEY_AUTO_DELETE = "key_auto_delete"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        when (prefs.getString(KEY_TEXT_SIZE, "medium")) {
            "small" -> binding.textSizeToggleGroup.check(R.id.btn_small)
            "medium" -> binding.textSizeToggleGroup.check(R.id.btn_medium)
            "large" -> binding.textSizeToggleGroup.check(R.id.btn_large)
        }

        when (prefs.getString(KEY_AUTO_DELETE, "never")) {
            "never" -> binding.deleteCycleToggleGroup.check(R.id.btn_never)
            "7days" -> binding.deleteCycleToggleGroup.check(R.id.btn_7days)
            "30days" -> binding.deleteCycleToggleGroup.check(R.id.btn_30days)
        }

        binding.textSizeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val value = when (checkedId) {
                    R.id.btn_small -> "small"
                    R.id.btn_medium -> "medium"
                    R.id.btn_large -> "large"
                    else -> "medium"
                }
                prefs.edit { putString(KEY_TEXT_SIZE, value) }
            }
        }

        binding.deleteCycleToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val value = when (checkedId) {
                    R.id.btn_never -> "never"
                    R.id.btn_7days -> "7days"
                    R.id.btn_30days -> "30days"
                    else -> "never"
                }
                prefs.edit { putString(KEY_AUTO_DELETE, value) }
            }
        }

        binding.memoAllDelete.setOnClickListener {
            showResetDialog()
        }

        binding.manageFolder.setOnClickListener {
            showManageFoldersDialog()
        }

    }


    private fun showResetDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete, null)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btn_cancel)
        val btnConfirm = dialogView.findViewById<TextView>(R.id.btn_confirm)

        val dialog = AlertDialog.Builder(requireContext())
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
            val viewModel = ViewModelProvider(this)[MemoViewModel::class.java]
            viewModel.deleteAllMemos()
            CustomToastMessage.createToast(
                requireContext(),
                "전체 메모 데이터 삭제 완료"
            )
                .show()
            dialog.dismiss()
        }

        dialog.show()

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun showManageFoldersDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_manage_folders, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recycler_folders)
        val prefs = requireContext().getSharedPreferences("folder_prefs", Context.MODE_PRIVATE)
        val folderSet =
            prefs.getStringSet("folder_list", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        if (folderSet.isEmpty()) {
            CustomToastMessage.createToast(requireContext(), "관리할 폴더가 없습니다.").show()
            return
        }

        val folderList = folderSet.toMutableList()
        val memoViewModel = ViewModelProvider(this)[MemoViewModel::class.java]

        Log.e("TAG", "showManageFoldersDialog: $folderList")

        val adapter = ManageFolderAdapter(
            folderList,
            onRename = { oldName, newName ->
                folderSet.remove(oldName)
                folderSet.add(newName)
                prefs.edit { putStringSet("folder_list", folderSet) }
                memoViewModel.renameFolder(oldName, newName)
                (activity as? MainActivity)?.refreshFolderMenus() // ← 여기에!
            },
            onDelete = { folderName ->
                folderSet.remove(folderName)
                prefs.edit { putStringSet("folder_list", folderSet) }
                memoViewModel.moveMemosToDefault(folderName)
                (activity as? MainActivity)?.refreshFolderMenus() // ← 여기에!
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        val dialog = AlertDialog.Builder(requireContext())
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

        dialog.show()

        /*
        * 가로 꽉차게 설정 (필수)
        * */
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}
