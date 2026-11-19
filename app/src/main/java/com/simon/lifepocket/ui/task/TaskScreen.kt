package com.simon.lifepocket.ui.task

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simon.lifepocket.data.TaskGroupWithItems
import com.simon.lifepocket.ui.theme.Hairline
import com.simon.lifepocket.ui.theme.Ink
import com.simon.lifepocket.ui.theme.Seal
import com.simon.lifepocket.ui.theme.SubInk
import com.simon.lifepocket.vm.TaskViewModel

@Composable
fun TaskScreen(vm: TaskViewModel) {
    val cards by vm.taskCards.collectAsStateWithLifecycle(initialValue = emptyList())
    var showNewTask by remember { mutableStateOf(false) }
    var pendingGroup by remember { mutableStateOf<TaskGroupWithItems?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp)
    ) {
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("事", fontSize = 26.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Serif, letterSpacing = 8.sp, color = Ink)
            Spacer(Modifier.weight(1f))
            val pending = cards.sumOf { it.totalCount - it.doneCount }
            Text(if (pending > 0) "$pending 件待办" else "都完成啦", fontSize = 12.5.sp, color = SubInk)
        }

        if (cards.isEmpty()) {
            Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(90.dp))
                Text("还没有任务", fontSize = 15.sp, color = SubInk, fontFamily = FontFamily.Serif)
                Spacer(Modifier.height(8.dp))
                Text("任务1、任务2…… 每个任务下面记要做的小事", fontSize = 13.sp, color = SubInk)
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(cards, key = { it.group.id }) { card ->
                    TaskCard(
                        card = card,
                        vm = vm,
                        onGroupLongPress = { pendingGroup = card },
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .height(50.dp)
                .border(1.dp, Hairline, RoundedCornerShape(3.dp))
                .clickable { showNewTask = true },
            contentAlignment = Alignment.Center,
        ) {
            Text("＋ 新建一个任务", fontSize = 14.5.sp, color = Ink, letterSpacing = 2.sp)
        }
    }

    if (showNewTask) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewTask = false },
            title = { Text("新建任务", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                TextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    placeholder = { Text("比如：装修布置新家", fontSize = 14.sp, color = SubInk) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Ink,
                        unfocusedIndicatorColor = Hairline,
                        cursorColor = Seal,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        vm.addGroup(name.trim())
                        showNewTask = false
                    }
                ) { Text("创建", color = Seal) }
            },
            dismissButton = { TextButton(onClick = { showNewTask = false }) { Text("取消") } },
        )
    }

    pendingGroup?.let { card ->
        var renaming by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { pendingGroup = null },
            title = { Text(card.group.name, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = { Text("管理这个任务（${card.doneCount}/${card.totalCount}）", fontSize = 13.5.sp, color = SubInk) },
            confirmButton = {
                TextButton(onClick = { renaming = true }) { Text("重命名", color = Ink) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        vm.deleteGroup(card.group.id)
                        pendingGroup = null
                    }) { Text("删除", color = Seal) }
                    TextButton(onClick = { pendingGroup = null }) { Text("取消") }
                }
            },
        )
        if (renaming) {
            var newName by remember { mutableStateOf(card.group.name) }
            AlertDialog(
                onDismissRequest = { renaming = false; pendingGroup = null },
                title = { Text("重命名任务", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
                text = {
                    TextField(
                        value = newName,
                        onValueChange = { newName = it.take(24) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Ink,
                            unfocusedIndicatorColor = Hairline,
                            cursorColor = Seal,
                        ),
                    )
                },
                confirmButton = {
                    TextButton(enabled = newName.isNotBlank(), onClick = {
                        vm.renameGroup(card.group.id, newName.trim())
                        renaming = false
                        pendingGroup = null
                    }) { Text("保存", color = Seal) }
                },
                dismissButton = { TextButton(onClick = { renaming = false; pendingGroup = null }) { Text("取消") } },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskCard(card: TaskGroupWithItems, vm: TaskViewModel, onGroupLongPress: () -> Unit) {
    var adding by remember(card.group.id) { mutableStateOf(false) }
    var newText by remember(card.group.id) { mutableStateOf("") }

    Column(Modifier.padding(top = 24.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = {}, onLongClick = onGroupLongPress)
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                card.group.name,
                fontSize = 18.5.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif,
                letterSpacing = 2.sp,
                color = Ink,
                modifier = Modifier.weight(1f),
            )
            Text("${card.doneCount} / ${card.totalCount}", fontSize = 12.5.sp, color = SubInk)
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(Ink))

        card.items.forEach { item ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = { vm.toggleItem(item) }, onLongClick = { vm.deleteItem(item.id) })
                    .padding(vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(20.dp)
                        .background(if (item.done) Seal else Color.Transparent)
                        .border(1.dp, if (item.done) Seal else SubInk),
                    contentAlignment = Alignment.Center,
                ) {
                    if (item.done) Text("✓", fontSize = 12.sp, color = Color(0xFFFAF8F2), fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    item.title,
                    fontSize = 15.5.sp,
                    color = if (item.done) SubInk else Ink,
                    textDecoration = if (item.done) TextDecoration.LineThrough else null,
                )
            }
        }

        if (adding) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = newText,
                    onValueChange = { newText = it.take(30) },
                    placeholder = { Text("要做的一件小事", fontSize = 13.5.sp, color = SubInk) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Ink,
                        unfocusedIndicatorColor = Hairline,
                        cursorColor = Seal,
                    ),
                    modifier = Modifier.weight(1f),
                )
                TextButton(enabled = newText.isNotBlank(), onClick = {
                    vm.addItem(card.group.id, newText.trim())
                    newText = ""
                    adding = false
                }) { Text("添加", color = Seal) }
            }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { adding = true }
                    .padding(vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("＋", color = Seal, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text("添加一件小事", fontSize = 13.sp, color = SubInk)
            }
        }
    }
}
