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
import com.simon.lifepocket.util.AiSettings
import com.simon.lifepocket.util.LlmClient
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class RecordViewModel(app: Application) : ViewModel() {
    private val db = (app as LifePocketApp).database
    private val categoryDao = db.categoryDao()
    private val subDao = db.subCategoryDao()
    private val recordDao = db.recordDao()
    private val prefs = app.getSharedPreferences("lifepocket", android.content.Context.MODE_PRIVATE)

    /** 上次记账用的收/支类型（0 支出 / 1 收入），打开弹层时用作默认 */
    fun lastType(): Int = prefs.getInt("last_type", 0)

    /** 上次记账用的大类 id，-1 表示还没有记录过 */
    fun lastCatId(): Long = prefs.getLong("last_cat_id", -1L)

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
        prefs.edit().putInt("last_type", type).putLong("last_cat_id", categoryId).apply()
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

    /** 最新分组快照（供 AI 拆任务时做同名去重） */
    private var cachedGroups: List<TaskGroup> = emptyList()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            groupDao.observeAll().collect { cachedGroups = it }
        }
    }

    val taskCards: Flow<List<TaskGroupWithItems>> = combine(
        groupDao.observeAll(),
        itemDao.observeAll(),
    ) { groups, items ->
        groups.map { g -> TaskGroupWithItems(g, items.filter { it.groupId == g.id }) }
    }

    /**
     * AI 拆任务：批量创建任务组+小任务；同名组合并（小任务追加到现有组末尾）。
     * 上限 10 组 × 20 条。返回 (实际新建组数, 实际新增条目数)。
     */
    fun addGroupsFromAi(groups: List<Pair<String, List<String>>>, onDone: (Int, Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var newGroups = 0
            var newItems = 0
            val existing = cachedGroups.associateBy { it.name }
            val created = mutableMapOf<String, Long>()
            for ((name, items) in groups.take(10)) {
                val cleanName = name.trim().take(24)
                if (cleanName.isEmpty()) continue
                val gid = existing[cleanName]?.id
                    ?: created[cleanName]
                    ?: run {
                        val id = groupDao.insert(TaskGroup(name = cleanName, createdAt = System.currentTimeMillis()))
                        created[cleanName] = id
                        newGroups++
                        id
                    }
                var sort = itemDao.countInGroup(gid).toLong()
                for (title in items.take(20)) {
                    val t = title.trim().take(30)
                    if (t.isEmpty()) continue
                    itemDao.insert(
                        TaskItem(groupId = gid, title = t, sort = sort, createdAt = System.currentTimeMillis())
                    )
                    sort++
                    newItems++
                }
            }
            launch(Dispatchers.Main) { onDone(newGroups, newItems) }
        }
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

/** 一条聊天消息：role "user" / "assistant" */
class ChatMessage(val role: String, val text: String)

/**
 * AI 陪伴助手：人设（用户自定义名字）+ 近期对话上下文 + 本月支出聚合统计（只发数字，不发明细）。
 * 历史仅保留在内存（本次会话），不出网存档。
 */
class ChatViewModel(app: Application) : ViewModel() {
    private val appRef = app
    private val recordDao = (app as LifePocketApp).database.recordDao()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    val personaName: String get() = AiSettings.name(appRef)
    private var busy = false

    fun send(text: String) {
        val clean = text.trim()
        if (busy || clean.isEmpty()) return
        busy = true
        _messages.value = _messages.value + ChatMessage("user", clean)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val persona = "你是「${AiSettings.name(appRef)}」，用户的私人随身助手。" +
                    "性格温暖、会倾听、观察细腻、偶尔带一点轻幽默，说话简短自然（一般不超过三句），像老朋友一样。" +
                    "可以适当用 emoji。用户会跟你聊生活和记账；涉及钱的话题只能基于给你的聚合统计，不要编造明细。" +
                    "如果用户情绪低落，先共情再鼓励，不敷衍；遇到明显的极端情绪表达，温和地建议寻求身边人或专业帮助。"
                val history = _messages.value.dropLast(1).takeLast(16)
                    .joinToString("\n") { "${if (it.role == "user") "用户" else "你"}：${it.text.take(120)}" }
                val stats = buildStatsLine()
                val system = buildString {
                    append(persona)
                    if (stats.isNotBlank()) append("\n\n[用户记账聚合统计，仅供自然聊天，不外传] ").append(stats)
                    if (history.isNotBlank()) append("\n\n[最近的对话]\n").append(history)
                }
                val reply = LlmClient.chat(appRef, system, clean, temperature = 0.7)
                _messages.value = _messages.value + ChatMessage("assistant", reply)
            } catch (e: Exception) {
                _messages.value = _messages.value +
                    ChatMessage("assistant", "（出了点小状况：${e.message?.take(80) ?: "未知错误"}，稍后再试试）")
            } finally {
                busy = false
            }
        }
    }

    /** 只输出聚合数字（本月支出总额），明细永不进入 Prompt */
    private suspend fun buildStatsLine(): String = try {
        val range = LpMonth.now().epochRange()
        val expense = recordDao.sumBetween(0, range.first, range.last).first()
        if (expense > 0) "本月支出合计 ¥${Money.yuan(expense)}" else ""
    } catch (_: Exception) {
        ""
    }
}

class AppVmFactory(private val app: LifePocketApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        RecordViewModel::class.java -> RecordViewModel(app) as T
        TaskViewModel::class.java -> TaskViewModel(app) as T
        PhotoViewModel::class.java -> PhotoViewModel(app) as T
        ChatViewModel::class.java -> ChatViewModel(app) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: $modelClass")
    }
}
