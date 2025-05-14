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
import kotlin.math.min

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

            val prefs = requireContext().getSharedPreferences("folder_prefs", Context.MODE_PRIVATE)
            val folderSet =
                prefs.getStringSet("folder_list", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

            if (folderSet.isEmpty()) {
                CustomToastMessage.createToast(
                    requireContext(),
                    getString(R.string.no_folders_to_manage)
                ).show()
                return@setOnClickListener
            }

            val manageFoldersFragment = ManageFoldersFragment()

            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.fade_in,
                    R.anim.fade_out,
                    R.anim.fade_in,
                    R.anim.fade_out
                )
                .replace(R.id.nav_host_fragment, manageFoldersFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun showResetDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete, null)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btn_cancel)
        val btnConfirm = dialogView.findViewById<TextView>(R.id.btn_confirm)

        val dialog = AlertDialog.Builder(requireContext())
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
            val viewModel = ViewModelProvider(this)[MemoViewModel::class.java]
            viewModel.deleteAllMemos()
            CustomToastMessage.createToast(
                requireContext(),
                getString(R.string.all_memos_deleted)
            )
                .show()
            dialog.dismiss()
        }

        dialog.show()

        /*
        * 태블릿 최대 크기 지정 600dp
        * */
        val dialogWidth = resources.displayMetrics.widthPixels
        val maxDialogWidth = resources.getDimensionPixelSize(R.dimen.dialog_max_width)

        dialog.window?.setLayout(
            min(dialogWidth, maxDialogWidth),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
