package com.simple.memo.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.simple.memo.data.model.MemoEntity

@Dao
interface MemoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemo(memo: MemoEntity)

    @Update
    suspend fun updateMemo(memo: MemoEntity)

    @Delete
    suspend fun deleteMemo(memo: MemoEntity)

    @Query("SELECT * FROM memos WHERE id = :id")
    suspend fun getMemoById(id: Int): MemoEntity?

    @Query("SELECT * FROM memos WHERE isDeleted = 0 ORDER BY date DESC")
    fun observeAllActiveMemos(): LiveData<List<MemoEntity>>

    @Query("SELECT * FROM memos WHERE isDeleted = 1 ORDER BY date DESC")
    fun observeAllActiveDeleteMemos(): LiveData<List<MemoEntity>>

    @Query("SELECT * FROM memos WHERE isDeleted = 0 AND content LIKE '%' || :keyword || '%' ORDER BY date DESC")
    fun searchMemos(keyword: String): LiveData<List<MemoEntity>>

    @Query("DELETE FROM memos WHERE isDeleted = 1 AND date < :thresholdDate")
    suspend fun deleteOldTrashMemos(thresholdDate: String)

    @Query("SELECT * FROM memos WHERE isDeleted = 0 AND folderName = :folderName ORDER BY date DESC")
    fun observeMemosByFolder(folderName: String): LiveData<List<MemoEntity>>

    @Query("DELETE FROM memos")
    suspend fun deleteAllMemos()

    @Query("UPDATE memos SET folderName = :newName WHERE folderName = :oldName")
    suspend fun updateFolderName(oldName: String, newName: String)

    @Query("UPDATE memos SET folderName = '' WHERE folderName = :deletedFolder")
    suspend fun moveMemosToDefault(deletedFolder: String)

    @Query("SELECT * FROM memos WHERE isDeleted = 0 AND folderName = :folder AND content LIKE :keyword ORDER BY date DESC")
    fun searchMemosInFolder(folder: String, keyword: String): LiveData<List<MemoEntity>>

}