package com.simon.lifepocket.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        Category::class,
        SubCategory::class,
        Record::class,
        TaskGroup::class,
        TaskItem::class,
        Photo::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun subCategoryDao(): SubCategoryDao
    abstract fun recordDao(): RecordDao
    abstract fun taskGroupDao(): TaskGroupDao
    abstract fun taskItemDao(): TaskItemDao
    abstract fun photoDao(): PhotoDao
}
