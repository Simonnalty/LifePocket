package com.simon.lifepocket.ui.home

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.simon.lifepocket.data.Photo
import com.simon.lifepocket.data.TaskGroupWithItems
import com.simon.lifepocket.ui.theme.CardWhite
import com.simon.lifepocket.ui.theme.Hairline
import com.simon.lifepocket.ui.theme.Ink
import com.simon.lifepocket.ui.theme.Paper
import com.simon.lifepocket.ui.theme.Seal
import com.simon.lifepocket.ui.theme.SubInk
import com.simon.lifepocket.util.AiSettings
import com.simon.lifepocket.util.LlmClient
import com.simon.lifepocket.vm.PhotoViewModel
import com.simon.lifepocket.vm.TaskViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * 主界面：上面是胶片相册（轮换的精选照片），下面是待办事项；记账在「账」页。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(photoVm: PhotoViewModel, taskVm: TaskViewModel) {
    val photos by photoVm.photos.collectAsStateWithLifecycle(initialValue = emptyList())
    val intervalSec by photoVm.intervalSec.collectAsStateWithLifecycle()
    val cards by taskVm.taskCards.collectAsStateWithLifecycle(initialValue = emptyList())
    var idx by remember { mutableStateOf(0) }
    var pendingDeletePhoto by remember { mutableStateOf<Photo?>(null) }
    var showNewTask by remember { mutableStateOf(false) }
    var pendingGroup by remember { mutableStateOf<TaskGroupWithItems?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAiTask by remember { mutableStateOf(false) }
    var aiTaskText by remember { mutableStateOf("") }
    var aiTaskBusy by remember { mutableStateOf(false) }
    var aiTaskMsg by remember { mutableStateOf("") }
    var aiTaskResult by remember { mutableStateOf<List<Pair<String, List<String>>>>(emptyList()) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> if (uris.isNotEmpty()) photoVm.addUris(uris) }

    // 定时轮换（手动模式下不启动）
    LaunchedEffect(photos.size, intervalSec) {
        if (intervalSec > 0 && photos.size > 1) {
            while (true) {
                delay(intervalSec * 1000L)
                idx = (idx + 1) % photos.size
            }
        }
    }
    if (idx >= photos.size && photos.isNotEmpty()) idx = 0

    val pending = cards.sumOf { it.totalCount - it.doneCount }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "口袋管家",
                fontSize = 21.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif,
                letterSpacing = 3.sp,
                color = Ink,
            )
            Spacer(Modifier.weight(1f))
            Text(
                if (pending > 0) "$pending 件待办" else "都完成啦",
                fontSize = 12.5.sp,
                color = SubInk,
            )
        }

        // ---------- 上：胶片相册 ----------
        Box(
            Modifier
                .fillMaxWidth()
                .weight(0.42f),
            contentAlignment = Alignment.Center,
        ) {
            if (photos.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("还没有精选的照片", fontSize = 14.5.sp, color = SubInk, fontFamily = FontFamily.Serif)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "＋ 从相册选照片",
                        fontSize = 13.5.sp,
                        color = Paper,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Ink)
                            .clickable { picker.launch(arrayOf("image/*")) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }
            } else {
                val safeIdx = idx.coerceIn(0, photos.lastIndex)
                // 整叠照片做淡入淡出：i 是新叠最上面那张的下标，露角的卡片就是它的下一批
                Crossfade(
                    targetState = safeIdx,
                    animationSpec = tween(220),
                    label = "filmStack",
                ) { i ->
                    val f = photos[i.coerceIn(0, photos.lastIndex)]
                    val a = photos[(i + 1) % photos.size]
                    val b = photos[(i + 2) % photos.size]
                    Box(
                        Modifier
                            .fillMaxSize()
                            // 垂直滑动切照片：上滑=下一张（露角那张），下滑=上一张
                            .pointerInput(Unit) {
                                var acc = 0f
                                detectVerticalDragGestures(
                                    onVerticalDrag = { change, dragAmount ->
                                        acc += dragAmount
                                        change.consume()
                                    },
                                    onDragEnd = {
                                        if (acc <= -60) idx = (idx + 1) % photos.size
                                        else if (acc >= 60) idx = (idx - 1 + photos.size) % photos.size
                                        acc = 0f
                                    },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        // 下层两张：露角更明显的那张（右下、探出更多）就是「下一张」，
                        // 上滑后出现的正是它；另一张（左下小露边）是下下张。
                        // API 31+ 真模糊，低版本降级为降不透明度。
                        PhotoCard(
                            photo = a,
                            width = 218.dp,
                            imageHeight = 186.dp,
                            modifier = Modifier
                                .offset(x = 10.dp, y = 30.dp)
                                .graphicsLayer { rotationZ = 2.5f; alpha = 0.85f }
                                .then(if (Build.VERSION.SDK_INT >= 31) Modifier.blur(3.dp) else Modifier),
                        )
                        PhotoCard(
                            photo = b,
                            width = 218.dp,
                            imageHeight = 186.dp,
                            modifier = Modifier
                                .offset(x = (-10).dp, y = 16.dp)
                                .graphicsLayer { rotationZ = -2.5f; alpha = 0.85f }
                                .then(if (Build.VERSION.SDK_INT >= 31) Modifier.blur(3.dp) else Modifier),
                        )
                        // 上层一张：清晰，点按换下一张，长按移出
                        PhotoCard(
                            photo = f,
                            width = 238.dp,
                            imageHeight = 204.dp,
                            modifier = Modifier
                                .combinedClickable(
                                    onClick = { idx = (i + 1) % photos.size },
                                    onLongClick = { pendingDeletePhoto = f },
                                ),
                            caption = "${i + 1} / ${photos.size}",
                        )
                    }
                }
            }
        }
        // 轮换控制 + 选照片
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                listOf(0 to "手动", 30 to "30秒", 60 to "1分", 300 to "5分").forEach { (sec, label) ->
                    Text(
                        label,
                        fontSize = 12.5.sp,
                        fontWeight = if (intervalSec == sec) FontWeight.Bold else FontWeight.Normal,
                        color = if (intervalSec == sec) Ink else SubInk,
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .clickable { photoVm.setInterval(sec) }
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                "＋ 照片",
                fontSize = 13.sp,
                color = Seal,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .clickable { picker.launch(arrayOf("image/*")) }
                    .padding(horizontal = 6.dp, vertical = 6.dp),
            )
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))

        // ---------- 下：待办事项 ----------
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "事",
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif,
                letterSpacing = 6.sp,
                color = Ink,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "长按任务可重命名或删除",
                fontSize = 11.5.sp,
                color = SubInk,
            )
        }

        if (cards.isEmpty()) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("还没有任务", fontSize = 14.5.sp, color = SubInk, fontFamily = FontFamily.Serif)
                Spacer(Modifier.height(6.dp))
                Text("任务1、任务2…… 每个任务下面记要做的小事", fontSize = 12.5.sp, color = SubInk)
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(cards, key = { it.group.id }) { card ->
                    TaskCard(
                        card = card,
                        vm = taskVm,
                        onGroupLongPress = { pendingGroup = card },
                    )
                }
                item { Spacer(Modifier.height(70.dp)) }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .border(1.dp, Hairline, RoundedCornerShape(3.dp))
                    .clickable { showNewTask = true },
                contentAlignment = Alignment.Center,
            ) {
                Text("＋ 新建一个任务", fontSize = 13.5.sp, color = Ink, letterSpacing = 1.sp)
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .border(1.dp, if (AiSettings.ready(context)) Ink else Hairline, RoundedCornerShape(3.dp))
                    .background(if (AiSettings.ready(context)) CardWhite else Color.Transparent)
                    .clickable { showAiTask = true },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "AI 拆任务",
                    fontSize = 13.5.sp,
                    color = if (AiSettings.ready(context)) Ink else SubInk,
                    letterSpacing = 1.sp,
                )
            }
        }
    }

    if (showAiTask) {
        AlertDialog(
            onDismissRequest = { if (!aiTaskBusy) showAiTask = false },
            title = { Text("AI 拆任务", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (aiTaskResult.isEmpty()) {
                        TextField(
                            value = aiTaskText,
                            onValueChange = { aiTaskText = it.take(600) },
                            placeholder = {
                                Text(
                                    "把整段计划粘贴进来，自动拆成任务和小事\n例如：下个月装修新家，要量房、找装修公司、定预算；12月英语考试，要背单词、刷真题",
                                    fontSize = 12.5.sp,
                                    color = SubInk,
                                )
                            },
                            minLines = 4,
                            maxLines = 8,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Ink,
                                unfocusedIndicatorColor = Hairline,
                                cursorColor = Seal,
                            ),
                        )
                        if (aiTaskMsg.isNotEmpty()) {
                            Text(aiTaskMsg, fontSize = 12.sp, color = SubInk, modifier = Modifier.padding(top = 8.dp))
                        }
                    } else {
                        // 预览：确认后才真正创建
                        Column(
                            Modifier
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            aiTaskResult.forEach { (g, items) ->
                                Text("▪ $g", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
                                items.forEach { Text("　· $it", fontSize = 13.sp, color = SubInk) }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = if (aiTaskResult.isEmpty()) {
                        aiTaskText.isNotBlank() && !aiTaskBusy && AiSettings.ready(context)
                    } else !aiTaskBusy,
                    onClick = {
                        if (aiTaskResult.isEmpty()) {
                            aiTaskBusy = true
                            aiTaskMsg = ""
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val sys = "你是待办清单规划师。把用户的一大段计划拆分成若干个大任务，每个大任务下面列出要做的具体小事。\n" +
                                        "只输出一个 JSON 数组，不要解释：" +
                                        "[{\"group\":\"任务名(不超过12字)\",\"items\":[\"小事1\",\"小事2\"]}]\n" +
                                        "要求：最多 8 个任务组，每组最多 15 条小事；小事是可执行的短句（不超过15字）；全部用中文；按常识合理拆分。"
                                    val raw = LlmClient.chat(context, sys, aiTaskText)
                                    val arr = JSONArray(LlmClient.stripFence(raw))
                                    val parsed = mutableListOf<Pair<String, List<String>>>()
                                    for (k in 0 until arr.length()) {
                                        val o = arr.getJSONObject(k)
                                        val g = o.optString("group", "").trim()
                                        val items = mutableListOf<String>()
                                        val ia = o.optJSONArray("items")
                                        if (ia != null) for (m in 0 until ia.length()) items.add(ia.optString(m, "").trim())
                                        if (g.isNotEmpty() && items.isNotEmpty()) parsed.add(g to items)
                                    }
                                    withContext(Dispatchers.Main) {
                                        if (parsed.isEmpty()) aiTaskMsg = "没解析出任务，换个说法再试一次"
                                        else aiTaskResult = parsed
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) { aiTaskMsg = "解析失败：${e.message?.take(60)}" }
                                } finally {
                                    withContext(Dispatchers.Main) { aiTaskBusy = false }
                                }
                            }
                        } else {
                            val snapshot = aiTaskResult
                            aiTaskBusy = true
                            taskVm.addGroupsFromAi(snapshot) { g, i ->
                                Toast.makeText(context, "已创建 $g 个任务、$i 条小事 ✓", Toast.LENGTH_SHORT).show()
                                showAiTask = false
                                aiTaskBusy = false
                                aiTaskText = ""
                                aiTaskResult = emptyList()
                                aiTaskMsg = ""
                            }
                        }
                    },
                ) {
                    Text(
                        if (aiTaskResult.isEmpty()) (if (aiTaskBusy) "解析中…" else "AI 拆解") else "确认创建",
                        color = Seal,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !aiTaskBusy,
                    onClick = { showAiTask = false; aiTaskResult = emptyList(); aiTaskMsg = "" },
                ) { Text(if (aiTaskResult.isEmpty()) "取消" else "返回修改", color = SubInk) }
            },
        )
    }

    pendingDeletePhoto?.let { p ->
        AlertDialog(
            onDismissRequest = { pendingDeletePhoto = null },
            title = { Text("移出这张照片？", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = { Text("只从口袋管家里移出，不会删除相册原图。", fontSize = 13.5.sp, color = SubInk) },
            confirmButton = {
                TextButton(onClick = {
                    photoVm.deletePhoto(p.id)
                    pendingDeletePhoto = null
                }) { Text("移出", color = Seal) }
            },
            dismissButton = { TextButton(onClick = { pendingDeletePhoto = null }) { Text("取消") } },
        )
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
                        taskVm.addGroup(name.trim())
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
                        taskVm.deleteGroup(card.group.id)
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
                        taskVm.renameGroup(card.group.id, newName.trim())
                        renaming = false
                        pendingGroup = null
                    }) { Text("保存", color = Seal) }
                },
                dismissButton = { TextButton(onClick = { renaming = false; pendingGroup = null }) { Text("取消") } },
            )
        }
    }
}

