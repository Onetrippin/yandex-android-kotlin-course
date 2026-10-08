package ru.servermonitor.app.data.storage

import android.content.Context
import ru.servermonitor.app.data.model.Server
import ru.servermonitor.app.data.model.json
import ru.servermonitor.app.data.model.parse
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ServerFileStorage(
    context: Context,
) {

    private val file = File(
        context.applicationContext.filesDir,
        FILE_NAME,
    )

    private val mutableServers = mutableListOf<Server>()

    val servers: List<Server>
        get() = mutableServers.toList()

    fun add(server: Server) {
        val existingIndex = mutableServers.indexOfFirst { existing ->
            existing.id == server.id
        }

        if (existingIndex >= 0) {
            mutableServers[existingIndex] = server
        } else {
            mutableServers.add(server)
        }
    }

    fun remove(id: String) {
        mutableServers.removeAll { server ->
            server.id == id
        }
    }

    fun save() {
        val jsonArray = JSONArray()

        mutableServers.forEach { server ->
            jsonArray.put(server.json)
        }

        file.writeText(jsonArray.toString())
    }

    fun load() {
        if (!file.exists()) {
            return
        }

        val loadedServers = runCatching {
            val jsonArray = JSONArray(file.readText())

            buildList {
                for (index in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.opt(index) as? JSONObject
                        ?: continue

                    val server = Server.parse(jsonObject)
                        ?: continue

                    add(server)
                }
            }
        }.getOrNull() ?: return

        mutableServers.clear()
        mutableServers.addAll(loadedServers)
    }

    private companion object {
        const val FILE_NAME = "servers.json"
    }
}
