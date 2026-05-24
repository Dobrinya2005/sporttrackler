package com.fitnesstrainer.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.data.local.db.StepEntity
import com.fitnesstrainer.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class SportTracklerWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, manager, it) }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, SportTracklerWidget::class.java)
            )
            val provider = SportTracklerWidget()
            provider.onUpdate(context, manager, ids)
        }

        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_sporttrackler)

            val intent = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, intent)

            CoroutineScope(Dispatchers.IO).launch {
                val tokenStorage = App.instance.tokenStorage
                val db           = App.instance.database
                val userId       = tokenStorage.getUserId()
                val today        = LocalDate.now().toString()
                val firstName    = tokenStorage.getFirstName() ?: ""

                val steps   = db.stepDao().get(userId, today)?.steps ?: 0
                val summary = db.dailySummaryDao().get(userId, today)

                val calorieGoal = summary?.calorieGoal ?: run {
                    val weight = db.measurementDao().getAll(userId).firstOrNull()?.weightKg
                    weight?.let { ((it * 33) / 50).toInt() * 50 }
                }
                val calories = summary?.let { "%.0f / %d ккал".format(it.totalCalories, calorieGoal ?: 0) }
                    ?: "— ккал"

                views.setTextViewText(R.id.widget_greeting, "Привет, $firstName!")
                views.setTextViewText(R.id.widget_calories, calories)
                views.setTextViewText(R.id.widget_steps, "$steps шагов")

                manager.updateAppWidget(id, views)
            }
        }
    }
}