/** 胶片卡片：白边相纸 + 圆角照片（紧凑版） */
@Composable
private fun PhotoCard(
    photo: Photo,
    width: androidx.compose.ui.unit.Dp,
    imageHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    caption: String = "",
) {
    Column(
        modifier
            .width(width)
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(CardWhite)
            .padding(10.dp)
    ) {
        AsyncImage(
            model = photo.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0xFFEDE8DC)),
        )
        if (caption.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("精选", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = Ink)
                Spacer(Modifier.weight(1f))
                Text(caption, fontSize = 11.sp, color = SubInk)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskCard(card: TaskGroupWithItems, vm: TaskViewModel, onGroupLongPress: () -> Unit) {
    var adding by remember(card.group.id) { mutableStateOf(false) }
    var newText by remember(card.group.id) { mutableStateOf("") }

    Column(Modifier.padding(top = 20.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = {}, onLongClick = onGroupLongPress)
                .padding(bottom = 9.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                card.group.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif,
                letterSpacing = 2.sp,
                color = Ink,
                modifier = Modifier.weight(1f),
            )
            Text("${card.doneCount} / ${card.totalCount}", fontSize = 12.sp, color = SubInk)
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(Ink))

        card.items.forEach { item ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = { vm.toggleItem(item) }, onLongClick = { vm.deleteItem(item.id) })
                    .padding(vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(19.dp)
                        .background(if (item.done) Seal else Color.Transparent)
                        .border(1.dp, if (item.done) Seal else SubInk),
                    contentAlignment = Alignment.Center,
                ) {
                    if (item.done) Text("✓", fontSize = 11.sp, color = Color(0xFFFAF8F2), fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(13.dp))
                Text(
                    item.title,
                    fontSize = 14.5.sp,
                    color = if (item.done) SubInk else Ink,
                    textDecoration = if (item.done) TextDecoration.LineThrough else null,
                )
            }
        }

        if (adding) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = newText,
                    onValueChange = { newText = it.take(30) },
                    placeholder = { Text("要做的一件小事", fontSize = 13.sp, color = SubInk) },
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
                    .padding(vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("＋", color = Seal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text("添加一件小事", fontSize = 12.5.sp, color = SubInk)
            }
        }
    }
}
