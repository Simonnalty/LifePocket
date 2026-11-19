package com.simon.lifepocket.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.simon.lifepocket.LifePocketApp
import com.simon.lifepocket.ui.home.HomeScreen
import com.simon.lifepocket.ui.record.RecordScreen
import com.simon.lifepocket.ui.settings.SettingsScreen
import com.simon.lifepocket.ui.theme.Hairline
import com.simon.lifepocket.ui.theme.Ink
import com.simon.lifepocket.ui.theme.Paper
import com.simon.lifepocket.ui.theme.Seal
import com.simon.lifepocket.ui.theme.SubInk
import com.simon.lifepocket.vm.AppVmFactory

private val TABS = listOf(
    "home" to "家",
    "record" to "账",
)

@Composable
fun Root() {
    val nav = rememberNavController()
    val app = LocalContext.current.applicationContext as LifePocketApp
    val factory = remember { AppVmFactory(app) }
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "home"
    val showBar = route == "home" || route == "record"

    Scaffold(
        containerColor = Paper,
        bottomBar = {
            if (showBar) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Paper)
                        .navigationBarsPadding()
                ) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .padding(horizontal = 30.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TABS.forEach { (r, label) ->
                            val active = route == r
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clickable {
                                        nav.navigate(r) {
                                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    androidx.compose.material3.Text(
                                        label,
                                        fontSize = 16.sp,
                                        letterSpacing = 6.sp,
                                        color = if (active) Ink else SubInk,
                                        fontWeight = if (active) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                                    )
                                    Box(
                                        Modifier
                                            .padding(top = 5.dp)
                                            .height(2.dp)
                                            .fillMaxWidth(0.5f)
                                            .background(if (active) Seal else androidx.compose.ui.graphics.Color.Transparent)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "home",
            // 快速转场：替代默认 700ms 淡入淡出，全局只留 120ms，操作更跟手
            enterTransition = { fadeIn(tween(120)) },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = { fadeIn(tween(120)) },
            popExitTransition = { fadeOut(tween(120)) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding(),
        ) {
            composable("home") {
                HomeScreen(
                    photoVm = viewModel(factory = factory),
                    taskVm = viewModel(factory = factory),
                )
            }
            composable("record") {
                RecordScreen(
                    vm = viewModel(factory = factory),
                    onOpenSettings = { nav.navigate("settings") },
                )
            }
            composable("settings") {
                SettingsScreen(
                    vm = viewModel(factory = factory),
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}
