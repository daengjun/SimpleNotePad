package com.simple.memo.ui.home

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.simple.memo.R
import com.simple.memo.data.model.MemoEntity
import com.simple.memo.databinding.FragmentHomeBinding
import com.simple.memo.ui.common.MemoBottomSheetDialogFragment
import com.simple.memo.ui.write.WriteMemoFragment
import com.simple.memo.util.CustomToastMessage
import com.simple.memo.viewModel.MemoViewModel
import kotlin.math.min


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var searchBarWatcher: TextWatcher? = null
    private lateinit var memoViewModel: MemoViewModel
    private lateinit var memoAdapter: MemoAdapter
    private var currentFolderName: String? = null
    private var isMultiSelectMode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        memoViewModel = ViewModelProvider(this)[MemoViewModel::class.java]

        // null = 전체
        currentFolderName = arguments?.getString("folderName")

        val searchBar = requireActivity().findViewById<EditText>(R.id.search_bar)

        searchBarWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val keyword = s.toString()
                if (keyword.isBlank()) {
                    observeMemoList()
                } else {

                    memoViewModel.searchMemosByFolder(currentFolderName, keyword)
                        .observe(viewLifecycleOwner) {
                            memoAdapter.submitList(it)
                        }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        searchBar.addTextChangedListener(searchBarWatcher)

        memoAdapter = MemoAdapter(
            onItemClick = { clickedMemo ->
                hideKeyboard()
                requireActivity().supportFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.fade_in,
                        R.anim.fade_out,
                        R.anim.fade_in,
                        R.anim.fade_out
                    )
                    .replace(R.id.nav_host_fragment, WriteMemoFragment.newInstance(clickedMemo))
                    .addToBackStack(null)
                    .commit()
            },
            onLongClickItemClick = { longClickedMemo ->
                val bottomSheet = MemoBottomSheetDialogFragment(
                    memo = longClickedMemo,
                    onDeleteClick = {
                        val updatedMemo = it.copy(isDeleted = true)
                        memoViewModel.updateMemo(updatedMemo)
                    },
                    onShare = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share))
                            putExtra(Intent.EXTRA_TEXT, it.content)
                        }
                        startActivity(
                            Intent.createChooser(
                                shareIntent,
                                getString(R.string.memo_share_app_select)
                            )
                        )
                    }
                )
                bottomSheet.show(parentFragmentManager, bottomSheet.tag)
            }
        )

        binding.recyclerMemo.adapter = memoAdapter
        binding.recyclerMemo.layoutManager = LinearLayoutManager(requireContext())

        observeMemoList()

        binding.fabAdd.setOnClickListener {

            if (isMultiSelectMode) {
                val selected = memoAdapter.getSelectedMemos()
                if (selected.isNotEmpty()) {
                    showDeleteConfirmDialog(selected)
                } else {
                    CustomToastMessage.createToast(
                        requireContext(),
                        getString(R.string.none_select_memo)
                    )
                        .show()
                }
            } else {
                hideKeyboard()
                val writeFragment = if (currentFolderName != null) {
                    WriteMemoFragment.newInstance(folderName = currentFolderName!!)
                } else {
                    WriteMemoFragment()
                }

                requireActivity().supportFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.fade_in,
                        R.anim.fade_out,
                        R.anim.fade_in,
                        R.anim.fade_out
                    )
                    .replace(R.id.nav_host_fragment, writeFragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
        return binding.root
    }

    private fun observeMemoList() {
        if (currentFolderName == null) {
            memoViewModel.allMemos.observe(viewLifecycleOwner) {
                memoAdapter.submitList(it)
            }
        } else {
            memoViewModel.getMemosByFolder(currentFolderName!!).observe(viewLifecycleOwner) {
                memoAdapter.submitList(it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val searchBar = requireActivity().findViewById<EditText>(R.id.search_bar)
        searchBarWatcher?.let { searchBar.removeTextChangedListener(it) }
        searchBarWatcher = null
        _binding = null
    }

    private fun hideKeyboard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireActivity().currentFocus?.windowToken, 0)
    }


    fun startMultiSelectMode() {
        if (isMultiSelectMode) return
        isMultiSelectMode = true
        memoAdapter.setMultiSelectMode(true)
        binding.fabAdd.setImageResource(R.drawable.ic_delete)

        (requireActivity() as AppCompatActivity).findViewById<TextView>(R.id.tv_toolbar_title)
            .animate()
            .alpha(1f)
            .setDuration(200)
            .start()
    }

    fun exitMultiSelectMode() {
        if (!isMultiSelectMode) return
        isMultiSelectMode = false
        memoAdapter.exitMultiSelectMode()
        binding.fabAdd.setImageResource(R.drawable.ic_add_memo)

        (requireActivity() as AppCompatActivity).findViewById<TextView>(R.id.tv_toolbar_title)
            .animate()
            .alpha(0f)
            .setDuration(200)
            .start()
    }

    private fun showDeleteConfirmDialog(selectedMemos: List<MemoEntity>) {

        val dialogView = layoutInflater.inflate(R.layout.dialog_multi_delete, null)
        val contentText = dialogView.findViewById<TextView>(R.id.content_text)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btn_cancel)
        val btnConfirm = dialogView.findViewById<TextView>(R.id.btn_confirm)
        contentText.text = getString(R.string.delete_memo_confirmation, selectedMemos.size)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
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
            moveToTrash(selectedMemos)
            dialog.dismiss()
        }

        dialog.show()

//        dialog.window?.setLayout(
//            ViewGroup.LayoutParams.MATCH_PARENT,
//            ViewGroup.LayoutParams.WRAP_CONTENT
//        )

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


    private fun moveToTrash(selectedMemos: List<MemoEntity>) {
        selectedMemos.forEach { memo ->
            val trashedMemo = memo.copy(isDeleted = true)
            memoViewModel.updateMemo(trashedMemo)
        }
        exitMultiSelectMode()
    }

    fun isMultiSelectMode(): Boolean = isMultiSelectMode

}


