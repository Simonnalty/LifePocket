package com.simon.lifepocket.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import com.simon.lifepocket.vm.RecordViewModel
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordSheet(vm: RecordViewModel, onDismiss: () -> Unit) {
    var type by remember { mutableStateOf(0) } // 0 支出 / 1 收入
    var amountText by remember { mutableStateOf("") }
    val categories by vm.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    var catId by remember(categories) { mutableStateOf(categories.firstOrNull()?.id ?: 0L) }
    var subSel by remember { mutableStateOf("") }
    var customMode by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val subs = if (catId != 0L) vm.subsFor(catId).collectAsStateWithLifecycle(initialValue = emptyList()).value else emptyList()

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

            Text("大类", fontSize = 12.5.sp, letterSpacing = 5.sp, color = SubInk, modifier = Modifier.padding(top = 26.dp))
            Row(
                Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                categories.forEach { c ->
                    Column(Modifier.clickable {
                        catId = c.id
                        subSel = ""
                    }) {
                        Text(
                            c.name,
                            fontSize = 17.sp,
                            fontWeight = if (catId == c.id) FontWeight.Black else FontWeight.SemiBold,
                            fontFamily = FontFamily.Serif,
                            color = if (catId == c.id) Ink else SubInk,
                        )
                        Box(
                            Modifier
                                .padding(top = 4.dp)
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(if (catId == c.id) Seal else Color.Transparent)
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
            val canSave = cents > 0 && catId != 0L
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
