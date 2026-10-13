package ru.servermonitor.app.data.storage

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory
import ru.servermonitor.app.data.model.Server
import ru.servermonitor.app.data.model.json
import ru.servermonitor.app.data.model.parse
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset

class ServerFileStorage(
    private val file: File,
) {

    private val log = LoggerFactory.getLogger(ServerFileStorage::class.java)

    private val mutableServers = mutableListOf<Server>()

    val servers: List<Server>
        get() {
            purgeDecommissionedServers()
            return mutableServers.toList()
        }

    fun add(server: Server) {
        purgeDecommissionedServers()
        if (server.isDecommissionedAt(LocalDateTime.now(ZoneOffset.UTC))) {
            val removedExistingServer = mutableServers.removeAll { existing ->
                existing.id == server.id
            }
            if (removedExistingServer) {
                log.info("add: removed decommissioned server id={}", server.id)
                writeCurrentServers()
            } else {
                log.info("add: ignored decommissioned server id={}", server.id)
            }
            return
        }

        log.debug("add: start, serverId={}", server.id)
        val existingIndex = mutableServers.indexOfFirst { existing ->
            existing.id == server.id
        }

        if (existingIndex >= 0) {
            mutableServers[existingIndex] = server
            log.info("add: updated server id={}", server.id)
        } else {
            mutableServers.add(server)
            log.info("add: added server id={}", server.id)
        }
    }

    fun remove(id: String) {
        purgeDecommissionedServers()
        val removed = mutableServers.removeAll { server ->
            server.id == id
        }

        if (removed) {
            log.info("remove: deleted server id={}", id)
        } else {
            log.warn("remove: server id={} does not exist", id)
        }
    }

    fun save() {
        purgeDecommissionedServers(persistChanges = false)
        log.info("save: start, {} servers", mutableServers.size)
        writeCurrentServers()
    }

    private fun writeCurrentServers() {
        try {
            val jsonArray = JSONArray()
            mutableServers.forEach { server ->
                jsonArray.put(server.json)
            }

            file.writeText(jsonArray.toString())
            log.info("save: done, {} bytes", file.length())
        } catch (exception: IOException) {
            log.error("save: failed", exception)
        } catch (exception: JSONException) {
            log.error("save: failed to create JSON", exception)
        }
    }

    fun load() {
        log.info("load: start")
        purgeDecommissionedServers(persistChanges = false)
        if (!file.exists()) {
            log.warn("load: file does not exist, keeping current list")
            return
        }

        try {
            val jsonArray = JSONArray(file.readText())
            val loadedServers = mutableListOf<Server>()
            val now = LocalDateTime.now(ZoneOffset.UTC)
            var skippedCount = 0
            var decommissionedCount = 0

            for (index in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.opt(index) as? JSONObject
                if (jsonObject == null) {
                    skippedCount++
                    continue
                }

                val server = Server.parse(jsonObject)
                if (server == null) {
                    skippedCount++
                    continue
                }

                if (server.isDecommissionedAt(now)) {
                    decommissionedCount++
                    continue
                }

                loadedServers.add(server)
            }

            mutableServers.clear()
            mutableServers.addAll(loadedServers)
            log.debug("load: {} servers restored", loadedServers.size)
            if (skippedCount > 0) {
                log.warn("load: skipped {} invalid entries", skippedCount)
            }
            if (decommissionedCount > 0) {
                log.info("load: removed {} decommissioned servers", decommissionedCount)
                writeCurrentServers()
            }
        } catch (exception: IOException) {
            log.error("load: failed to read file", exception)
        } catch (exception: JSONException) {
            log.error("load: failed to parse JSON", exception)
        }
    }

    private fun purgeDecommissionedServers(persistChanges: Boolean = true) {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val activeServers = mutableServers.filterNot { server -> server.isDecommissionedAt(now) }
        val removedCount = mutableServers.size - activeServers.size
        if (removedCount == 0) return

        mutableServers.clear()
        mutableServers.addAll(activeServers)
        log.info("removed {} decommissioned servers", removedCount)
        if (persistChanges) {
            writeCurrentServers()
        }
    }

    companion object {
        const val FILE_NAME = "servers.json"
    }
}

private fun Server.isDecommissionedAt(now: LocalDateTime): Boolean =
    decommissionAt?.let { deadline -> !deadline.isAfter(now) } ?: false
