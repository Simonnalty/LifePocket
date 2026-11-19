package com.simon.lifepocket.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simon.lifepocket.data.Category
import com.simon.lifepocket.data.CategorySum
import com.simon.lifepocket.data.Record
import com.simon.lifepocket.util.LpMonth
import com.simon.lifepocket.util.Money
import com.simon.lifepocket.ui.theme.CardWhite
import com.simon.lifepocket.ui.theme.Hairline
import com.simon.lifepocket.ui.theme.Ink
import com.simon.lifepocket.ui.theme.Paper
import com.simon.lifepocket.ui.theme.Seal
import com.simon.lifepocket.ui.theme.SubInk
import com.simon.lifepocket.vm.RecordViewModel
import java.time.LocalDate

@Composable
fun RecordScreen(vm: RecordViewModel, onOpenSettings: () -> Unit) {
    var view by remember { mutableStateOf("detail") } // detail / stats
    var showSheet by remember { mutableStateOf(false) }

    val month by vm.month.collectAsStateWithLifecycle()
    val records by vm.monthRecords.collectAsStateWithLifecycle()
    val expense by vm.expenseTotal.collectAsStateWithLifecycle()
    val income by vm.incomeTotal.collectAsStateWithLifecycle()
    val prevExpense by vm.prevExpenseTotal.collectAsStateWithLifecycle()
    val catSums by vm.categorySums.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle(initialValue = emptyList())

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 26.dp)
        ) {
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "口袋管家",
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Serif,
                    color = Ink,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "⚙",
                    fontSize = 18.sp,
                    color = SubInk,
                    modifier = Modifier.clickable { onOpenSettings() },
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("‹", fontSize = 16.sp, color = SubInk, modifier = Modifier.clickable { vm.prevMonth() })
                    Text(
                        month.label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    Text("›", fontSize = 16.sp, color = SubInk, modifier = Modifier.clickable { vm.nextMonth() })
                }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    TabLink("账目", view == "detail") { view = "detail" }
                    TabLink("统计", view == "stats") { view = "stats" }
                }
            }

            Text(
                "本月支出 ¥${Money.yuan(expense)} · 收入 ¥${Money.yuan(income)}",
                fontSize = 14.sp,
                color = SubInk,
                modifier = Modifier.padding(top = 12.dp),
            )

            if (view == "detail") {
                DetailList(records, categories, onDelete = { vm.deleteRecord(it.id) })
            } else {
                StatsContent(
                    month = month,
                    records = records,
                    expense = expense,
                    income = income,
                    prevExpense = prevExpense,
                    catSums = catSums,
                    categories = categories,
                )
            }
        }

        // 「记」按钮：墨色方块，全 App 唯一入口
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 26.dp)
                .size(54.dp)
                .background(Ink)
                .clickable { showSheet = true },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "记",
                color = Paper,
                fontSize = 23.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif,
            )
        }
    }

    if (showSheet) {
        AddRecordSheet(vm = vm, onDismiss = { showSheet = false })
    }
}

@Composable
private fun TabLink(label: String, active: Boolean, onClick: () -> Unit) {
    Column(Modifier.clickable { onClick() }) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) Ink else SubInk,
        )
        Box(
            Modifier
                .padding(top = 4.dp)
                .height(2.dp)
                .width(24.dp)
                .background(if (active) Seal else androidx.compose.ui.graphics.Color.Transparent)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DetailList(records: List<Record>, categories: List<Category>, onDelete: (Record) -> Unit) {
    val catMap = remember(categories) { categories.associateBy { it.id } }
    var pendingDelete by remember { mutableStateOf<Record?>(null) }
    val today = LocalDate.now().toEpochDay()

    if (records.isEmpty()) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(90.dp))
            Text("这个月还没有记录", fontSize = 15.sp, color = SubInk, fontFamily = FontFamily.Serif)
            Spacer(Modifier.height(8.dp))
            Text("点右下角「记」，写下第一笔", fontSize = 13.sp, color = SubInk)
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize()) {
        records.groupBy { it.dateEpochDay }.forEach { (day, list) ->
            item(key = "h$day") { DayHeader(day, list, today) }
            items(list, key = { it.id }) { r ->
                val cat = catMap[r.categoryId]
                RecordRow(
                    record = r,
                    categoryLabel = cat?.let { "${it.emoji} ${it.name}" } ?: "🏷 已删除分类",
                    onLongPress = { pendingDelete = r },
                )
            }
        }
        item { Spacer(Modifier.height(90.dp)) }
    }

    pendingDelete?.let { r ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这笔账？", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = { Text("${r.subName} ¥${Money.yuan(r.amountCents)}", fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(r)
                    pendingDelete = null
                }) { Text("删除", color = Seal) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun DayHeader(day: Long, list: List<Record>, today: Long) {
    val date = LocalDate.ofEpochDay(day)
    val week = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")[date.dayOfWeek.value - 1]
    val label = when (day) {
        today -> "今天"
        today - 1 -> "昨天"
        else -> "${date.monthValue}月${date.dayOfMonth}日 · $week"
    }
    val inc = list.filter { it.type == 1 }.sumOf { it.amountCents }
    val exp = list.filter { it.type == 0 }.sumOf { it.amountCents }
    val net = inc - exp
    val netLabel = when {
        net > 0 -> "+¥${Money.yuan(net)}"
        net < 0 -> "−¥${Money.yuan(-net)}"
        else -> ""
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 12.5.sp, letterSpacing = 3.sp, color = SubInk)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f).height(1.dp).background(Hairline))
        if (netLabel.isNotEmpty()) {
            Text(
                netLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (day == today) Seal else SubInk,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecordRow(record: Record, categoryLabel: String, onLongPress: () -> Unit) {
    val sign = if (record.type == 0) "−¥" else "+¥"
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = {}, onLongClick = onLongPress)
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (record.note.isBlank()) record.subName else "${record.subName} · ${record.note}",
                    fontSize = 16.sp,
                    color = Ink,
                )
                Text(
                    categoryLabel,
                    fontSize = 12.5.sp,
                    letterSpacing = 2.sp,
                    color = SubInk,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                sign + Money.yuan(record.amountCents),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = Ink,
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
    }
}

