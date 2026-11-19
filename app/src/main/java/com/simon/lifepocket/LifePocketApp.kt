package com.simon.lifepocket

import android.app.Application
import androidx.room.Room
import com.simon.lifepocket.data.AppDatabase
import com.simon.lifepocket.data.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LifePocketApp : Application() {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "lifepocket.db").build()
    }

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch { seedCategoriesIfNeeded() }
    }

    /** 首次启动预置五个大类（可在设置里增删） */
    private suspend fun seedCategoriesIfNeeded() {
        val dao = database.categoryDao()
        if (dao.count() > 0) return
        dao.insertAll(
            listOf(
                Category(name = "吃", emoji = "🍗", sort = 0),
                Category(name = "穿", emoji = "👕", sort = 1),
                Category(name = "住", emoji = "🏠", sort = 2),
                Category(name = "行", emoji = "🚗", sort = 3),
                Category(name = "学习", emoji = "📚", sort = 4),
            )
        )
    }
}
