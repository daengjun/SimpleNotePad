package com.simple.memo.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.simple.memo.R


data class FolderItem(val name: String)

class FolderAdapter(
    private val onClick: (FolderItem, View) -> Unit
) : ListAdapter<FolderItem, FolderAdapter.FolderViewHolder>(DiffCallback) {

    // FolderAdapter 안에 현재 선택된 name 저장
    private var _selectedFolderName: String? = null
    val selectedFolderName: String?
        get() = _selectedFolderName

    private var selectedPosition: Int = RecyclerView.NO_POSITION

    inner class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.img_icon)
        private val nameText: TextView = itemView.findViewById(R.id.tv_folder_name)

        fun bind(item: FolderItem, isSelected: Boolean) {
            icon.setImageResource(R.drawable.ic_folder)
            nameText.text = item.name
            itemView.isSelected = isSelected
            itemView.tag = item // 외부 구분용 tag 설정

            itemView.setOnClickListener {
                val prev = selectedPosition
                val current = bindingAdapterPosition

                if (prev != RecyclerView.NO_POSITION) notifyItemChanged(prev)
                selectedPosition = current
                notifyItemChanged(current)

                onClick(item, itemView)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_drawer_folder, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val isSelected = position == selectedPosition
        holder.bind(getItem(position), isSelected)
    }

    fun selectFolderByName(name: String) {
        _selectedFolderName = name
        val index = currentList.indexOfFirst { it.name == name }
        if (index != -1 && index != selectedPosition) {
            val prev = selectedPosition
            selectedPosition = index
            if (prev != RecyclerView.NO_POSITION) notifyItemChanged(prev)
            notifyItemChanged(index)
        }
    }

    fun clearSelection() {
        val prev = selectedPosition
        selectedPosition = RecyclerView.NO_POSITION
        _selectedFolderName = null
        if (prev != RecyclerView.NO_POSITION) notifyItemChanged(prev)
    }

    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<FolderItem>() {
            override fun areItemsTheSame(oldItem: FolderItem, newItem: FolderItem): Boolean {
                return oldItem.name == newItem.name
            }

            override fun areContentsTheSame(oldItem: FolderItem, newItem: FolderItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}