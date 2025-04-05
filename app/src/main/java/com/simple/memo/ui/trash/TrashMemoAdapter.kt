package com.simple.memo.ui.trash

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.simple.memo.data.model.MemoEntity
import com.simple.memo.R

class TrashMemoAdapter(
    private val onLongClickItemClick: (MemoEntity) -> Unit
) : ListAdapter<MemoEntity, TrashMemoAdapter.MemoViewHolder>(TrashMemoDiffCallback()) {

    inner class MemoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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

            itemView.setOnLongClickListener {
                onLongClickItemClick(memo)
                true
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

class TrashMemoDiffCallback : DiffUtil.ItemCallback<MemoEntity>() {
    override fun areItemsTheSame(oldItem: MemoEntity, newItem: MemoEntity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: MemoEntity, newItem: MemoEntity): Boolean {
        return oldItem == newItem
    }
}