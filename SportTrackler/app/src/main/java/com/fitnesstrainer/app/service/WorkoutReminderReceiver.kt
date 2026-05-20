package com.fitnesstrainer.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.ui.MainActivity

class WorkoutReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Show notification
        val channel = NotificationChannel(
            CHANNEL_ID, "Напоминания о тренировке",
            NotificationManager.IMPORTANCE_HIGH
        )
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)

        val tapIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_steps)
            .setContentTitle("Время тренироваться! 💪")
            .setContentText("Не забудь про тренировку сегодня")
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(NOTIFICATION_ID, notification)

        // Reschedule for next occurrence
        val prefs  = context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)
        val hour   = prefs.getInt("reminder_hour", 8)
        val minute = prefs.getInt("reminder_minute", 0)
        val days   = prefs.getString("reminder_days", "2,3,4,5,6,7,1")
            ?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.toSet()
            ?: setOf(2,3,4,5,6,7,1)
        scheduleForDays(context, hour, minute, days)
    }

    companion object {
        const val CHANNEL_ID      = "workout_reminder_channel"
        const val NOTIFICATION_ID = 202

        // Legacy single-day schedule (kept for compatibility)
        fun schedule(context: Context, hour: Int, minute: Int) {
            scheduleForDays(context, hour, minute, setOf(2,3,4,5,6,7,1))
        }

        fun scheduleForDays(context: Context, hour: Int, minute: Int, days: Set<Int>) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            // Cancel all existing alarms first
            cancelAll(context)
            // Schedule one alarm per selected day (requestCode = dayOfWeek)
            days.forEach { dayOfWeek ->
                val intent = PendingIntent.getBroadcast(
                    context, dayOfWeek,
                    Intent(context, WorkoutReminderReceiver::class.java)
                        .putExtra("dayOfWeek", dayOfWeek),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                val cal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.DAY_OF_WEEK, dayOfWeek)
                    set(java.util.Calendar.HOUR_OF_DAY, hour)
                    set(java.util.Calendar.MINUTE, minute)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(java.util.Calendar.WEEK_OF_YEAR, 1)
                    }
                }
                when {
                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                            && am.canScheduleExactAlarms() ->
                        am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, cal.timeInMillis, intent)
                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M ->
                        am.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, cal.timeInMillis, intent)
                    else ->
                        am.set(android.app.AlarmManager.RTC_WAKEUP, cal.timeInMillis, intent)
                }
            }
        }

        fun cancel(context: Context) = cancelAll(context)

        private fun cancelAll(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            // Cancel alarms for all 7 days (requestCode 1..7)
            for (day in 1..7) {
                val intent = PendingIntent.getBroadcast(
                    context, day,
                    Intent(context, WorkoutReminderReceiver::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
                ) ?: continue
                am.cancel(intent)
            }
        }
    }
}
