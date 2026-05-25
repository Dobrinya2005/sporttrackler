package com.fitnesstrainer.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.data.local.db.StepEntity
import com.fitnesstrainer.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

class StepCounterService : Service(), SensorEventListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null

    private var stepsAtBoot = -1
    private var todaySteps  = 0
    private val today get() = LocalDate.now().toString()

    companion object {
        const val CHANNEL_ID     = "step_counter_channel"
        const val NOTIFICATION_ID = 101
        const val ACTION_GET_STEPS = "com.fitnesstrainer.app.GET_STEPS"
        const val EXTRA_STEPS = "steps"

        fun start(context: Context) {
            val intent = Intent(context, StepCounterService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, StepCounterService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification(0))
        try {
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            stepSensor    = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            if (stepSensor == null) {
                stopSelf()
                return
            }
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        } catch (e: Exception) {
            stopSelf()
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        val total = event.values[0].toInt()
        if (stepsAtBoot == -1) stepsAtBoot = total
        todaySteps = total - stepsAtBoot

        updateNotification(todaySteps)
        saveSteps(todaySteps)
        com.fitnesstrainer.app.widget.SportTracklerWidget.updateAll(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun saveSteps(steps: Int) {
        scope.launch {
            val uid = App.instance.tokenStorage.getUserId()
            if (uid == -1) return@launch
            App.instance.database.stepDao().insert(StepEntity(uid, today, steps))
        }
    }

    private fun updateNotification(steps: Int) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(steps))
    }

    private fun buildNotification(steps: Int): Notification {
        val intent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_steps)
            .setContentTitle("SportTrackler")
            .setContentText("Шагов сегодня: $steps")
            .setContentIntent(intent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Шагомер",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Счётчик шагов в фоне" }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}
