package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AppDatabase
import com.example.util.AlarmEventBus
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("TASK_ID", -1)
        Log.d("AlarmReceiver", "Alarm received for Task ID: $taskId")
        if (taskId == -1) return

        val coroutineScope = CoroutineScope(Dispatchers.IO)
        coroutineScope.launch {
            val db = AppDatabase.getDatabase(context)
            val task = db.taskDao().getTaskById(taskId)
            if (task != null) {
                // Post Notification
                val label = if (task.isAppointment) "موعد تنبيه نشط:" else "تنبيه مهمة نشط:"
                NotificationHelper.showNotification(
                    context = context,
                    id = task.id,
                    title = "$label ${task.title}",
                    content = task.description,
                    isAppointment = task.isAppointment
                )

                // Trigger App Foreground Banner if running
                launch(Dispatchers.Main) {
                    AlarmEventBus.trigger(task)
                }
            }
        }
    }
}
