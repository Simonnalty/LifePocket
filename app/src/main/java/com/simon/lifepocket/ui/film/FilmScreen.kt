package com.simon.lifepocket.ui.film

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.simon.lifepocket.data.Photo
import com.simon.lifepocket.ui.theme.CardWhite
import com.simon.lifepocket.ui.theme.Paper
import com.simon.lifepocket.ui.theme.Ink
import com.simon.lifepocket.ui.theme.Seal
import com.simon.lifepocket.ui.theme.SubInk
import com.simon.lifepocket.vm.PhotoViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilmScreen(vm: PhotoViewModel) {
    val photos by vm.photos.collectAsStateWithLifecycle(initialValue = emptyList())
    val intervalSec by vm.intervalSec.collectAsStateWithLifecycle()
    var idx by remember { mutableStateOf(0) }
    var pendingDelete by remember { mutableStateOf<Photo?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> if (uris.isNotEmpty()) vm.addUris(uris) }

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

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp)
    ) {
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("影", fontSize = 26.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Serif, letterSpacing = 8.sp, color = Ink)
            Spacer(Modifier.weight(1f))
            Text(
                "＋ 选照片",
                fontSize = 13.5.sp,
                color = Seal,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { picker.launch(arrayOf("image/*")) },
            )
        }
        Text(
            if (intervalSec == 0) "手动模式 · 点按切换" else "每 ${intervalSec} 秒换一张 · 点按切换",
            fontSize = 12.5.sp,
            color = SubInk,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            listOf(0 to "手动", 30 to "30秒", 60 to "1分", 300 to "5分").forEach { (sec, label) ->
                Column(Modifier.clickable { vm.setInterval(sec) }) {
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = if (intervalSec == sec) FontWeight.Bold else FontWeight.Normal,
                        color = if (intervalSec == sec) Ink else SubInk,
                    )
                    Box(
                        Modifier
                            .padding(top = 3.dp)
                            .width(28.dp)
                            .height(2.dp)
                            .background(if (intervalSec == sec) Seal else Color.Transparent)
                    )
                }
            }
        }

        if (photos.isEmpty()) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("还没有精选的照片", fontSize = 15.sp, color = SubInk, fontFamily = FontFamily.Serif)
                Spacer(Modifier.height(10.dp))
                Text(
                    "从相册挑几张喜欢的，摆成一叠会轮换的胶片",
                    fontSize = 13.sp,
                    color = SubInk,
                )
                Spacer(Modifier.height(22.dp))
                Text(
                    "＋ 从相册选照片",
                    fontSize = 14.5.sp,
                    color = Paper,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Ink)
                        .clickable { picker.launch(arrayOf("image/*")) }
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                )
            }
        } else {
            val safeIdx = idx.coerceIn(0, photos.lastIndex)
            val front = photos[safeIdx]
            val backA = photos[(safeIdx + 1) % photos.size]
            val backB = photos[(safeIdx + 2) % photos.size]

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                // 下层两张：偏移 + 旋转 + 虚化（API 31+ 才有真模糊，低版本降级为降低不透明度）
                PhotoCard(
                    photo = backA,
                    width = 288.dp,
                    imageHeight = 280.dp,
                    modifier = Modifier
                        .offset(x = (-14).dp, y = 20.dp)
                        .graphicsLayer { rotationZ = -2.5f; alpha = 0.8f }
                        .then(if (Build.VERSION.SDK_INT >= 31) Modifier.blur(3.dp) else Modifier),
                    caption = "",
                    counter = "",
                )
                PhotoCard(
                    photo = backB,
                    width = 288.dp,
                    imageHeight = 280.dp,
                    modifier = Modifier
                        .offset(x = 14.dp, y = 42.dp)
                        .graphicsLayer { rotationZ = 2.5f; alpha = 0.8f }
                        .then(if (Build.VERSION.SDK_INT >= 31) Modifier.blur(3.dp) else Modifier),
                    caption = "",
                    counter = "",
                )
                // 上层一张：清晰
                PhotoCard(
                    photo = front,
                    width = 314.dp,
                    imageHeight = 316.dp,
                    modifier = Modifier
                        .combinedClickable(
                            onClick = { idx = (safeIdx + 1) % photos.size },
                            onLongClick = { pendingDelete = front },
                        ),
                    caption = "${safeIdx + 1} / ${photos.size} · ${LocalDate.now().year} 年的精选",
                    counter = "",
                )
            }

            Text(
                "精选的照片 · 只留在手机里（长按可移出）",
                fontSize = 12.sp,
                color = SubInk,
                letterSpacing = 3.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 22.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }

    pendingDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("移出这张照片？", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = { Text("只从口袋管家里移出，不会删除相册原图。", fontSize = 13.5.sp, color = SubInk) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deletePhoto(p.id)
                    pendingDelete = null
                }) { Text("移出", color = Seal) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun PhotoCard(
    photo: Photo,
    width: androidx.compose.ui.unit.Dp,
    imageHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    caption: String,
    counter: String,
) {
    Column(
        modifier
            .width(width)
            .shadow(8.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(CardWhite)
            .padding(12.dp)
    ) {
        AsyncImage(
            model = photo.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFEDE8DC)),
        )
        if (caption.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("精选", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = Ink)
                Spacer(Modifier.weight(1f))
                Text(caption, fontSize = 12.sp, color = SubInk)
            }
        }
    }
}
