package com.simon.lifepocket.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simon.lifepocket.ui.theme.Hairline
import com.simon.lifepocket.ui.theme.Ink
import com.simon.lifepocket.ui.theme.Seal
import com.simon.lifepocket.ui.theme.SubInk
import com.simon.lifepocket.vm.RecordViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(vm: RecordViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val categories by vm.categories.collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddCat by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                val csv = vm.exportCsv()
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(csv.toByteArray(Charsets.UTF_8))
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "已导出 ✓", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "导出失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp)
    ) {
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "‹ 返回",
                fontSize = 14.sp,
                color = SubInk,
                modifier = Modifier.clickable { onBack() },
            )
            Spacer(Modifier.weight(1f))
            Text("设置", fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Serif, letterSpacing = 6.sp, color = Ink)
            Spacer(Modifier.weight(1.2f))
        }

        SectionLabel("数据")
        SettingRow(
            title = "导出记账 CSV",
            sub = "全部账目 · 日期/类型/大类/小类/备注/金额",
            onClick = { exportLauncher.launch("口袋管家_记账导出.csv") },
        )
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .background(Hairline.copy(alpha = 0.55f))
                .padding(14.dp)
        ) {
            Text(
                "隐私：本应用不申请任何网络权限，所有数据只保存在你的手机上。你可以在「应用信息 → 权限」里验证。",
                fontSize = 12.5.sp,
                color = SubInk,
                lineHeight = 20.sp,
            )
        }

        SectionLabel("分类管理")
        categories.forEach { c ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${c.emoji}  ${c.name}", fontSize = 16.sp, color = Ink)
                Spacer(Modifier.weight(1f))
                Text(
                    "删除",
                    fontSize = 13.sp,
                    color = Seal,
                    modifier = Modifier.clickable {
                        vm.deleteCategory(c.id) { ok ->
                            Toast.makeText(
                                context,
                                if (ok) "已删除「${c.name}」" else "「${c.name}」下还有账目，先处理账目再删除",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { showAddCat = true }
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("＋", color = Seal, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text("添加大类（如：医疗、宠物）", fontSize = 13.5.sp, color = SubInk)
        }

        SectionLabel("关于")
        AboutRow("版本", "1.0.0")
        AboutRow("开源协议", "MIT License（源码见仓库）")
        AboutRow("设计", "「白 · 手账」· huashu-design 三方向选定")
        AboutRow("致谢", "BeeCount / Veri Fin / Taskfern 等开源项目")
        Spacer(Modifier.height(60.dp))
    }

    if (showAddCat) {
        var emoji by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCat = false },
            title = { Text("添加大类", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    TextField(
                        value = emoji,
                        onValueChange = { emoji = it.take(2) },
                        placeholder = { Text("图标（如 🏥）", fontSize = 14.sp, color = SubInk) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Ink,
                            unfocusedIndicatorColor = Hairline,
                            cursorColor = Seal,
                        ),
                    )
                    TextField(
                        value = name,
                        onValueChange = { name = it.take(8) },
                        placeholder = { Text("名称（如 医疗）", fontSize = 14.sp, color = SubInk) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Ink,
                            unfocusedIndicatorColor = Hairline,
                            cursorColor = Seal,
                        ),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        vm.addCategory(name.trim(), emoji.trim())
                        showAddCat = false
                    }
                ) { Text("添加", color = Seal) }
            },
            dismissButton = { TextButton(onClick = { showAddCat = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 12.5.sp,
        letterSpacing = 6.sp,
        color = SubInk,
        modifier = Modifier.padding(top = 34.dp, bottom = 4.dp),
    )
    Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
}

@Composable
private fun SettingRow(title: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = Ink)
            Text(sub, fontSize = 12.5.sp, color = SubInk, modifier = Modifier.padding(top = 3.dp))
        }
        Text("›", fontSize = 16.sp, color = SubInk)
    }
}

@Composable
private fun AboutRow(title: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 14.sp, color = SubInk)
        Spacer(Modifier.weight(1f))
        Text(value, fontSize = 14.sp, color = Ink, textAlign = androidx.compose.ui.text.style.TextAlign.Right)
    }
}
