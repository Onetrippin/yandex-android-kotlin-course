package ru.servermonitor.app.data.model

import android.graphics.Color
import java.time.LocalDateTime
import java.util.UUID

data class Server(
    val name: String,
    val host: String,
    val port: Int,
    val environment: ServerEnvironment,
    val id: String = UUID.randomUUID().toString(),
    val labelColor: Int = Color.WHITE,
    val maintenanceUntil: LocalDateTime? = null,
    val isMonitoringEnabled: Boolean = true,
) {
    init {
        require(name.isNotBlank()) { "Server name must not be blank" }
        require(host.isNotBlank()) { "Server host must not be blank" }
        require(port in MIN_PORT..MAX_PORT) { "Server port is out of range" }
    }

    companion object
}

private const val MIN_PORT = 1
private const val MAX_PORT = 65_535
