package com.simon.lifepocket.vm

import android.content.Intent
import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.simon.lifepocket.LifePocketApp
import com.simon.lifepocket.data.Category
import com.simon.lifepocket.data.CategorySum
import com.simon.lifepocket.data.Photo
import com.simon.lifepocket.data.Record
import com.simon.lifepocket.data.SubCategory
import com.simon.lifepocket.data.TaskGroup
import com.simon.lifepocket.data.TaskGroupWithItems
import com.simon.lifepocket.data.TaskItem
import com.simon.lifepocket.util.LpMonth
import com.simon.lifepocket.util.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class RecordViewModel(app: Application) : ViewModel() {
    private val db = (app as LifePocketApp).database
    private val categoryDao = db.categoryDao()
    private val subDao = db.subCategoryDao()
    private val recordDao = db.recordDao()

    private val _month = MutableStateFlow(LpMonth.now())
    val month: StateFlow<LpMonth.YMonth> = _month

    val categories: Flow<List<Category>> = categoryDao.observeAll()

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthRecords: StateFlow<List<Record>> = _month
        .flatMapLatest { m ->
            val range = m.epochRange()
            recordDao.observeMonth(range.first, range.last)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val expenseTotal: StateFlow<Long> = _month
        .flatMapLatest { m -> recordDao.sumBetween(0, m.epochRange().first, m.epochRange().last) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    @OptIn(ExperimentalCoroutinesApi::class)
    val incomeTotal: StateFlow<Long> = _month
        .flatMapLatest { m -> recordDao.sumBetween(1, m.epochRange().first, m.epochRange().last) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    @OptIn(ExperimentalCoroutinesApi::class)
    val prevExpenseTotal: StateFlow<Long> = _month
        .flatMapLatest { m ->
            val range = m.prev().epochRange()
            recordDao.sumBetween(0, range.first, range.last)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    @OptIn(ExperimentalCoroutinesApi::class)
    val categorySums: StateFlow<List<CategorySum>> = _month
        .flatMapLatest { m -> recordDao.categorySums(m.epochRange().first, m.epochRange().last) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun prevMonth() { _month.value = _month.value.prev() }
    fun nextMonth() { _month.value = _month.value.next() }

    fun subsFor(categoryId: Long): Flow<List<SubCategory>> = subDao.observeByCategory(categoryId)

    fun addSubIfNeeded(categoryId: Long, name: String) {
        viewModelScope.launch(Dispatchers.IO) { subDao.addIfMissing(categoryId, name) }
    }

    fun addRecord(type: Int, amountCents: Long, categoryId: Long, subName: String, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            recordDao.insert(
                Record(
                    type = type,
                    amountCents = amountCents,
                    categoryId = categoryId,
                    subName = subName,
                    note = note,
                    dateEpochDay = LocalDate.now().toEpochDay(),
                    createdAt = System.currentTimeMillis(),
                )
            )
        }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { recordDao.delete(id) }
    }

    fun addCategory(name: String, emoji: String) {
        viewModelScope.launch(Dispatchers.IO) {
            categoryDao.insert(Category(name = name, emoji = emoji.ifBlank { "🏷" }, sort = categoryDao.count()))
        }
    }

    /** 删除大类；其下还有账目时拒绝（返回 false） */
    fun deleteCategory(id: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = recordDao.countInCategory(id) == 0
            if (ok) categoryDao.delete(id)
            launch(Dispatchers.Main) { onResult(ok) }
        }
    }

    /** 导出全部账目为 CSV 文本 */
    suspend fun exportCsv(): String {
        val records = recordDao.getAll()
        val categories = categoryDao.getAll().associateBy { it.id }
        val rows = mutableListOf(listOf("日期", "类型", "大类", "小类", "备注", "金额(元)"))
        for (r in records) {
            rows.add(
                listOf(
                    LocalDate.ofEpochDay(r.dateEpochDay).toString(),
                    if (r.type == 0) "支出" else "收入",
                    categories[r.categoryId]?.name ?: "已删除",
                    r.subName,
                    r.note,
                    Money.yuan(r.amountCents),
                )
            )
        }
        return com.simon.lifepocket.util.Csv.build(rows)
    }
}

class TaskViewModel(app: Application) : ViewModel() {
    private val db = (app as LifePocketApp).database
    private val groupDao = db.taskGroupDao()
    private val itemDao = db.taskItemDao()

    val taskCards: Flow<List<TaskGroupWithItems>> = combine(
        groupDao.observeAll(),
        itemDao.observeAll(),
    ) { groups, items ->
        groups.map { g -> TaskGroupWithItems(g, items.filter { it.groupId == g.id }) }
    }

    fun addGroup(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            groupDao.insert(TaskGroup(name = name, createdAt = System.currentTimeMillis()))
        }
    }

    fun renameGroup(id: Long, name: String) {
        viewModelScope.launch(Dispatchers.IO) { groupDao.rename(id, name) }
    }

    fun deleteGroup(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { groupDao.delete(id) }
    }

    fun addItem(groupId: Long, title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val sort = itemDao.countInGroup(groupId).toLong()
            itemDao.insert(TaskItem(groupId = groupId, title = title, sort = sort, createdAt = System.currentTimeMillis()))
        }
    }

    fun toggleItem(item: TaskItem) {
        viewModelScope.launch(Dispatchers.IO) { itemDao.update(item.copy(done = !item.done)) }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { itemDao.delete(id) }
    }
}

class PhotoViewModel(app: Application) : ViewModel() {
    private val application = app
    private val photoDao = (app as LifePocketApp).database.photoDao()
    private val prefs = app.getSharedPreferences("lifepocket", android.content.Context.MODE_PRIVATE)

    private val _intervalSec = MutableStateFlow(prefs.getInt("film_interval", 30))
    val intervalSec: StateFlow<Int> = _intervalSec

    val photos: Flow<List<Photo>> = photoDao.observeAll()

    fun setInterval(sec: Int) {
        prefs.edit().putInt("film_interval", sec).apply()
        _intervalSec.value = sec
    }

    /** 保存系统选图器返回的 URI（取持久化读权限，无需任何存储权限） */
    fun addUris(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            for (uri in uris) {
                try {
                    application.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                    // 拿不到持久化权限也先存着，失效时界面上会显示占位
                }
                photoDao.insert(Photo(uri = uri.toString(), addedAt = System.currentTimeMillis()))
            }
        }
    }

    fun deletePhoto(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { photoDao.delete(id) }
    }
}

class AppVmFactory(private val app: LifePocketApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        RecordViewModel::class.java -> RecordViewModel(app) as T
        TaskViewModel::class.java -> TaskViewModel(app) as T
        PhotoViewModel::class.java -> PhotoViewModel(app) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: $modelClass")
    }
}
