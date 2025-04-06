package com.simple.memo.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.simple.memo.R
import com.simple.memo.ui.home.MemoAdapter
import com.simple.memo.util.TextSizeUtils.getTextSizeValue
import com.simple.memo.viewModel.MemoViewModel

class MemoPickerFragment : Fragment() {

    private lateinit var viewModel: MemoViewModel
    private lateinit var adapter: MemoAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_memo_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_memos)
        val fab = view.findViewById<FloatingActionButton>(R.id.fab_go_write)

        viewModel = ViewModelProvider(requireActivity())[MemoViewModel::class.java]
        (requireActivity() as AppCompatActivity).findViewById<TextView>(R.id.tv_toolbar_title)
            .apply {
                text = context.getString(R.string.select_memo)
                visibility = View.VISIBLE
            }

        adapter = MemoAdapter(
            onItemClick = { memo ->
                val resultIntent = Intent(requireContext(), SimpleMemoWidget::class.java).apply {
                    action = "com.simple.memo.ACTION_MEMO_SELECTED"
                    putExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        requireActivity().intent.getIntExtra(
                            AppWidgetManager.EXTRA_APPWIDGET_ID,
                            AppWidgetManager.INVALID_APPWIDGET_ID
                        )
                    )
                    putExtra("selectedMemoId", memo.id)
                    putExtra("selectedMemoContent", memo.content)
                }
                requireContext().sendBroadcast(resultIntent)
                requireActivity().finish()
            },
            onLongClickItemClick = {}
        )

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        viewModel.allMemos.observe(viewLifecycleOwner) { memos ->
            adapter.submitList(memos)

            if (memos.isEmpty()) {
                fab.visibility = View.VISIBLE
                fab.setOnClickListener {
                    (requireActivity() as? MemoFlowActivity)?.openWriteMemoFragment()
                }
            } else {
                fab.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()

        /*
        * 텍스트 사이즈 변경시 즉시 반영
        * */
        val prefs = requireContext().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        val selectedSize = prefs.getString("key_text_size", "medium") ?: "medium"
        val textSizeValue = getTextSizeValue(selectedSize)
        (requireActivity() as AppCompatActivity).findViewById<TextView>(R.id.tv_toolbar_title)
            .setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeValue)
        adapter.notifyDataSetChanged()
    }
}
