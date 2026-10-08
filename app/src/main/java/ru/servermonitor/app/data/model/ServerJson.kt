package ru.servermonitor.app.data.model

import android.graphics.Color
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

val Server.json: JSONObject
    get() = JSONObject().apply {
        put(KEY_ID, id)
        put(KEY_NAME, name)
        put(KEY_HOST, host)
        put(KEY_PORT, port)
        put(KEY_ENVIRONMENT, environment.name)

        if (labelColor != Color.WHITE) {
            put(KEY_LABEL_COLOR, labelColor)
        }

        maintenanceUntil?.let { dateTime ->
            put(
                KEY_MAINTENANCE_UNTIL,
                dateTime.toInstant(ZoneOffset.UTC).toEpochMilli(),
            )
        }

        if (!isMonitoringEnabled) {
            put(KEY_IS_MONITORING_ENABLED, false)
        }
    }

fun Server.Companion.parse(json: JSONObject): Server? {
    return runCatching {
        val name = (json.opt(KEY_NAME) as? String)
            ?.takeIf(String::isNotBlank)
            ?: return null

        val host = (json.opt(KEY_HOST) as? String)
            ?.takeIf(String::isNotBlank)
            ?: return null

        val portNumber = json.opt(KEY_PORT) as? Number
            ?: return null
        val rawPort = portNumber.toDouble()
        if (!rawPort.isFinite() || rawPort % 1.0 != 0.0) {
            return null
        }
        val port = rawPort.toInt()
        if (port !in MIN_PORT..MAX_PORT) {
            return null
        }

        val rawEnvironment = json.opt(KEY_ENVIRONMENT) as? String
            ?: return null
        val environment = ServerEnvironment.entries.firstOrNull { candidate ->
            candidate.name == rawEnvironment
        } ?: return null

        val id = if (json.has(KEY_ID)) {
            (json.opt(KEY_ID) as? String)
                ?.takeIf(String::isNotBlank)
                ?: return null
        } else {
            UUID.randomUUID().toString()
        }

        val labelColor = if (json.has(KEY_LABEL_COLOR)) {
            (json.opt(KEY_LABEL_COLOR) as? Number)?.toInt()
                ?: return null
        } else {
            Color.WHITE
        }

        val maintenanceUntil = if (
            json.has(KEY_MAINTENANCE_UNTIL) &&
            !json.isNull(KEY_MAINTENANCE_UNTIL)
        ) {
            val timestamp = (json.opt(KEY_MAINTENANCE_UNTIL) as? Number)?.toLong()
                ?: return null

            LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneOffset.UTC,
            )
        } else {
            null
        }

        val isMonitoringEnabled = if (json.has(KEY_IS_MONITORING_ENABLED)) {
            json.opt(KEY_IS_MONITORING_ENABLED) as? Boolean
                ?: return null
        } else {
            true
        }

        Server(
            id = id,
            name = name,
            host = host,
            port = port,
            environment = environment,
            labelColor = labelColor,
            maintenanceUntil = maintenanceUntil,
            isMonitoringEnabled = isMonitoringEnabled,
        )
    }.getOrNull()
}

private const val KEY_ID = "id"
private const val KEY_NAME = "name"
private const val KEY_HOST = "host"
private const val KEY_PORT = "port"
private const val KEY_ENVIRONMENT = "environment"
private const val KEY_LABEL_COLOR = "labelColor"
private const val KEY_MAINTENANCE_UNTIL = "maintenanceUntil"
private const val KEY_IS_MONITORING_ENABLED = "isMonitoringEnabled"
private const val MIN_PORT = 1
private const val MAX_PORT = 65_535
