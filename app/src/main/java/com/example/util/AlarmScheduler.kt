package com.example.util

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.Task
import com.example.receiver.AlarmReceiver

object AlarmScheduler {

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleAlarm(context: Context, task: Task) {
        if (task.reminderTimestamp <= System.currentTimeMillis()) {
            Log.d("AlarmScheduler", "Reminder timestamp is in the past; skipping.")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("TASK_ID", task.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        task.reminderTimestamp,
                        pendingIntent
                    )
                    Log.d("AlarmScheduler", "Scheduled exact alarm for task ${task.id} at ${task.reminderTimestamp}")
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        task.reminderTimestamp,
                        pendingIntent
                    )
                    Log.d("AlarmScheduler", "Scheduled standard alarm (exact disallowed) for task ${task.id}")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    task.reminderTimestamp,
                    pendingIntent
                )
                Log.d("AlarmScheduler", "Scheduled exact alarm (sdk < 31) for task ${task.id}")
            }
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "SecurityException scheduling exact alarm, falling back", e)
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                task.reminderTimestamp,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(context: Context, task: Task) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("TASK_ID", task.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("AlarmScheduler", "Canceled alarm for task ${task.id}")
        }
    }
}
