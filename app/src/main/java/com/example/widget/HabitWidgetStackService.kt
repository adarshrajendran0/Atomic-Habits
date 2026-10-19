package com.example.widget

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.AppDatabase
import com.example.data.CompletionLog
import com.example.data.Habit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalTime
import com.example.data.cueTimeList

class HabitWidgetStackService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return HabitWidgetStackFactory(applicationContext, intent)
    }
}

class HabitWidgetStackFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val appWidgetId = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )

    private var habits: List<Habit> = emptyList()
    private var logs: List<CompletionLog> = emptyList()

    override fun onCreate() {
        // No-op
    }

    override fun onDataSetChanged() {
        // Query the local database from the background thread synchronously
        runBlocking {
            try {
                val db = AppDatabase.getDatabase(context)
                val dao = db.habitDao()
                habits = dao.getAllActiveHabits().first()
                logs = dao.getAllCompletionLogs().first()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        habits = emptyList()
        logs = emptyList()
    }

    override fun getCount(): Int {
        return 4 // 4 distinct cards
    }

    private fun findPriorityHabit(habits: List<Habit>, context: Context, prefs: android.content.SharedPreferences): Habit? {
        val currentHour = LocalTime.now().hour
        val targetTimePeriod = when {
            currentHour in 5..11 -> "morning"
            currentHour in 12..16 -> "afternoon"
            else -> "evening"
        }
        val currentSsid = if (prefs.getBoolean("location_suggestions_enabled", false) &&
            androidx.core.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            try { com.example.location.LocationHelper.getCurrentWifiSsid(context) } catch(e: Exception) { null }
        } else null

        return habits.maxByOrNull { habit ->
            var score = if (habit.timeOfDay.equals(targetTimePeriod, ignoreCase = true)) 100 else 0
            
            val cues = habit.cueTimeList()
            if (cues.isNotEmpty()) {
                val minDeltaMinutes = cues.map { Math.abs(java.time.Duration.between(it, LocalTime.now()).toMinutes()) }.minOrNull() ?: 9999
                if (minDeltaMinutes <= 120) {
                    // within 2 hours adds to score
                    score += (200 - minDeltaMinutes.toInt())
                }
            }
            if (habit.linkedWifiSsid != null && habit.linkedWifiSsid == currentSsid) score += 500
            score
        } ?: habits.firstOrNull()
    }

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.habit_widget_card_item)
        val todayStr = LocalDate.now().toString()

        val prefs = context.getSharedPreferences("atomic_habits_prefs", Context.MODE_PRIVATE)
        val useMaterialYou = prefs.getInt("widget_theme", 0) == 1 &&
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S

        val (bgColor, primaryTextColor, secondaryTextColor) = if (useMaterialYou) {
            Triple(
                context.getColor(android.R.color.system_neutral1_900),  // dark tonal surface from wallpaper
                context.getColor(android.R.color.system_neutral1_50),   // light tonal text from wallpaper
                context.getColor(android.R.color.system_accent1_200)    // wallpaper-derived accent for secondary text
            )
        } else {
            Triple(
                android.graphics.Color.parseColor("#121212"),
                android.graphics.Color.parseColor("#FFFFFF"),
                android.graphics.Color.parseColor("#888888")
            )
        }

        try {
            views.setColorStateList(
                R.id.widget_root_card,
                "setBackgroundTintList",
                android.content.res.ColorStateList.valueOf(bgColor)
            )
        } catch (e: Exception) {
            views.setInt(R.id.widget_root_card, "setBackgroundColor", bgColor)
        }

        views.setTextColor(R.id.widget_primary_text, primaryTextColor)
        views.setTextColor(R.id.widget_secondary_text, secondaryTextColor)
        views.setTextColor(R.id.widget_card_title, secondaryTextColor)

        when (position) {
            0 -> {
                // 1. "Right Now": Surface contextually relevant habit (morning/afternoon/evening)
                views.setTextViewText(R.id.widget_card_title, "Right Now cue")
                val currentHour = LocalTime.now().hour
                val targetTimePeriod = when {
                    currentHour in 5..11 -> "morning"
                    currentHour in 12..16 -> "afternoon"
                    else -> "evening"
                }

                val matchingHabit = findPriorityHabit(habits, context, prefs)

                if (matchingHabit != null) {
                    val isDone = logs.any { it.habitId == matchingHabit.id && it.dateString == todayStr }
                    views.setTextViewText(R.id.widget_primary_text, matchingHabit.name)
                    
                    val subtitle = buildString {
                        if (matchingHabit.timeOfDaySpecific.isNotBlank()) append(" At ${matchingHabit.timeOfDaySpecific}")
                        if (matchingHabit.place.isNotBlank()) append(" in ${matchingHabit.place}")
                    }.trim()
                    
                    views.setTextViewText(
                        R.id.widget_secondary_text,
                        if (subtitle.isBlank()) "${targetTimePeriod.replaceFirstChar { it.uppercase() }} routine" else subtitle
                    )

                    // Set up click fill-in intent
                    val fillInIntent = Intent().apply {
                        putExtra("habit_id", matchingHabit.id)
                    }
                    views.setOnClickFillInIntent(R.id.widget_complete_btn, fillInIntent)
                    views.setViewVisibility(R.id.widget_complete_btn, android.view.View.VISIBLE)

                    // Update checkbox graphics
                    views.setImageViewResource(
                        R.id.widget_complete_btn,
                        if (isDone) android.R.drawable.checkbox_on_background else android.R.drawable.checkbox_off_background
                    )
                } else {
                    views.setTextViewText(R.id.widget_primary_text, "Start fresh today")
                    views.setTextViewText(R.id.widget_secondary_text, "No active habits set")
                    views.setViewVisibility(R.id.widget_complete_btn, android.view.View.INVISIBLE)
                }
            }
            1 -> {
                // 2. "Today's Glance"
                views.setTextViewText(R.id.widget_card_title, "Today's Glance")
                if (habits.isNotEmpty()) {
                    val completedTodayCount = habits.count { activeHabit ->
                        logs.any { it.habitId == activeHabit.id && it.dateString == todayStr }
                    }
                    views.setTextViewText(
                        R.id.widget_primary_text,
                        "Completed: $completedTodayCount of ${habits.size} show-ups today"
                    )
                    views.setTextViewText(
                        R.id.widget_secondary_text,
                        "Progress: ${((completedTodayCount.toFloat() / habits.size.toFloat()) * 100).toInt()}% towards identity goals"
                    )
                } else {
                    views.setTextViewText(R.id.widget_primary_text, "Practice starts now")
                    views.setTextViewText(R.id.widget_secondary_text, "Create a habit in notebook")
                }
                views.setViewVisibility(R.id.widget_complete_btn, android.view.View.INVISIBLE)
            }
            2 -> {
                // 3. "At-Risk Streak"
                views.setTextViewText(R.id.widget_card_title, "At-Risk Streak")
                val atRiskHabit = habits
                    .filter { it.currentStreak > 0 }
                    .filter { activeHabit -> !logs.any { it.habitId == activeHabit.id && it.dateString == todayStr } }
                    .maxByOrNull { it.currentStreak }
                    ?: habits.find { activeHabit -> !logs.any { it.habitId == activeHabit.id && it.dateString == todayStr } }

                if (atRiskHabit != null) {
                    views.setTextViewText(R.id.widget_primary_text, "Protect: ${atRiskHabit.name}")
                    views.setTextViewText(
                        R.id.widget_secondary_text,
                        "Unbroken: ${atRiskHabit.currentStreak} day streak at risk! Keep it?"
                    )

                    // Click fill-in intent
                    val fillInIntent = Intent().apply {
                        putExtra("habit_id", atRiskHabit.id)
                    }
                    views.setOnClickFillInIntent(R.id.widget_complete_btn, fillInIntent)
                    views.setViewVisibility(R.id.widget_complete_btn, android.view.View.VISIBLE)
                    views.setImageViewResource(R.id.widget_complete_btn, android.R.drawable.checkbox_off_background)
                } else {
                    views.setTextViewText(R.id.widget_primary_text, "All streaks are safe!")
                    views.setTextViewText(R.id.widget_secondary_text, "Showing up 100% complete")
                    views.setViewVisibility(R.id.widget_complete_btn, android.view.View.INVISIBLE)
                }
            }
            3 -> {
                // 4. "Quiet Motivation"
                views.setTextViewText(R.id.widget_card_title, "Quiet Motivation")
                val priorityHabit = findPriorityHabit(habits, context, prefs)

                val message = if (priorityHabit != null) {
                    val identity = priorityHabit.identityTag.takeIf(String::isNotBlank)
                    val streak = priorityHabit.currentStreak
                    val templates = buildList {
                        add("Showing up for \"${priorityHabit.name}\" is a small vote for who you're becoming.")
                        if (identity != null) add("You don't have to feel like a $identity to act like one — \"${priorityHabit.name}\" is how.")
                        if (streak > 0) add("$streak days of \"${priorityHabit.name}\" so far. That's identity, not luck.")
                        add("\"${priorityHabit.name}\" doesn't need to be perfect today — it just needs to happen.")
                    }
                    templates[LocalDate.now().dayOfYear % templates.size]
                } else {
                    // Empty state — no habits yet, so there's nothing real to ground a message in.
                    // Keep one calm, identity-framed line rather than the old static rotation.
                    "Every habit you start is a vote for the person you're choosing to become."
                }

                views.setTextViewText(R.id.widget_primary_text, message)
                views.setTextViewText(R.id.widget_secondary_text, "For ${priorityHabit?.name ?: "your next habit"}")
                views.setViewVisibility(R.id.widget_complete_btn, android.view.View.INVISIBLE)
            }
        }

        // Card tap opens corresponding screen
        val destination = when (position) {
            0, 1 -> "today"     // Right Now, Today's Glance
            2 -> "streaks"      // At-Risk Streak
            else -> null        // Quiet Motivation
        }
        val openScreenFillInIntent = Intent().apply {
            putExtra("widget_action", "open_screen")
            if (destination != null) {
                putExtra("widget_destination", destination)
            }
        }
        views.setOnClickFillInIntent(R.id.widget_root_card, openScreenFillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }
}
