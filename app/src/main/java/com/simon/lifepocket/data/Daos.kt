package com.simon.lifepocket.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sort, id")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY sort, id")
    suspend fun getAll(): List<Category>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert
    suspend fun insert(c: Category): Long

    @Insert
    suspend fun insertAll(list: List<Category>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface SubCategoryDao {
    @Query("SELECT * FROM sub_categories WHERE categoryId = :categoryId ORDER BY id")
    fun observeByCategory(categoryId: Long): Flow<List<SubCategory>>

    @Query("SELECT id FROM sub_categories WHERE categoryId = :categoryId AND name = :name")
    suspend fun findId(categoryId: Long, name: String): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(s: SubCategory): Long

    /** 已存在则忽略，返回是否新增 */
    suspend fun addIfMissing(categoryId: Long, name: String): Boolean {
        if (findId(categoryId, name) != null) return false
        insert(SubCategory(categoryId = categoryId, name = name))
        return true
    }

    @Query("DELETE FROM sub_categories WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface RecordDao {
    @Query("SELECT * FROM records WHERE dateEpochDay BETWEEN :from AND :to ORDER BY dateEpochDay DESC, createdAt DESC")
    fun observeMonth(from: Long, to: Long): Flow<List<Record>>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM records WHERE type = :type AND dateEpochDay BETWEEN :from AND :to")
    fun sumBetween(type: Int, from: Long, to: Long): Flow<Long>

    @Query(
        "SELECT categoryId, COALESCE(SUM(amountCents), 0) AS total FROM records " +
            "WHERE type = 0 AND dateEpochDay BETWEEN :from AND :to " +
            "GROUP BY categoryId ORDER BY total DESC"
    )
    fun categorySums(from: Long, to: Long): Flow<List<CategorySum>>

    @Query("SELECT * FROM records ORDER BY dateEpochDay DESC, createdAt DESC")
    suspend fun getAll(): List<Record>

    @Query("SELECT COUNT(*) FROM records WHERE categoryId = :categoryId")
    suspend fun countInCategory(categoryId: Long): Int

    @Insert
    suspend fun insert(r: Record): Long

    @Update
    suspend fun update(r: Record)

    @Query("DELETE FROM records WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface TaskGroupDao {
    @Query("SELECT * FROM task_groups ORDER BY id")
    fun observeAll(): Flow<List<TaskGroup>>

    @Insert
    suspend fun insert(g: TaskGroup): Long

    @Query("UPDATE task_groups SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM task_groups WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface TaskItemDao {
    @Query("SELECT * FROM task_items ORDER BY sort, id")
    fun observeAll(): Flow<List<TaskItem>>

    @Query("SELECT COUNT(*) FROM task_items WHERE groupId = :groupId")
    suspend fun countInGroup(groupId: Long): Int

    @Insert
    suspend fun insert(i: TaskItem): Long

    @Update
    suspend fun update(i: TaskItem)

    @Query("DELETE FROM task_items WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos ORDER BY id")
    fun observeAll(): Flow<List<Photo>>

    @Insert
    suspend fun insert(p: Photo): Long

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun delete(id: Long)
}
