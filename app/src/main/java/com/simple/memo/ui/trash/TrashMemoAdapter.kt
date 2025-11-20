package com.simple.memo.ui.trash

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
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.simple.memo.data.model.MemoEntity
import com.simple.memo.R

class TrashMemoAdapter(
    private val onItemClick: (MemoEntity) -> Unit,
    private val onLongClickItemClick: (MemoEntity) -> Unit
) : ListAdapter<MemoEntity, TrashMemoAdapter.MemoViewHolder>(TrashMemoDiffCallback()) {

    private var isMultiSelectMode = false
    private val selectedMemos = mutableSetOf<MemoEntity>()

    fun setMultiSelectMode(enabled: Boolean) {
        isMultiSelectMode = enabled
        selectedMemos.clear()
        notifyDataSetChanged()
    }

    fun exitMultiSelectMode() {
        isMultiSelectMode = false
        selectedMemos.clear()
        notifyDataSetChanged()
    }

    fun getSelectedMemos(): List<MemoEntity> = selectedMemos.toList()

    inner class MemoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val containerLayout: LinearLayout = itemView.findViewById(R.id.containerLayout)
        private val tvContent: TextView = itemView.findViewById(R.id.tv_content)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        private val cbSelect: CheckBox = itemView.findViewById(R.id.cb_select)

        fun bind(memo: MemoEntity) {
            tvContent.text = memo.content
            tvDate.text = memo.date

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

            // 멀티선택 모드일 때만 체크박스 보이기
            cbSelect.visibility = if (isMultiSelectMode) View.VISIBLE else View.GONE

            cbSelect.setOnCheckedChangeListener(null)
            cbSelect.isChecked = isSelected

            containerLayout.setBackgroundColor(
                if (isSelected) "#74B8B4B4".toColorInt()
                else Color.WHITE
            )

            cbSelect.setOnCheckedChangeListener { _, checked ->
                if (!isMultiSelectMode) return@setOnCheckedChangeListener

                if (checked) {
                    addSelection(memo)
                } else {
                    removeSelection(memo)
                }
                notifyItemChanged(bindingAdapterPosition)
            }

            itemView.setOnClickListener {
                if (isMultiSelectMode) {
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

            itemView.setOnLongClickListener {
                onLongClickItemClick(memo)
                true
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

    fun toggleMemoSelection(memo: MemoEntity) {
        if (selectedMemos.any { it.id == memo.id }) {
            selectedMemos.removeAll { it.id == memo.id }
        } else {
            selectedMemos.add(memo)
        }

        val index = currentList.indexOfFirst { it.id == memo.id }
        if (index != -1) {
            notifyItemChanged(index)
        }
    }
}

class TrashMemoDiffCallback : DiffUtil.ItemCallback<MemoEntity>() {
    override fun areItemsTheSame(oldItem: MemoEntity, newItem: MemoEntity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: MemoEntity, newItem: MemoEntity): Boolean {
        return oldItem == newItem
    }
}