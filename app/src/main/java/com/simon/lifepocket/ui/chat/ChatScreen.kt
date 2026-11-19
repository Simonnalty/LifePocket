package com.simon.lifepocket.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import com.simon.lifepocket.vm.ChatViewModel

/** AI 陪伴页：自定义名字的人设助手。聊天内容会发送给所选模型服务商（默认 GLM-4-Flash）。 */
@Composable
fun ChatScreen(vm: ChatViewModel) {
    val messages by vm.messages.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val ready = AiSettings.ready(context)

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.size - 1)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                vm.personaName,
                fontSize = 21.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif,
                letterSpacing = 3.sp,
                color = Ink,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "陪伴助手 · 聊天会发送给 AI 服务商",
                fontSize = 11.5.sp,
                color = SubInk,
            )
        }

        if (!ready) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .background(CardWhite)
                    .padding(14.dp)
            ) {
                Text(
                    "先到「账」页右上角 ⚙ 设置里开启 AI 助手并填入 Key，然后就能聊天了。",
                    fontSize = 13.sp,
                    color = SubInk,
                    lineHeight = 20.sp,
                )
            }
        }

        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
        ) {
            if (messages.isEmpty() && ready) {
                item {
                    Column(Modifier.fillMaxWidth().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "嗨，我是${vm.personaName}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Serif,
                            color = Ink,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "想聊什么都可以 · 问我这个月花了多少钱也行",
                            fontSize = 12.5.sp,
                            color = SubInk,
                        )
                    }
                }
            }
            items(messages) { m ->
                val isUser = m.role == "user"
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
                ) {
                    Text(
                        m.text,
                        fontSize = 14.5.sp,
                        lineHeight = 22.sp,
                        color = if (isUser) Paper else Ink,
                        modifier = Modifier
                            .widthIn(max = 300.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = if (isUser) 12.dp else 3.dp,
                                    topEnd = if (isUser) 3.dp else 12.dp,
                                    bottomEnd = 12.dp,
                                    bottomStart = 12.dp,
                                )
                            )
                            .background(if (isUser) Ink else CardWhite)
                            .padding(horizontal = 13.dp, vertical = 10.dp),
                    )
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        // 输入行
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BasicTextField(
                value = input,
                onValueChange = { input = it.take(200) },
                enabled = ready,
                textStyle = TextStyle(fontSize = 14.5.sp, color = Ink),
                cursorBrush = SolidColor(Seal),
                modifier = Modifier
                    .weight(1f)
                    .background(CardWhite)
                    .border(1.dp, Hairline)
                    .padding(horizontal = 13.dp, vertical = 11.dp),
            )
            Box(
                Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (input.isNotBlank() && ready) Ink else Hairline)
                    .clickable(enabled = input.isNotBlank() && ready) {
                        vm.send(input)
                        input = ""
                    }
                    .padding(horizontal = 16.dp, vertical = 11.dp),
            ) {
                Text(
                    "发送",
                    fontSize = 13.5.sp,
                    color = if (input.isNotBlank() && ready) Paper else SubInk,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
