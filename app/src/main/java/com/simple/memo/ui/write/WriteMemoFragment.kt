package com.simple.memo.ui.write

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.simple.memo.R
import com.simple.memo.data.model.MemoEntity
import com.simple.memo.databinding.FragmentWriteMemoBinding
import com.simple.memo.ui.main.MainActivity
import com.simple.memo.util.TextSizeUtils.getTextSizeValue
import com.simple.memo.viewModel.MemoViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class WriteMemoFragment : Fragment() {

    private var _binding: FragmentWriteMemoBinding? = null
    private val binding get() = _binding!!

    private lateinit var memoViewModel: MemoViewModel

    private var originalMemo: MemoEntity? = null
    private var isDeletedFromButton = false
    private var currentFolderName: String = "기본"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWriteMemoBinding.inflate(inflater, container, false)
        memoViewModel = ViewModelProvider(this)[MemoViewModel::class.java]

        val prefs = requireContext().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        val selectedSize = prefs.getString("key_text_size", "medium") ?: "medium"
        val textSizeValue = getTextSizeValue(selectedSize)
        binding.editTextMemo.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeValue)

        originalMemo = arguments?.getSerializable(ARG_MEMO) as? MemoEntity

        currentFolderName = if (originalMemo == null) {
            arguments?.getString(ARG_FOLDER_NAME) ?: "기본"
        } else {
            originalMemo!!.folderName
        }
        originalMemo?.let {
            binding.editTextMemo.setText(it.content)
        }

        val mainActivity = (requireActivity()) as MainActivity
        mainActivity.setToolbarTitleVisible(false)

        return binding.root
    }

    override fun onPause() {
        super.onPause()
        val currentContent = binding.editTextMemo.text.toString().trim()
        val originalContent = originalMemo?.content?.trim()
        if (currentContent.isEmpty()) return

        if (isDeletedFromButton) {
            val updatedMemo = originalMemo!!.copy(
                content = currentContent,
                date = getCurrentFormattedDate(),
                isDeleted = true
            )
            memoViewModel.updateMemo(updatedMemo)
            return
        }

        if (originalMemo == null) {
            insertMemo(currentContent)
        } else if (currentContent != originalContent) {
            val updatedMemo = originalMemo!!.copy(
                content = currentContent,
                date = getCurrentFormattedDate()
            )
            memoViewModel.updateMemo(updatedMemo)
        }
    }

    private fun insertMemo(content: String) {
        val memo = MemoEntity(
            content = content,
            date = getCurrentFormattedDate(),
            isDeleted = false,
            folderName = currentFolderName
        )
        memoViewModel.insertMemo(memo)
    }

    private fun getCurrentFormattedDate(): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd h:mm:ss a", Locale.getDefault())
        return sdf.format(Date())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_MEMO = "arg_memo"
        private const val ARG_FOLDER_NAME = "arg_folder_name"

        fun newInstance(memo: MemoEntity): WriteMemoFragment {
            val fragment = WriteMemoFragment()
            val bundle = Bundle()
            bundle.putSerializable(ARG_MEMO, memo)
            fragment.arguments = bundle
            return fragment
        }

        fun newInstance(folderName: String): WriteMemoFragment {
            val fragment = WriteMemoFragment()
            val bundle = Bundle()
            bundle.putString(ARG_FOLDER_NAME, folderName)
            fragment.arguments = bundle
            return fragment
        }
    }

    fun getCurrentMemo(): MemoEntity? {
        return originalMemo
    }

    fun markAsDeleted() {
        isDeletedFromButton = true
    }
}