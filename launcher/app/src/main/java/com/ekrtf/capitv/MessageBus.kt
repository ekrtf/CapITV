package com.ekrtf.capitv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** One transient on-screen message at a time ("Canal+ is not installed", ...). */
class MessageBus {
    var current by mutableStateOf<Message?>(null)
        private set
    private var counter = 0L

    fun show(text: String) {
        counter++
        current = Message(counter, text)
    }

    fun dismiss(id: Long) {
        if (current?.id == id) current = null
    }

    data class Message(val id: Long, val text: String)
}
