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
        val channel = NotificationChannel(
            CHANNEL_ID, "Напоминания о тренировке",
            NotificationManager.IMPORTANCE_DEFAULT
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
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID      = "workout_reminder_channel"
        const val NOTIFICATION_ID = 202

        fun schedule(context: Context, hour: Int, minute: Int) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = PendingIntent.getBroadcast(
                context, 0,
                Intent(context, WorkoutReminderReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(java.util.Calendar.DAY_OF_YEAR, 1)
                }
            }
            am.setRepeating(
                android.app.AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                android.app.AlarmManager.INTERVAL_DAY,
                intent
            )
        }

        fun cancel(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = PendingIntent.getBroadcast(
                context, 0,
                Intent(context, WorkoutReminderReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            am.cancel(intent)
        }
    }
}
