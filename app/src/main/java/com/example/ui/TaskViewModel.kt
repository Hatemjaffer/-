package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Task
import com.example.data.TaskRepository
import com.example.util.AlarmEventBus
import com.example.util.AlarmScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository
    val allTasks: StateFlow<List<Task>>

    // Filters and Search Local State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(FilterType.ALL) // ALL, TASK, APPOINTMENT
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _selectedPriorityFilter = MutableStateFlow("ALL") // ALL, HIGH, MEDIUM, LOW
    val selectedPriorityFilter = _selectedPriorityFilter.asStateFlow()

    // Foreground Alert state
    private val _activeAlert = MutableStateFlow<Task?>(null)
    val activeAlert = _activeAlert.asStateFlow()

    init {
        val taskDao = AppDatabase.getDatabase(application).taskDao()
        repository = TaskRepository(taskDao)
        allTasks = repository.allTasks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Bind Foreground Alarms from Event Bus
        viewModelScope.launch {
            AlarmEventBus.triggeredAlarm.collect { task ->
                _activeAlert.value = task
            }
        }
    }

    fun searchTasks(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filterType: FilterType) {
        _selectedFilter.value = filterType
    }

    fun setPriorityFilter(priority: String) {
        _selectedPriorityFilter.value = priority
    }

    fun dismissActiveAlert() {
        _activeAlert.value = null
    }

    fun addTask(
        title: String,
        description: String,
        isAppointment: Boolean,
        dueTimestamp: Long,
        priority: String,
        alertScheduled: Boolean,
        reminderTimestamp: Long
    ) {
        viewModelScope.launch {
            val newTask = Task(
                title = title,
                description = description,
                isAppointment = isAppointment,
                dueTimestamp = dueTimestamp,
                priority = priority,
                isCompleted = false,
                alertScheduled = alertScheduled,
                reminderTimestamp = reminderTimestamp
            )
            val newId = repository.insert(newTask)
            
            if (alertScheduled) {
                // To configure alarms accurately, we need the database assigned incremental primary ID
                val taskWithId = newTask.copy(id = newId.toInt())
                AlarmScheduler.scheduleAlarm(getApplication(), taskWithId)
            }
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            repository.update(updatedTask)
            
            // If completed, cancel any outstanding alarm
            if (updatedTask.isCompleted && updatedTask.alertScheduled) {
                AlarmScheduler.cancelAlarm(getApplication(), updatedTask)
            } else if (!updatedTask.isCompleted && updatedTask.alertScheduled) {
                // If uncompleted and has alarm, reschedule it
                AlarmScheduler.scheduleAlarm(getApplication(), updatedTask)
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            if (task.alertScheduled) {
                AlarmScheduler.cancelAlarm(getApplication(), task)
            }
            repository.delete(task)
        }
    }

    fun editTask(task: Task) {
        viewModelScope.launch {
            // Cancel previous alarm if existed
            if (task.alertScheduled) {
                AlarmScheduler.cancelAlarm(getApplication(), task)
            }
            
            repository.update(task)
            
            // Schedule new alarm if active and not completed
            if (task.alertScheduled && !task.isCompleted) {
                AlarmScheduler.scheduleAlarm(getApplication(), task)
            }
        }
    }
}

enum class FilterType {
    ALL, TASK, APPOINTMENT
}
