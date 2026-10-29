package com.jarvis.assistant

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class JarvisState { IDLE, LISTENING, THINKING, SPEAKING }

data class Turn(val from: String, val text: String, val ts: Long = System.currentTimeMillis())

object JarvisBus {
    private val _turns = MutableStateFlow<List<Turn>>(emptyList())
    val turns: StateFlow<List<Turn>> = _turns
    private val _state = MutableStateFlow(JarvisState.IDLE)
    val state: StateFlow<JarvisState> = _state

    fun add(from: String, text: String) {
        if (text.isBlank()) return
        _turns.value = (_turns.value + Turn(from, text)).takeLast(200)
    }
    fun setState(s: JarvisState) { _state.value = s }
    fun clear() { _turns.value = emptyList() }
}
