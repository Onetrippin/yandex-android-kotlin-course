package com.example.noname.data.model

import android.graphics.Color
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

val TodoItem.json: JSONObject
    get() = JSONObject().apply {
        put(KEY_UID, uid)
        put(KEY_TEXT, text)

        if (importance != Importance.BASIC) {
            put(KEY_IMPORTANCE, importance.name)
        }

        if (color != Color.WHITE) {
            put(KEY_COLOR, color)
        }

        deadline?.let { deadline ->
            put(
                KEY_DEADLINE,
                deadline.toInstant(ZoneOffset.UTC).toEpochMilli(),
            )
        }

        put(KEY_IS_DONE, isDone)
    }

fun TodoItem.Companion.parse(json: JSONObject): TodoItem? {
    return runCatching {
        val text = json.opt(KEY_TEXT) as? String
            ?: return null

        val uid = if (json.has(KEY_UID)) {
            json.opt(KEY_UID) as? String
                ?: return null
        } else {
            UUID.randomUUID().toString()
        }

        val importance = if (json.has(KEY_IMPORTANCE)) {
            val rawImportance = json.opt(KEY_IMPORTANCE) as? String
                ?: return null

            Importance.entries.firstOrNull { importance ->
                importance.name == rawImportance
            } ?: return null
        } else {
            Importance.BASIC
        }

        val color = if (json.has(KEY_COLOR)) {
            (json.opt(KEY_COLOR) as? Number)?.toInt()
                ?: return null
        } else {
            Color.WHITE
        }

        val deadline = if (
            json.has(KEY_DEADLINE) &&
            !json.isNull(KEY_DEADLINE)
        ) {
            val timestamp = (json.opt(KEY_DEADLINE) as? Number)?.toLong()
                ?: return null

            LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneOffset.UTC,
            )
        } else {
            null
        }

        val isDone = if (json.has(KEY_IS_DONE)) {
            json.opt(KEY_IS_DONE) as? Boolean
                ?: return null
        } else {
            false
        }

        TodoItem(
            uid = uid,
            text = text,
            importance = importance,
            color = color,
            deadline = deadline,
            isDone = isDone,
        )
    }.getOrNull()
}

private const val KEY_UID = "uid"
private const val KEY_TEXT = "text"
private const val KEY_IMPORTANCE = "importance"
private const val KEY_COLOR = "color"
private const val KEY_DEADLINE = "deadline"
private const val KEY_IS_DONE = "isDone"