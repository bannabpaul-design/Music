package com.example.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SleepTimerManager(
    private val scope: CoroutineScope,
    private val onTimerFinished: () -> Unit
) {
    private val _remainingSeconds = MutableStateFlow<Int?>(null)
    val remainingSeconds: StateFlow<Int?> = _remainingSeconds.asStateFlow()

    private var timerJob: Job? = null

    fun startTimer(minutes: Int) {
        cancelTimer()
        val totalSeconds = minutes * 60
        _remainingSeconds.value = totalSeconds

        timerJob = scope.launch(Dispatchers.Default) {
            var current = totalSeconds
            while (current > 0) {
                delay(1000L)
                current--
                _remainingSeconds.value = current
            }
            _remainingSeconds.value = null
            onTimerFinished()
        }
    }

    fun addMinutes(minutes: Int) {
        val current = _remainingSeconds.value ?: 0
        val newSeconds = current + (minutes * 60)
        _remainingSeconds.value = newSeconds
        if (timerJob == null || timerJob?.isActive != true) {
            startTimer(newSeconds / 60)
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _remainingSeconds.value = null
    }

    fun isRunning(): Boolean = _remainingSeconds.value != null
}
