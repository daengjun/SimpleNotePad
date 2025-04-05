package com.simple.memo.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.simple.memo.data.local.MemoDatabase
import com.simple.memo.data.model.MemoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MemoViewModel(application: Application) : AndroidViewModel(application) {

    private val memoDao = MemoDatabase.getDatabase(application).memoDao()
    val allMemos: LiveData<List<MemoEntity>> = memoDao.observeAllActiveMemos()
    val deleteAllMemos: LiveData<List<MemoEntity>> = memoDao.observeAllActiveDeleteMemos()

    fun insertMemo(memo: MemoEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            memoDao.insertMemo(memo)
        }
    }

    fun updateMemo(memo: MemoEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            memoDao.updateMemo(memo)
        }
    }

    fun deleteMemo(memo: MemoEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            memoDao.deleteMemo(memo)
        }
    }


    fun searchMemos(keyword: String): LiveData<List<MemoEntity>> {
        return memoDao.searchMemos(keyword)
    }


    /*
    * 휴지통 설정한 날짜에 따라서 메모 자동 삭제
    * */
    fun autoDeleteOldTrash(cycle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.e("TAG", "autoDeleteOldTrash : $cycle")
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

}
