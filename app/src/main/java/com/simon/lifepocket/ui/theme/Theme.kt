package com.simon.lifepocket.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 「白 · 手账」设计语言（huashu-design dir-2 方向，用户选定）
 * 暖白纸面（偏白不发黄）+ 墨色 + 一处朱砂。
 */
val Paper = Color(0xFFFAF8F2)      // 纸面底色
val CardWhite = Color(0xFFFFFFFF)  // 卡片 / 弹层
val Ink = Color(0xFF26221C)        // 墨色正文
val SubInk = Color(0xFF6E6756)     // 次级文字
val Hairline = Color(0xFFEBE6D8)   // 发丝分隔线
val Seal = Color(0xFFB5532A)       // 朱砂（全版面唯一的强调色）

@Composable
fun LifePocketTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Seal,
            onPrimary = CardWhite,
            background = Paper,
            onBackground = Ink,
            surface = CardWhite,
            onSurface = Ink,
            surfaceVariant = Paper,
            onSurfaceVariant = SubInk,
            outline = Hairline,
        ),
        content = content,
    )
}
