package ru.servermonitor.app.ui

import android.graphics.Color
import android.os.Bundle
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.json.JSONObject
import ru.servermonitor.app.R
import ru.servermonitor.app.data.model.Server
import ru.servermonitor.app.data.model.ServerEnvironment
import ru.servermonitor.app.data.model.json
import ru.servermonitor.app.data.model.parse
import ru.servermonitor.app.ui.colorpicker.ColorPickerRoute
import ru.servermonitor.app.ui.components.HsvColor
import ru.servermonitor.app.ui.editor.ServerEditorRoute
import ru.servermonitor.app.ui.navigation.ServerDestinations
import ru.servermonitor.app.ui.servers.ServerListScreen

private val ServerListSaver = listSaver<List<Server>, String>(
    save = { servers -> servers.map { it.json.toString() } },
    restore = { serialized -> serialized.mapNotNull { Server.parse(JSONObject(it)) } },
)

@Composable
fun ServerMonitorApp() {
    val navController = rememberNavController()
    var servers by rememberSaveable(stateSaver = ServerListSaver) { mutableStateOf(demoServers()) }
    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            NavHost(
                navController = navController,
                startDestination = ServerDestinations.LIST,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
            ) {
                composable(ServerDestinations.LIST) { entry ->
                    ServerListScreen(
                        servers = servers,
                        onOpenServer = { id ->
                            if (navController.isActive(entry)) {
                                navController.navigate(ServerDestinations.edit(id)) { launchSingleTop = true }
                            }
                        },
                        onCreateServer = {
                            if (navController.isActive(entry)) {
                                navController.navigate(ServerDestinations.EDIT) { launchSingleTop = true }
                            }
                        },
                        onDeleteServer = { id -> servers = servers.filterNot { it.id == id } },
                    )
                }
                composable(
                    route = ServerDestinations.EDIT_PATTERN,
                    arguments = listOf(navArgument(ServerDestinations.SERVER_ID) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }),
                ) { entry ->
                    val id = entry.arguments?.getString(ServerDestinations.SERVER_ID)
                    val server = servers.firstOrNull { it.id == id }
                    val cancel: () -> Unit = { if (navController.isActive(entry)) navController.popBackStack() }
                    if (id != null && server == null) {
                        MissingServerScreen(onBack = cancel)
                    } else {
                        ServerEditorRoute(
                            server = server,
                            backStackEntry = entry,
                            onSave = { changed ->
                                if (navController.isActive(entry)) {
                                    servers = if (servers.any { it.id == changed.id }) {
                                        servers.map { if (it.id == changed.id) changed else it }
                                    } else servers + changed
                                    navController.popBackStack()
                                }
                            },
                            onCancel = cancel,
                            onOpenColorPicker = { color ->
                                if (navController.isActive(entry)) {
                                    navController.navigate(ServerDestinations.colorPicker(color)) { launchSingleTop = true }
                                }
                            },
                        )
                    }
                }
                composable(
                    route = ServerDestinations.COLOR_PATTERN,
                    arguments = listOf(
                        navArgument(ServerDestinations.HUE) { type = NavType.FloatType },
                        navArgument(ServerDestinations.SATURATION) { type = NavType.FloatType },
                        navArgument(ServerDestinations.VALUE) { type = NavType.FloatType },
                    ),
                    enterTransition = {
                        slideInVertically(tween(280), initialOffsetY = { it })
                    },
                    popExitTransition = {
                        slideOutVertically(tween(280), targetOffsetY = { it })
                    },
                ) { entry ->
                    ColorPickerRoute(
                        color = HsvColor(
                            entry.arguments?.getFloat(ServerDestinations.HUE) ?: 0f,
                            entry.arguments?.getFloat(ServerDestinations.SATURATION) ?: 1f,
                            entry.arguments?.getFloat(ServerDestinations.VALUE) ?: 1f,
                        ),
                        onCancel = { if (navController.isActive(entry)) navController.popBackStack() },
                        onConfirm = { color ->
                            if (navController.isActive(entry)) {
                                navController.previousBackStackEntry?.savedStateHandle?.set(
                                    ServerDestinations.COLOR_RESULT,
                                    Bundle().apply {
                                        putFloat(ServerDestinations.HUE, color.hue)
                                        putFloat(ServerDestinations.SATURATION, color.saturation)
                                        putFloat(ServerDestinations.VALUE, color.value)
                                    },
                                )
                                navController.popBackStack()
                            }
                        },
                    )
                }
            }
        }
    }
}

private fun NavHostController.isActive(entry: NavBackStackEntry): Boolean =
    currentBackStackEntry == entry && entry.lifecycle.currentState == Lifecycle.State.RESUMED

@Composable
private fun MissingServerScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.server_not_found), style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onBack) { Text(stringResource(R.string.back_to_servers)) }
    }
}

private fun demoServers(): List<Server> = listOf(
    Server(
        name = "Учебный сервер", host = "127.0.0.1", port = 8080,
        environment = ServerEnvironment.DEVELOPMENT, id = "demo-local", labelColor = Color.rgb(66, 165, 245),
    ),
    Server(
        name = "Основной сервер базы данных и резервного копирования",
        host = "db.example.internal", port = 5432,
        environment = ServerEnvironment.PRODUCTION, id = "demo-database", labelColor = Color.rgb(239, 83, 80),
    ),
    Server(
        name = "Тестовый API", host = "staging.example.com", port = 443,
        environment = ServerEnvironment.STAGING, id = "demo-staging",
        labelColor = Color.rgb(255, 193, 7), isMonitoringEnabled = false,
    ),
)
