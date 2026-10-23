package ru.servermonitor.app.ui.servers

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.drop
import ru.servermonitor.app.R
import ru.servermonitor.app.data.model.Server
import ru.servermonitor.app.data.model.ServerEnvironment
import ru.servermonitor.app.ui.theme.ServerMonitorTheme
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun ServerListScreen(
    servers: List<Server>,
    onOpenServer: (String) -> Unit,
    onCreateServer: () -> Unit,
    onDeleteServer: (String) -> Unit,
) {
    var deleteTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    var openedSwipeId by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    BackHandler(enabled = openedSwipeId != null && deleteTargetId == null) { openedSwipeId = null }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateServer) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_server))
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 800.dp).fillMaxWidth()) {
                Text(
                    stringResource(R.string.servers_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
                if (servers.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_servers))
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp),
                    ) {
                        items(servers, key = Server::id) { server ->
                            ServerSwipeRow(
                                server = server,
                                isOpen = openedSwipeId == server.id,
                                onOpen = { onOpenServer(server.id) },
                                onRequestDelete = { deleteTargetId = server.id },
                                onSwipeOpened = { openedSwipeId = server.id },
                                onSwipeClosed = { if (openedSwipeId == server.id) openedSwipeId = null },
                            )
                        }
                    }
                }
            }
        }
    }
    servers.firstOrNull { it.id == deleteTargetId }?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTargetId = null },
            title = { Text(stringResource(R.string.delete_server_title)) },
            text = { Text(stringResource(R.string.delete_server_confirmation, target.name)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteServer(target.id)
                    deleteTargetId = null
                    openedSwipeId = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetId = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

private enum class SwipeAnchor { Closed, Open }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ServerSwipeRow(
    server: Server,
    isOpen: Boolean,
    onOpen: () -> Unit,
    onRequestDelete: () -> Unit,
    onSwipeOpened: () -> Unit,
    onSwipeClosed: () -> Unit,
) {
    val density = LocalDensity.current
    val actionWidth = 112.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val velocityThresholdPx = with(density) { 125.dp.toPx() }
    val state = remember(server.id, velocityThresholdPx) {
        AnchoredDraggableState(
            initialValue = if (isOpen) SwipeAnchor.Open else SwipeAnchor.Closed,
            positionalThreshold = { distance -> distance * .35f },
            velocityThreshold = { velocityThresholdPx },
            snapAnimationSpec = spring(),
            decayAnimationSpec = exponentialDecay(),
        )
    }
    val currentOnOpened by rememberUpdatedState(onSwipeOpened)
    val currentOnClosed by rememberUpdatedState(onSwipeClosed)
    SideEffect {
        state.updateAnchors(DraggableAnchors {
            SwipeAnchor.Closed at 0f
            SwipeAnchor.Open at -actionWidthPx
        })
    }
    LaunchedEffect(isOpen, state) {
        state.animateTo(if (isOpen) SwipeAnchor.Open else SwipeAnchor.Closed)
    }
    LaunchedEffect(state) {
        snapshotFlow { state.settledValue }.drop(1).collect { anchor ->
            if (anchor == SwipeAnchor.Open) currentOnOpened() else currentOnClosed()
        }
    }
    Box(Modifier.fillMaxWidth().clipToBounds()) {
        if (state.offset.isFinite() && state.offset < 0f) {
            Button(
                onClick = onRequestDelete,
                modifier = Modifier.matchParentSize().background(MaterialTheme.colorScheme.errorContainer),
                contentPadding = PaddingValues(0.dp),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                enabled = isOpen,
            ) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(actionWidth).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth()
                .offset { IntOffset((state.offset.takeIf { it.isFinite() } ?: 0f).roundToInt(), 0) }
                .anchoredDraggable(state, reverseDirection = false, orientation = Orientation.Horizontal)
                .background(if (server.isMonitoringEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)
                .clickable {
                    if (abs(state.offset) < .5f && !state.isAnimationRunning) onOpen() else onSwipeClosed()
                }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(server.name, style = MaterialTheme.typography.titleMedium)
                val host = if (':' in server.host) "[${server.host}]" else server.host
                Text("$host:${server.port}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(when (server.environment) {
                        ServerEnvironment.DEVELOPMENT -> R.string.environment_development
                        ServerEnvironment.STAGING -> R.string.environment_staging
                        ServerEnvironment.PRODUCTION -> R.string.environment_production
                    }),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    stringResource(if (server.isMonitoringEnabled) R.string.monitoring_enabled else R.string.monitoring_disabled),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(Modifier.size(20.dp).clip(CircleShape).background(Color(server.labelColor))
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Preview(showBackground = true, widthDp = 800, heightDp = 360)
@Composable
private fun ServerListPreview() {
    ServerMonitorTheme {
        ServerListScreen(
            servers = listOf(
                Server("API", "localhost", 8080, ServerEnvironment.DEVELOPMENT, id = "short"),
                Server("Резервный сервер базы данных для долгосрочного хранения журналов и резервных копий всех окружений", "backup.local", 5432, ServerEnvironment.PRODUCTION, id = "long", isMonitoringEnabled = false),
            ),
            onOpenServer = {}, onCreateServer = {}, onDeleteServer = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun EmptyServerListPreview() {
    ServerMonitorTheme { ServerListScreen(emptyList(), {}, {}, {}) }
}
