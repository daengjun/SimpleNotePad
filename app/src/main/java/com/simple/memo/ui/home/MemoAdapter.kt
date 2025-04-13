package com.simple.memo.ui.home

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        private val LinearLayoutManager: LinearLayout = itemView.findViewById(R.id.containerLayout)
        private val tvContent: TextView = itemView.findViewById(R.id.tv_content)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)

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

            LinearLayoutManager.setBackgroundColor(
                if (isSelected) {
                    "#7EB1AFAF".toColorInt()
                } else {
                    Color.WHITE
                }
            )

            itemView.setOnClickListener {
                if (isMultiSelectMode) {
                    toggleSelection(memo)
                    notifyItemChanged(adapterPosition)
                } else {
                    onItemClick(memo)
                }
            }

            itemView.setOnLongClickListener {
                onLongClickItemClick(memo)
                true
            }
        }

        private fun toggleSelection(memo: MemoEntity) {
            if (selectedMemos.contains(memo)) {
                selectedMemos.remove(memo)
            } else {
                selectedMemos.add(memo)
            }
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