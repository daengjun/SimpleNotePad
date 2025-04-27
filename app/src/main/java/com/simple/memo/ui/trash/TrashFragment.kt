package com.simple.memo.ui.trash

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.simple.memo.R
import com.simple.memo.data.model.MemoEntity
import com.simple.memo.databinding.FragmentTrashBinding
import com.simple.memo.ui.common.TrashMemoBottomSheetDialogFragment
import com.simple.memo.util.CustomToastMessage
import com.simple.memo.viewModel.MemoViewModel
import kotlin.math.min

class TrashFragment : Fragment() {

    private var _binding: FragmentTrashBinding? = null
    private val binding get() = _binding!!
    private lateinit var memoViewModel: MemoViewModel
    private lateinit var trashMemoAdapter: TrashMemoAdapter

    private var isMultiSelectMode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrashBinding.inflate(inflater, container, false)
        memoViewModel = ViewModelProvider(this)[MemoViewModel::class.java]

        trashMemoAdapter = TrashMemoAdapter(
            onItemClick = { memo ->
                if (isMultiSelectMode) {
                    trashMemoAdapter.toggleMemoSelection(memo)
                } else {
                    // 단일 클릭 동작
                }
            },
            onLongClickItemClick = { longClickedMemo ->
                val bottomSheet = TrashMemoBottomSheetDialogFragment(
                    memo = longClickedMemo,
                    onDeleteClick = { memoToDelete ->
                        memoViewModel.deleteMemo(memoToDelete)
                    }, onRestoreClick = { memoToDelete ->
                        val updatedMemo = memoToDelete.copy(
                            isDeleted = false
                        )

                        /*
                        * isDelete = false
                        * */
                        memoViewModel.updateMemo(updatedMemo)
                    }
                )
                bottomSheet.show(parentFragmentManager, bottomSheet.tag)
            }
        )

        binding.recyclerMemo.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMemo.adapter = trashMemoAdapter

        memoViewModel.deleteAllMemos.observe(viewLifecycleOwner) { memos ->
            trashMemoAdapter.submitList(memos)
        }

        binding.fabAdd.setOnClickListener {
            if (isMultiSelectMode) {
                val selected = getSelectedTrashMemos()
                if (selected.isNotEmpty()) {
                    showDeleteConfirmDialog(selected)
                } else {
                    CustomToastMessage.createToast(
                        requireContext(),
                        getString(R.string.none_select_memo)
                    )
                        .show()
                }
            }
        }
        return binding.root
    }

    fun startMultiSelectMode() {
        isMultiSelectMode = true
        trashMemoAdapter.setMultiSelectMode(true)

        binding.fabAdd.apply {
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            visibility = View.VISIBLE

            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200)
                .start()
        }

        (requireActivity() as AppCompatActivity).findViewById<TextView>(R.id.tv_toolbar_title)
            .animate()
            .alpha(1f)
            .setDuration(200)
            .start()

    }

    fun exitMultiSelectMode() {
        isMultiSelectMode = false
        trashMemoAdapter.exitMultiSelectMode()
        binding.fabAdd.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(200)
            .withEndAction {
                binding.fabAdd.visibility = View.GONE
            }
            .start()

        (requireActivity() as AppCompatActivity).findViewById<TextView>(R.id.tv_toolbar_title)
            .animate()
            .alpha(0f)
            .setDuration(200)
            .start()

    }

    fun isMultiSelectMode(): Boolean = isMultiSelectMode

    private fun getSelectedTrashMemos(): List<MemoEntity> = trashMemoAdapter.getSelectedMemos()

    private fun showDeleteConfirmDialog(selectedMemos: List<MemoEntity>) {

        val dialogView = layoutInflater.inflate(R.layout.dialog_multi_delete, null)
        val contentText = dialogView.findViewById<TextView>(R.id.content_text)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btn_cancel)
        val btnConfirm = dialogView.findViewById<TextView>(R.id.btn_confirm)

        contentText.text =
            getString(R.string.delete_memo_permanently_confirmation, selectedMemos.size)

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
            selectedMemos.forEach { memoViewModel.deleteMemo(it) }
            exitMultiSelectMode()
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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}