@Composable
private fun StatsContent(
    month: LpMonth.YMonth,
    records: List<Record>,
    expense: Long,
    income: Long,
    prevExpense: Long,
    catSums: List<CategorySum>,
    categories: List<Category>,
) {
    val catMap = remember(categories) { categories.associateBy { it.id } }
    val rows = remember(catSums, categories) {
        catSums.map { s -> (catMap[s.categoryId]?.name ?: "已删除") to s.total }
    }
    val topSubs = remember(records) {
        records.filter { it.type == 0 }
            .groupBy { it.subName }
            .map { it.key to it.value.size }
            .sortedByDescending { it.second }
            .take(2)
    }
    val isCurrentMonth = month == LpMonth.now()
    val daysElapsed = if (isCurrentMonth) LocalDate.now().dayOfMonth else month.firstDay().lengthOfMonth()
    val avgPerDay = if (daysElapsed > 0) expense / daysElapsed else 0L
    val deltaPct = if (prevExpense > 0) ((expense - prevExpense) * 100 / prevExpense) else null

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Spacer(Modifier.height(26.dp))
            Text("${month.label} · 钱去了哪里", fontSize = 12.5.sp, letterSpacing = 5.sp, color = SubInk)
            Text(
                "¥${Money.yuan(expense)}",
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif,
                color = Ink,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                "收入 ¥${Money.yuan(income)} · 结余 ¥${Money.yuan(income - expense)}",
                fontSize = 13.sp,
                color = SubInk,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        item {
            Spacer(Modifier.height(24.dp))
            rows.forEachIndexed { i, (name, total) ->
                val pct = if (expense > 0) (total.toDouble() / expense).coerceIn(0.0, 1.0) else 0.0
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .background(CardWhite)
                            .border(1.dp, Ink),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(name.take(1), fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Serif, color = Ink)
                    }
                    Spacer(Modifier.width(14.dp))
                    Box(
                        Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(Hairline)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(pct.toFloat())
                                .height(4.dp)
                                .background(Ink.copy(alpha = 0.75f))
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Text("¥${Money.yuan(total)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(
                        "${(pct * 100).toInt()}%",
                        fontSize = 12.5.sp,
                        color = SubInk,
                        modifier = Modifier.width(40.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Right,
                    )
                }
                if (i < rows.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
            }
        }
        item {
            Spacer(Modifier.height(22.dp))
            val subText = if (topSubs.isEmpty()) "还没有小类记录" else "最常记的小类 · " + topSubs.joinToString(" ") { "${it.first}（${it.second} 笔）" }
            Text(subText, fontSize = 13.sp, color = SubInk, lineHeight = 22.sp)
            val deltaText = when {
                deltaPct == null -> ""
                deltaPct >= 0 -> "比上月多 ${(deltaPct).toInt()}%"
                else -> "比上月少 ${(-deltaPct).toInt()}%"
            }
            Text(
                "平均每天 ¥${Money.yuan(avgPerDay)} $deltaText",
                fontSize = 13.sp,
                color = SubInk,
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(90.dp))
        }
    }
}
