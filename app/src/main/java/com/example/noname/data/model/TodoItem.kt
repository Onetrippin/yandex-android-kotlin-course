package com.example.noname.data.model

import android.graphics.Color
import java.time.LocalDateTime
import java.util.UUID

data class TodoItem(
    val text: String,
    val importance: Importance,
    val uid: String = UUID.randomUUID().toString(),
    val color: Int = Color.WHITE,
    val deadline: LocalDateTime? = null,
    val isDone: Boolean = false,
) {
    companion object
}
