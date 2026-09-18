package com.example.noname.data.storage

import android.content.Context
import com.example.noname.data.model.TodoItem
import com.example.noname.data.model.json
import com.example.noname.data.model.parse
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class FileStorage(
    context: Context,
) {

    private val file = File(
        context.applicationContext.filesDir,
        FILE_NAME,
    )

    private val mutableItems = mutableListOf<TodoItem>()

    val items: List<TodoItem>
        get() = mutableItems.toList()

    fun add(item: TodoItem) {
        val existingIndex = mutableItems.indexOfFirst {
            it.uid == item.uid
        }

        if (existingIndex >= 0) {
            mutableItems[existingIndex] = item
        } else {
            mutableItems.add(item)
        }
    }

    fun remove(uid: String) {
        mutableItems.removeAll { item ->
            item.uid == uid
        }
    }

    fun save() {
        val jsonArray = JSONArray()

        mutableItems.forEach { item ->
            jsonArray.put(item.json)
        }

        file.writeText(jsonArray.toString())
    }

    fun load() {
        if (!file.exists()) {
            return
        }

        val loadedItems = runCatching {
            val jsonArray = JSONArray(file.readText())

            buildList {
                for (index in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.opt(index) as? JSONObject
                        ?: continue

                    val item = TodoItem.parse(jsonObject)
                        ?: continue

                    add(item)
                }
            }
        }.getOrNull() ?: return

        mutableItems.clear()
        mutableItems.addAll(loadedItems)
    }

    private companion object {
        const val FILE_NAME = "todo_items.json"
    }
}