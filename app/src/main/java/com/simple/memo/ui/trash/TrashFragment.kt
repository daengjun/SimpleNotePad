package com.simple.memo.ui.trash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.simple.memo.databinding.FragmentTrashBinding
import com.simple.memo.ui.common.TrashMemoBottomSheetDialogFragment
import com.simple.memo.viewModel.MemoViewModel


class TrashFragment : Fragment() {

    private var _binding: FragmentTrashBinding? = null
    private val binding get() = _binding!!
    private lateinit var memoViewModel: MemoViewModel
    private lateinit var trashMemoAdapter: TrashMemoAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrashBinding.inflate(inflater, container, false)
        memoViewModel = ViewModelProvider(this)[MemoViewModel::class.java]

        trashMemoAdapter = TrashMemoAdapter(
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

        binding.recyclerMemo.adapter = trashMemoAdapter
        binding.recyclerMemo.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMemo.adapter = trashMemoAdapter


        memoViewModel.deleteAllMemos.observe(viewLifecycleOwner) { memos ->
            trashMemoAdapter.submitList(memos)
        }

        return binding.root
    }
}
