package com.simple.memo.viewModel

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.simple.memo.R
import com.simple.memo.ui.widget.SimpleMemoWidget
import com.simple.memo.data.local.MemoDatabase
import com.simple.memo.data.model.MemoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.core.content.edit

class MemoViewModel(application: Application) : AndroidViewModel(application) {

    private val memoDao = MemoDatabase.getDatabase(application).memoDao()
    val allMemos: LiveData<List<MemoEntity>> = memoDao.observeAllActiveMemos()
    val deleteAllMemos: LiveData<List<MemoEntity>> = memoDao.observeAllActiveDeleteMemos()

    fun insertMemo(memo: MemoEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            memoDao.insertMemo(memo)
        }
    }

    fun getMemosByFolder(folderName: String): LiveData<List<MemoEntity>> {
        return memoDao.observeMemosByFolder(folderName)
    }

    fun updateMemo(memo: MemoEntity) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                memoDao.updateMemo(memo)
                handleWidgetUpdateOrClear(memo)
            } catch (e: Exception) {
                Log.e("TAG", "updateMemo exception: ${e.message}", e)
            }
        }
    }

    fun deleteMemo(memo: MemoEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            memoDao.deleteMemo(memo)
        }
    }

    fun searchMemosByFolder(folder: String?, keyword: String): LiveData<List<MemoEntity>> {
        return if (folder == null) {
            memoDao.searchMemos(keyword)
        } else {
            memoDao.searchMemosInFolder(folder, "%$keyword%")
        }
    }

    /*
    * 휴지통 설정한 날짜에 따라서 메모 자동 삭제
    * */
    fun autoDeleteOldTrash(cycle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (cycle == "never") return@launch

            val sdf = SimpleDateFormat("yyyy/MM/dd h:mm:ss a", Locale.getDefault())
            val calendar = Calendar.getInstance()

            when (cycle) {
                "7days" -> calendar.add(Calendar.DAY_OF_YEAR, -7)
                "30days" -> calendar.add(Calendar.DAY_OF_YEAR, -30)
            }

            val threshold = sdf.format(calendar.time)
            memoDao.deleteOldTrashMemos(threshold)
        }
    }


    private fun handleWidgetUpdateOrClear(memo: MemoEntity) {

        Log.e("TAG", "handleWidgetUpdateOrClear: 1")
        val context = getApplication<Application>().applicationContext
        val prefs = context.getSharedPreferences("widget_memo_prefs", Context.MODE_PRIVATE)
        val appWidgetManager = AppWidgetManager.getInstance(context)

        val keys = prefs.all.keys.filter { it.startsWith("memo_id_") }

        for (key in keys) {
            val widgetId = key.removePrefix("memo_id_").toIntOrNull() ?: continue
            val savedMemoId = prefs.getInt("memo_id_$widgetId", -1)
            Log.e("TAG", "handleWidgetUpdateOrClear: 2")

            if (memo.id == savedMemoId) {
                if (memo.isDeleted) {
                    Log.e("TAG", "handleWidgetUpdateOrClear: 3")

                    prefs.edit {
                        remove("memo_id_$widgetId")
                            .remove("memo_content_$widgetId")
                    }
                } else {
                    Log.e("TAG", "handleWidgetUpdateOrClear: 4")
                    prefs.edit {
                        putString("memo_content_$widgetId", memo.content)
                    }
                }

                appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.lv_widget_memo)
                SimpleMemoWidget.updateWidget(context, appWidgetManager, widgetId)
            }
        }
    }

    fun deleteAllMemos() {
        viewModelScope.launch {
            memoDao.deleteAllMemos()
        }
    }

    fun renameFolder(oldName: String, newName: String) {
        viewModelScope.launch {
            memoDao.updateFolderName(oldName, newName)
        }
    }

    fun moveMemosToDefault(folderName: String) {
        viewModelScope.launch {
            memoDao.moveMemosToDefault(folderName)
        }
    }
}
