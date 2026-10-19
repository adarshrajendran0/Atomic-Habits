package com.example.ui

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import com.example.R
import com.example.widget.HabitWidgetProvider

class HabitViewModel(
    application: Application,
    private val repository: HabitRepository,
    private val settingsManager: SettingsManager
) : AndroidViewModel(application) {

    val allActiveHabits: StateFlow<List<Habit>> = repository.allActiveHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHabits: StateFlow<List<Habit>> = repository.allHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCompletionLogs: StateFlow<List<CompletionLog>> = repository.allCompletionLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReflections: StateFlow<List<Reflection>> = repository.allReflections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings flows
    val themeMode: StateFlow<Int> = settingsManager.themeMode
    val hapticsEnabled: StateFlow<Boolean> = settingsManager.hapticsEnabled
    val hapticIntensity: StateFlow<String> = settingsManager.hapticIntensity
    val coachEnabled: StateFlow<Boolean> = settingsManager.coachEnabled
    val coachIntensity: StateFlow<String> = settingsManager.coachIntensity
    val reduceMotion: StateFlow<Boolean> = settingsManager.reduceMotion
    val userIdentity: StateFlow<String> = settingsManager.userIdentity
    val isOnboarded: StateFlow<Boolean> = settingsManager.isOnboarded
    val widgetDisplayMode: StateFlow<Int> = settingsManager.widgetDisplayMode
    val widgetTheme: StateFlow<Int> = settingsManager.widgetTheme
    val locationSuggestionsEnabled: StateFlow<Boolean> = settingsManager.locationSuggestionsEnabled

    fun setLocationSuggestionsEnabled(enabled: Boolean) {
        settingsManager.setLocationSuggestionsEnabled(enabled)
    }

    private val _currentWifiSsid = MutableStateFlow<String?>(null)
    val currentWifiSsid: StateFlow<String?> = _currentWifiSsid

    // Temp screen transitions
    private val _coachingMessage = MutableStateFlow<String?>(null)
    val coachingMessage: StateFlow<String?> = _coachingMessage

    init {
        // Trigger generic onboarding suggestions check or check-ups
        checkCoachingGuidelines()
        
        viewModelScope.launch {
            settingsManager.locationSuggestionsEnabled.collect { enabled ->
                if (enabled) {
                    com.example.location.LocationHelper.observeWifiNetwork(getApplication()).collect { ssid ->
                        _currentWifiSsid.value = ssid
                    }
                } else {
                    _currentWifiSsid.value = null
                }
            }
        }
    }

    fun completeOnboarding(identity: String, starterHabit: Habit?) {
        viewModelScope.launch {
            settingsManager.setUserIdentity(identity)
            settingsManager.setIsOnboarded(true)
            if (starterHabit != null) {
                repository.insertHabit(starterHabit)
            }
        }
    }

    fun addHabit(
        name: String,
        identityTag: String,
        timeOfDay: String,
        place: String,
        timeOfDaySpecific: String,
        starterVersion: String,
        stackAnchorRef: String,
        notes: String,
        cheatDays: String = "",
        linkedWifiSsid: String? = null,
        targetQuantity: Int? = null,
        targetUnit: String = "",
        goalDurationDays: Int? = null,
        cueTimes: String = ""
    ) {
        viewModelScope.launch {
            repository.insertHabit(
                Habit(
                    name = name,
                    identityTag = identityTag,
                    timeOfDay = timeOfDay,
                    place = place,
                    timeOfDaySpecific = timeOfDaySpecific,
                    starterVersion = starterVersion,
                    stackAnchorRef = stackAnchorRef,
                    notes = notes,
                    cheatDays = cheatDays,
                    linkedWifiSsid = linkedWifiSsid,
                    targetQuantity = targetQuantity,
                    targetUnit = targetUnit,
                    goalDurationDays = goalDurationDays,
                    cueTimes = cueTimes
                )
            )
            checkCoachingGuidelines()
            refreshWidgets(getApplication())
        }
    }

    fun updateHabit(habit: Habit) {
        viewModelScope.launch {
            repository.updateHabit(habit)
            checkCoachingGuidelines()
            refreshWidgets(getApplication())
        }
    }

    fun logQuantity(habitId: Int, dateString: String, quantity: Int, context: Context) {
        viewModelScope.launch {
            repository.logCompletionWithQuantity(habitId, dateString, quantity)
            refreshWidgets(context)
            
            // Recompute/fetch habit for milestone recognition
            val habit = repository.getHabitById(habitId)
            if (habit != null) {
                // If it's a goal habit and the goal was reached today, special pulse
                val isMilestone = if (habit.goalDurationDays != null) {
                    habit.currentStreak == habit.goalDurationDays
                } else {
                    habit.currentStreak in listOf(7, 30, 100)
                }
                triggerHaptics(context, isMilestone)
            } else {
                triggerHaptics(context, false)
            }
            checkCoachingGuidelines()
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
            checkCoachingGuidelines()
            refreshWidgets(getApplication())
        }
    }

    fun toggleHabitArchive(habit: Habit) {
        viewModelScope.launch {
            repository.updateHabit(habit.copy(isArchived = !habit.isArchived))
            refreshWidgets(getApplication())
        }
    }

    fun toggleCompletion(habitId: Int, dateString: String, context: Context) {
        viewModelScope.launch {
            // Check logs before toggle to know if checking or unchecking
            val isUnchecking = allCompletionLogs.value.any { it.habitId == habitId && it.dateString == dateString }
            repository.toggleHabitCompletion(habitId, dateString)
            refreshWidgets(getApplication())

            if (!isUnchecking) {
                // Recompute/fetch habit for milestone recognition
                val habit = repository.getHabitById(habitId)
                if (habit != null) {
                    val isMilestone = habit.currentStreak in listOf(7, 30, 100)
                    triggerHaptics(context, isMilestone)
                } else {
                    triggerHaptics(context, false)
                }
            } else {
                triggerHaptics(context, false)
            }
            checkCoachingGuidelines()
        }
    }

    fun saveReflection(easyHabits: String, forcedHabits: String, notes: String) {
        viewModelScope.launch {
            repository.insertReflection(
                Reflection(
                    dateString = LocalDate.now().toString(),
                    easyHabits = easyHabits,
                    forcedHabits = forcedHabits,
                    notes = notes
                )
            )
        }
    }

    fun setOnboarded(onboarded: Boolean) {
        settingsManager.setIsOnboarded(onboarded)
    }

    fun setThemeMode(mode: Int) {
        settingsManager.setThemeMode(mode)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        settingsManager.setHapticsEnabled(enabled)
    }

    fun setHapticIntensity(intensity: String, context: Context) {
        settingsManager.setHapticIntensity(intensity)
        previewHaptic(context, intensity)
    }

    private fun previewHaptic(context: Context, intensity: String) {
        if (!hapticsEnabled.value) return
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (!vibrator.hasVibrator()) return
        val duration = when (intensity) { "Low" -> 15L; "High" -> 60L; else -> 30L }
        val amplitude = when (intensity) { "Low" -> 60; "High" -> 255; else -> 128 }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
        } else {
            @Suppress("DEPRECATION") vibrator.vibrate(duration)
        }
    }

    fun triggerTapHaptic(context: Context) {
        if (!hapticsEnabled.value) return
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (!vibrator.hasVibrator()) return
        val duration = 10L
        val amplitude = when (hapticIntensity.value) { "Low" -> 35; "High" -> 160; else -> 90 }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
        } else {
            @Suppress("DEPRECATION") vibrator.vibrate(duration)
        }
    }

    fun setCoachEnabled(enabled: Boolean) {
        settingsManager.setCoachEnabled(enabled)
        if (!enabled) _coachingMessage.value = null else checkCoachingGuidelines()
    }

    fun setCoachIntensity(intensity: String) {
        settingsManager.setCoachIntensity(intensity)
        checkCoachingGuidelines()
    }

    fun setReduceMotion(reduce: Boolean) {
        settingsManager.setReduceMotion(reduce)
    }

    fun setWidgetDisplayMode(mode: Int, context: Context) {
        settingsManager.setWidgetDisplayMode(mode)
        refreshWidgets(context)
    }

    fun setWidgetTheme(theme: Int, context: Context) {
        settingsManager.setWidgetTheme(theme)
        refreshWidgets(context)
    }

    private fun refreshWidgets(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, HabitWidgetProvider::class.java))
        mgr.notifyAppWidgetViewDataChanged(ids, R.id.widget_stack)
        mgr.notifyAppWidgetViewDataChanged(ids, R.id.widget_flipper)
        context.sendBroadcast(Intent(context, HabitWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        })
    }

    fun dismissCoachingMessage() {
        _coachingMessage.value = null
    }

    fun checkCoachingGuidelines() {
        if (!coachEnabled.value) {
            _coachingMessage.value = null
            return
        }

        viewModelScope.launch {
            val habits = allActiveHabits.value
            if (habits.isEmpty()) {
                _coachingMessage.value = "Tip: Make your first cue obvious by adding an anchor behavior (e.g. \"After I brew coffee, I will write\")."
                return@launch
            }

            // Coach intensity: Light Touch vs More Guidance
            val isMoreGuidance = coachIntensity.value == "More Guidance"

            // 1. "Never Miss Twice" - check if yesterday was missed on a habit that has streak > 0 but missed today
            val todayStr = LocalDate.now().toString()
            val yesterdayStr = LocalDate.now().minusDays(1).toString()
            
            for (habit in habits) {
                val logs = repository.getLogsForHabit(habit.id).stateIn(viewModelScope).value
                val completedToday = logs.any { it.dateString == todayStr }
                val completedYesterday = logs.any { it.dateString == yesterdayStr }

                if (!completedToday && !completedYesterday && logs.isNotEmpty()) {
                    _coachingMessage.value = "Never miss twice: Missing one day is an accident. Missing twice is the start of a new habit. Schedule a simple, 2-minute version for \"${habit.name}\" now."
                    return@launch
                }
            }

            // 2. Effortless completion -> Level up (Goldilocks Rule)
            for (habit in habits) {
                // If current streak is high (e.g. 10 or more), suggest leveling up
                if (habit.currentStreak >= 10) {
                    _coachingMessage.value = "The Goldilocks Rule: \"${habit.name}\" has a streak of ${habit.currentStreak} days! It's becoming automatic. Consider leveling it up slightly to keep it engaging."
                    return@launch
                }
            }

            // 3. Early struggle -> Scale down
            for (habit in habits) {
                val logs = repository.getLogsForHabit(habit.id).stateIn(viewModelScope).value
                // If created more than 5 days ago, and less than 2 completions, suggest 2-minute rule
                val ageDays = java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.Instant.ofEpochMilli(habit.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                    LocalDate.now()
                )
                if (ageDays >= 5 && logs.size < 2) {
                    _coachingMessage.value = "Struggling to stick with \"${habit.name}\"? Scale it down to \"${habit.starterVersion.ifBlank { "a 2-minute starter version" }}\". It's easier to start when friction is near-zero."
                    return@launch
                }
            }

            // Fallback general atomic insight
            if (isMoreGuidance) {
                _coachingMessage.value = "Environment design tip: Put your habits in your path. Make sure your home screen widget is placed prominently!"
            } else {
                _coachingMessage.value = null
            }
        }
    }

    private fun triggerHaptics(context: Context, isMilestone: Boolean) {
        if (!hapticsEnabled.value) return
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (!vibrator.hasVibrator()) return

        val intensity = hapticIntensity.value
        val baseDuration = when (intensity) {
            "Low" -> 15L
            "High" -> 60L
            else -> 30L // Medium
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val amplitude = when (intensity) {
                "Low" -> 60
                "High" -> 255
                else -> 128
            }
            if (isMilestone) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, baseDuration, 100, baseDuration * 2),
                        intArrayOf(0, amplitude, 0, amplitude),
                        -1
                    )
                )
            } else {
                vibrator.vibrate(VibrationEffect.createOneShot(baseDuration, amplitude))
            }
        } else {
            @Suppress("DEPRECATION")
            if (isMilestone) {
                vibrator.vibrate(longArrayOf(0, baseDuration, 100, baseDuration * 2), -1)
            } else {
                vibrator.vibrate(baseDuration)
            }
        }
    }
}

class HabitViewModelFactory(
    private val application: android.app.Application,
    private val repository: HabitRepository,
    private val settingsManager: SettingsManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HabitViewModel(application, repository, settingsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
