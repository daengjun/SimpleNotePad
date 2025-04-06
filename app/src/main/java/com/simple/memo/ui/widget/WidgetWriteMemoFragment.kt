package com.simple.memo.ui.widget

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.simple.memo.R
import com.simple.memo.data.model.MemoEntity
import com.simple.memo.databinding.FragmentWriteMemoBinding
import com.simple.memo.util.TextSizeUtils.getTextSizeValue
import com.simple.memo.viewModel.MemoViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WidgetWriteMemoFragment : Fragment() {

    private var _binding: FragmentWriteMemoBinding? = null
    private val binding get() = _binding!!

    private lateinit var memoViewModel: MemoViewModel

    private var originalMemo: MemoEntity? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWriteMemoBinding.inflate(inflater, container, false)
        memoViewModel = ViewModelProvider(this)[MemoViewModel::class.java]

        (requireActivity() as AppCompatActivity).findViewById<TextView>(R.id.tv_toolbar_title).visibility =
            View.INVISIBLE

        val prefs = requireContext().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        val selectedSize = prefs.getString("key_text_size", "medium") ?: "medium"
        val textSizeValue = getTextSizeValue(selectedSize)

        binding.editTextMemo.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeValue)
        originalMemo = arguments?.getSerializable(ARG_MEMO) as? MemoEntity

        originalMemo?.let {
            binding.editTextMemo.setText(it.content)
        }
        return binding.root
    }

    override fun onPause() {
        super.onPause()
        val currentContent = binding.editTextMemo.text.toString().trim()
        val originalContent = originalMemo?.content?.trim()
        if (currentContent.isEmpty()) return

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
            isDeleted = false
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
        fun newInstance(memo: MemoEntity): WidgetWriteMemoFragment {
            val fragment = WidgetWriteMemoFragment()
            val bundle = Bundle()
            bundle.putSerializable(ARG_MEMO, memo)
            fragment.arguments = bundle
            return fragment
        }
    }

    fun getCurrentMemo(): MemoEntity? {
        return originalMemo
    }
}