package com.simon.lifepocket.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simon.lifepocket.ui.theme.CardWhite
import com.simon.lifepocket.ui.theme.Hairline
import com.simon.lifepocket.ui.theme.Ink
import com.simon.lifepocket.ui.theme.Paper
import com.simon.lifepocket.ui.theme.Seal
import com.simon.lifepocket.ui.theme.SubInk
import com.simon.lifepocket.util.AiSettings
import com.simon.lifepocket.util.LlmClient
import com.simon.lifepocket.vm.RecordViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.roundToLong
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddRecordSheet(vm: RecordViewModel, onDismiss: () -> Unit) {
    var type by remember { mutableStateOf(vm.lastType()) } // 0 支出 / 1 收入
    var amountText by remember { mutableStateOf("") }
    val categories by vm.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    var catId by remember { mutableStateOf(-1L) } // -1 = 等待分类列表到位，再用上次用的大类
    var catReady by remember { mutableStateOf(false) }
    var subSel by remember { mutableStateOf("") }
    var customMode by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var aiInput by remember { mutableStateOf("") }
    var aiBusy by remember { mutableStateOf(false) }
    var aiMsg by remember { mutableStateOf("") }

    LaunchedEffect(categories) {
        if (!catReady && categories.isNotEmpty()) {
            catId = categories.firstOrNull { it.id == vm.lastCatId() }?.id ?: categories.first().id
            catReady = true
        }
    }

    val subs = if (catReady && catId > 0) vm.subsFor(catId).collectAsStateWithLifecycle(initialValue = emptyList()).value else emptyList()

    // 一句话 → LLM 抽取 → 回填表单（用户确认后才保存）
    fun aiParse() {
        val sentence = aiInput.trim()
        if (sentence.isEmpty() || aiBusy) return
        aiBusy = true
        aiMsg = ""
        scope.launch(Dispatchers.IO) {
            try {
                val cats = vm.categories.first()
                val catList = cats.joinToString("、") { it.name }
                val sys = "你是记账解析器。从用户这句话里抽取一笔账，只输出一个 JSON 对象，不要解释、不要多余文字。\n" +
                    "格式：{\"type\":0,\"amount_yuan\":12.5,\"category\":\"吃\",\"sub\":\"午饭\",\"note\":\"简短备注\"}\n" +
                    "type：0=支出，1=收入；amount_yuan：数字（元，可带小数）；\n" +
                    "category：必须从这些大类里选一个：" + catList + "；实在不属于任何一类就选\"" +
                    (cats.firstOrNull()?.name ?: "其他") + "\"；\n" +
                    "sub：小类，从原话提炼（如 午饭、奶茶、打车），没有就空字符串；note：不超过15字的简短备注，没有就空字符串。\n" +
                    "今天日期：" + LocalDate.now()
                val raw = LlmClient.chat(context, sys, sentence)
                val j = JSONObject(LlmClient.stripFence(raw))
                val t = if (j.optInt("type", 0) == 1) 1 else 0
                val yuan = j.optDouble("amount_yuan", 0.0)
                val catName = j.optString("category", "").trim()
                val sub = j.optString("sub", "").trim()
                val aiNote = j.optString("note", "").trim()
                withContext(Dispatchers.Main) {
                    type = t
                    amountText = if (yuan > 0) String.format("%.2f", yuan).trimEnd('0').trimEnd('.') else ""
                    cats.firstOrNull { it.name == catName }?.let { c ->
                        catId = c.id
                        catReady = true
                        subSel = ""
                        if (sub.isNotBlank()) {
                            vm.addSubIfNeeded(c.id, sub.take(12))
                            subSel = sub.take(12)
                        }
                    }
                    note = (if (aiNote.isNotBlank()) aiNote else sentence).take(30)
                    aiMsg = "已解析 ✓ 检查无误后点「保存」"
                    aiInput = ""
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { aiMsg = "解析失败：${e.message?.take(60) ?: "未知错误"}" }
            } finally {
                withContext(Dispatchers.Main) { aiBusy = false }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Paper,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 30.dp)
                .padding(bottom = 34.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("记一笔", fontSize = 19.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Serif, letterSpacing = 4.sp, color = Ink)
                Spacer(Modifier.weight(1f))
                Text("收起", fontSize = 13.sp, color = SubInk, modifier = Modifier.clickable { onDismiss() })
            }

            // ---- AI 帮我记：一句话 → 自动填好类型/金额/大类/小类/备注，确认后保存 ----
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("AI 帮我记", fontSize = 12.5.sp, letterSpacing = 5.sp, color = SubInk)
                Spacer(Modifier.weight(1f))
                if (!AiSettings.ready(context)) {
                    Text(
                        "到设置开启 AI",
                        fontSize = 12.sp,
                        color = Seal,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onDismiss() },
                    )
                }
            }
            if (AiSettings.ready(context)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = aiInput,
                        onValueChange = { aiInput = it.take(60) },
                        textStyle = TextStyle(fontSize = 13.5.sp, color = Ink),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .background(CardWhite)
                            .border(1.dp, Hairline)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        cursorBrush = SolidColor(Seal),
                    )
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (aiInput.isNotBlank() && !aiBusy) Ink else Hairline)
                            .clickable(enabled = aiInput.isNotBlank() && !aiBusy) { aiParse() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(
                            if (aiBusy) "解析中…" else "解析",
                            fontSize = 13.sp,
                            color = if (aiInput.isNotBlank() && !aiBusy) Paper else SubInk,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (aiMsg.isNotEmpty()) {
                    Text(
                        aiMsg,
                        fontSize = 12.sp,
                        color = if (aiMsg.startsWith("已解析")) Seal else SubInk,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                listOf(0 to "支出", 1 to "收入").forEach { (t, label) ->
                    Column(Modifier.clickable { type = t }) {
                        Text(
                            label,
                            fontSize = 14.sp,
                            fontWeight = if (type == t) FontWeight.Bold else FontWeight.Normal,
                            color = if (type == t) Ink else SubInk,
                        )
                        Box(
                            Modifier
                                .padding(top = 5.dp)
                                .width(28.dp)
                                .height(2.dp)
                                .background(if (type == t) Seal else Color.Transparent)
                        )
                    }
                }
            }

            Row(
                Modifier.padding(top = 22.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text("¥", fontSize = 24.sp, color = SubInk, fontFamily = FontFamily.Serif, modifier = Modifier.padding(bottom = 6.dp))
                BasicTextField(
                    value = amountText,
                    onValueChange = { v ->
                        if (v.matches(Regex("\\d{0,7}(\\.\\d{0,2})?"))) amountText = v
                    },
                    textStyle = TextStyle(
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Serif,
                        color = Ink,
                    ),
                    cursorBrush = SolidColor(Seal),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.weight(1f),
                )
                Text("今天", fontSize = 12.5.sp, color = SubInk, modifier = Modifier.padding(bottom = 10.dp))
            }

            Text("大类 · 点按切换", fontSize = 12.5.sp, letterSpacing = 5.sp, color = SubInk, modifier = Modifier.padding(top = 26.dp))
            FlowRow(
                Modifier.padding(top = 12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                categories.forEach { c ->
                    val active = catId == c.id
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, if (active) Ink else Hairline, RoundedCornerShape(4.dp))
                            .background(if (active) CardWhite else Color.Transparent)
                            .clickable {
                                catId = c.id
                                subSel = ""
                            }
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${c.emoji} ", fontSize = 14.sp)
                        Text(
                            c.name,
                            fontSize = 15.sp,
                            fontWeight = if (active) FontWeight.Black else FontWeight.SemiBold,
                            fontFamily = FontFamily.Serif,
                            color = if (active) Ink else SubInk,
                        )
                    }
                }
            }

            Text("小类 · 可自定", fontSize = 12.5.sp, letterSpacing = 5.sp, color = SubInk, modifier = Modifier.padding(top = 24.dp))
            Row(
                Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (customMode) {
                    BasicTextField(
                        value = customText,
                        onValueChange = { customText = it.take(12) },
                        textStyle = TextStyle(fontSize = 13.5.sp, color = Ink),
                        singleLine = true,
                        modifier = Modifier
                            .width(120.dp)
                            .background(CardWhite)
                            .border(1.dp, Ink)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        cursorBrush = SolidColor(Seal),
                    )
                    Text(
                        "确定",
                        fontSize = 13.5.sp,
                        color = Seal,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                val name = customText.trim()
                                if (name.isNotEmpty() && catId != 0L) {
                                    vm.addSubIfNeeded(catId, name)
                                    subSel = name
                                }
                                customMode = false
                                customText = ""
                            }
                            .padding(vertical = 8.dp)
                    )
                } else {
                    if (subs.isEmpty()) {
                        Text(
                            "该大类还没有小类 · 可点「＋自定义」添加，或直接保存",
                            fontSize = 12.5.sp,
                            color = SubInk,
                            modifier = Modifier.padding(vertical = 10.dp),
                        )
                    }
                    subs.forEach { s ->
                        Box(
                            Modifier
                                .clickable { subSel = s.name }
                                .background(if (subSel == s.name) CardWhite else Color.Transparent)
                                .border(1.dp, if (subSel == s.name) Ink else Hairline)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(s.name, fontSize = 13.5.sp, color = if (subSel == s.name) Ink else SubInk)
                        }
                    }
                    Box(
                        Modifier
                            .clickable { customMode = true }
                            .border(1.dp, Hairline)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("＋自定义", fontSize = 13.5.sp, color = SubInk)
                    }
                }
            }
            if (subSel.isNotEmpty()) {
                Text(
                    "已选：$subSel",
                    fontSize = 12.sp,
                    color = SubInk,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 26.dp)
                    .background(CardWhite),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = note,
                    onValueChange = { note = it.take(30) },
                    placeholder = { Text("备注（选填）", fontSize = 13.5.sp, color = SubInk) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Hairline,
                        unfocusedIndicatorColor = Hairline,
                        cursorColor = Seal,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            val cents = (amountText.toDoubleOrNull()?.times(100))?.roundToLong() ?: 0L
            val canSave = cents > 0 && catId > 0
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .height(52.dp)
                    .background(if (canSave) Ink else Hairline)
                    .clickable(enabled = canSave) {
                        vm.addRecord(
                            type = type,
                            amountCents = cents,
                            categoryId = catId,
                            subName = subSel.ifBlank { categories.firstOrNull { it.id == catId }?.name ?: "未分类" },
                            note = note.trim(),
                        )
                        onDismiss()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "保  存",
                    fontSize = 16.sp,
                    letterSpacing = 10.sp,
                    color = if (canSave) Paper else SubInk,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
