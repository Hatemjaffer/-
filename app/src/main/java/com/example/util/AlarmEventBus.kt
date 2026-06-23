package com.example.util

import com.example.data.Task
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AlarmEventBus {
    private val _triggeredAlarm = MutableSharedFlow<Task>(extraBufferCapacity = 10)
    val triggeredAlarm = _triggeredAlarm.asSharedFlow()

    fun trigger(task: Task) {
        _triggeredAlarm.tryEmit(task)
    }
}
