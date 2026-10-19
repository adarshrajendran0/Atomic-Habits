package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.data.AppDatabase
import com.example.data.CompletionLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class HabitWidgetProvider : AppWidgetProvider() {

    private val widgetScope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val ACTION_COMPLETE_HABIT = "com.example.widget.ACTION_COMPLETE_HABIT"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_COMPLETE_HABIT) {
            if (intent.getStringExtra("widget_action") == "open_screen") {
                val launchIntent = Intent(context, com.example.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    intent.getStringExtra("widget_destination")?.let { putExtra("widget_destination", it) }
                }
                context.startActivity(launchIntent)
                return
            }

            val habitId = intent.getIntExtra("habit_id", -1)
            if (habitId != -1) {
                widgetScope.launch {
                    val db = AppDatabase.getDatabase(context)
                    val dao = db.habitDao()
                    val todayStr = LocalDate.now().toString()

                    // Check if already completed today
                    val logs = dao.getLogsByHabitId(habitId).first()
                    val isCompleted = logs.any { it.dateString == todayStr }

                    if (isCompleted) {
                        dao.deleteCompletionLog(habitId, todayStr)
                    } else {
                        dao.insertCompletionLog(CompletionLog(habitId = habitId, dateString = todayStr))
                    }

                    // Simple streak recalculation for widgets
                    val updatedLogs = dao.getLogsByHabitId(habitId).first()
                    val habit = dao.getHabitById(habitId) ?: return@launch

                    val dateSet = updatedLogs.mapNotNull {
                        try { LocalDate.parse(it.dateString) } catch (e: Exception) { null }
                    }.toSet()

                    var streak = 0
                    var checkDate = if (dateSet.contains(LocalDate.now())) LocalDate.now() else LocalDate.now().minusDays(1)
                    while (dateSet.contains(checkDate)) {
                        streak++
                        checkDate = checkDate.minusDays(1)
                    }

                    dao.updateHabit(
                        habit.copy(
                            currentStreak = streak,
                            lastCompletedDate = updatedLogs.map { it.dateString }.sortedDescending().firstOrNull()
                        )
                    )

                    // Refresh widgets
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val thisWidget = ComponentName(context, HabitWidgetProvider::class.java)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                    
                    // Force BOTH the StackView and AdapterViewFlipper to reload datasets
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, com.example.R.id.widget_stack)
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, com.example.R.id.widget_flipper)
                    
                    onUpdate(context, appWidgetManager, appWidgetIds)
                }
            }
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, com.example.R.layout.habit_widget_layout)

        // Set up adapter intent pointing to our stack service
        val intent = Intent(context, HabitWidgetStackService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = android.net.Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(com.example.R.id.widget_stack, intent)
        views.setRemoteAdapter(com.example.R.id.widget_flipper, intent)

        // Set up PendingIntent template for clicks on list items inside StackView and AdapterViewFlipper
        val completeIntentTemplate = Intent(context, HabitWidgetProvider::class.java).apply {
            action = ACTION_COMPLETE_HABIT
        }
        val completePendingIntentTemplate = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            completeIntentTemplate,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setPendingIntentTemplate(com.example.R.id.widget_stack, completePendingIntentTemplate)
        views.setPendingIntentTemplate(com.example.R.id.widget_flipper, completePendingIntentTemplate)

        val prefs = context.getSharedPreferences("atomic_habits_prefs", Context.MODE_PRIVATE)
        val isPaged = prefs.getInt("widget_display_mode", 0) == 1
        views.setViewVisibility(com.example.R.id.widget_stack, if (isPaged) android.view.View.GONE else android.view.View.VISIBLE)
        views.setViewVisibility(com.example.R.id.widget_flipper, if (isPaged) android.view.View.VISIBLE else android.view.View.GONE)

        // Apply visual updates to the home screen
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
