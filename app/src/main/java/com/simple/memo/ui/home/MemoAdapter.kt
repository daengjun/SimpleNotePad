package com.simple.memo.ui.home

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.simple.memo.data.model.MemoEntity
import com.simple.memo.R
import androidx.core.graphics.toColorInt

class MemoAdapter(
    private val onItemClick: (MemoEntity) -> Unit,
    private val onLongClickItemClick: (MemoEntity) -> Unit
) : ListAdapter<MemoEntity, MemoAdapter.MemoViewHolder>(MemoDiffCallback()) {

    private var isMultiSelectMode = false
    private val selectedMemos = mutableSetOf<MemoEntity>()

    fun setMultiSelectMode(enabled: Boolean) {
        isMultiSelectMode = enabled
        selectedMemos.clear()
        notifyDataSetChanged()
    }

    fun getSelectedMemos(): List<MemoEntity> = selectedMemos.toList()

    fun exitMultiSelectMode() {
        isMultiSelectMode = false
        selectedMemos.clear()
        notifyDataSetChanged()
    }

    inner class MemoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val containerLayout: LinearLayout = itemView.findViewById(R.id.containerLayout)
        private val tvContent: TextView = itemView.findViewById(R.id.tv_content)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        private val cbSelect: CheckBox = itemView.findViewById(R.id.cb_select)

        fun bind(memo: MemoEntity) {
            tvContent.text = memo.content
            tvDate.text = memo.date

            // 글자 크기 세팅
            val prefs =
                itemView.context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
            val sizePref = prefs.getString("key_text_size", "medium") ?: "medium"
            val textSize = when (sizePref) {
                "small" -> 14f
                "medium" -> 16f
                "large" -> 18f
                else -> 16f
            }

            tvContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
            tvDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)

            val isSelected = selectedMemos.any { it.id == memo.id }

            // 멀티 선택 모드일 때만 체크박스 보이게
            cbSelect.visibility = if (isMultiSelectMode) View.VISIBLE else View.GONE

            cbSelect.setOnCheckedChangeListener(null)
            cbSelect.isChecked = isSelected

            containerLayout.setBackgroundColor(
                if (isSelected) {
                    "#51BAB5B5".toColorInt()
                } else {
                    Color.WHITE
                }
            )

            // 체크박스 클릭 시 선택 토글
            cbSelect.setOnCheckedChangeListener { _, checked ->
                if (!isMultiSelectMode) return@setOnCheckedChangeListener

                if (checked) {
                    addSelection(memo)
                } else {
                    removeSelection(memo)
                }
                notifyItemChanged(bindingAdapterPosition)
            }

            // 아이템 클릭
            itemView.setOnClickListener {
                if (isMultiSelectMode) {
                    // 멀티 선택 모드에서는 클릭 = 체크박스 토글과 동일하게
                    if (selectedMemos.any { it.id == memo.id }) {
                        removeSelection(memo)
                    } else {
                        addSelection(memo)
                    }
                    notifyItemChanged(bindingAdapterPosition)
                } else {
                    onItemClick(memo)
                }
            }

            // 터치시 살짝 작아지는 애니메이션 (멀티 선택 모드 아닐 때만)
            itemView.setOnTouchListener { v, event ->
                if (!isMultiSelectMode) {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(100).start()
                        }

                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                        }
                    }
                }
                false
            }

            itemView.setOnLongClickListener {
                onLongClickItemClick(memo)
                true
            }
        }

        private fun addSelection(memo: MemoEntity) {
            if (selectedMemos.none { it.id == memo.id }) {
                selectedMemos.add(memo)
            }
        }

        private fun removeSelection(memo: MemoEntity) {
            selectedMemos.removeAll { it.id == memo.id }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_memo, parent, false)
        return MemoViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
class MemoDiffCallback : DiffUtil.ItemCallback<MemoEntity>() {
    override fun areItemsTheSame(oldItem: MemoEntity, newItem: MemoEntity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: MemoEntity, newItem: MemoEntity): Boolean {
        return oldItem == newItem
    }
}