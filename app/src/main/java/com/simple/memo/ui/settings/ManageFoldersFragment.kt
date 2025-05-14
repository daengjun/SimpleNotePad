package com.simple.memo.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simple.memo.R
import com.simple.memo.ui.main.MainActivity
import com.simple.memo.viewModel.MemoViewModel

class ManageFoldersFragment : Fragment(R.layout.fragment_manage_folders) {
    private lateinit var prefs: SharedPreferences
    private lateinit var folderRawSet: Set<String>
    private lateinit var folderList: MutableList<String>
    private lateinit var adapter: ManageFolderAdapter
    private lateinit var memoViewModel: MemoViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_folders)

        prefs = requireContext().getSharedPreferences("folder_prefs", Context.MODE_PRIVATE)
        loadFolderData()

        memoViewModel = ViewModelProvider(this)[MemoViewModel::class.java]

        adapter = ManageFolderAdapter(
            folderList,
            onRename = { oldName, newName ->
                val updatedSet = folderRawSet.toMutableSet().apply {
                    removeIf { it.startsWith("$oldName|||") }
                    add("$newName|||${System.currentTimeMillis()}")
                }

                prefs.edit { putStringSet("folder_list", updatedSet) }

                memoViewModel.renameFolder(oldName, newName)
                (activity as? MainActivity)?.updateFolderList()
                loadFolderData()

            },
            onDelete = { folderName ->
                val updatedSet = folderRawSet.toMutableSet().apply {
                    removeIf { it.startsWith("$folderName|||") }
                }

                prefs.edit { putStringSet("folder_list", updatedSet) }

                memoViewModel.moveMemosToDefault(folderName)
                (activity as? MainActivity)?.updateFolderList()
                loadFolderData()
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun loadFolderData() {
        folderRawSet = prefs.getStringSet("folder_list", emptySet()) ?: emptySet()
        folderList = folderRawSet
            .mapNotNull {
                val parts = it.split("|||")
                if (parts.size == 2) parts[0] to parts[1].toLongOrNull() else null
            }
            .sortedByDescending { it.second }
            .map { it.first }
            .toMutableList()
    }

    fun refreshFolderList() {
        loadFolderData()
        adapter.updateData(folderList.toMutableList())
    }

}