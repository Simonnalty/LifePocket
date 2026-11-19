package com.simon.lifepocket.ui

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
import com.simon.lifepocket.ui.film.FilmScreen
import com.simon.lifepocket.ui.record.RecordScreen
import com.simon.lifepocket.ui.settings.SettingsScreen
import com.simon.lifepocket.ui.task.TaskScreen
import com.simon.lifepocket.ui.theme.Hairline
import com.simon.lifepocket.ui.theme.Ink
import com.simon.lifepocket.ui.theme.Paper
import com.simon.lifepocket.ui.theme.Seal
import com.simon.lifepocket.ui.theme.SubInk
import com.simon.lifepocket.vm.AppVmFactory

private val TABS = listOf(
    "record" to "账目",
    "tasks" to "事",
    "film" to "影",
)

@Composable
fun Root() {
    val nav = rememberNavController()
    val app = LocalContext.current.applicationContext as LifePocketApp
    val factory = remember { AppVmFactory(app) }
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "record"
    val showBar = route == "record" || route == "tasks" || route == "film"

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
                            val active = route == r ||
                                (r == "record" && route == "record")
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
            startDestination = "record",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding(),
        ) {
            composable("record") {
                RecordScreen(
                    vm = viewModel(factory = factory),
                    onOpenSettings = { nav.navigate("settings") },
                )
            }
            composable("tasks") {
                TaskScreen(vm = viewModel(factory = factory))
            }
            composable("film") {
                FilmScreen(vm = viewModel(factory = factory))
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
