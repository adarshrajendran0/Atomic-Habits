package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("atomic_habits_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getInt("theme_mode", 0)) // 0: system, 1: light, 2: dark
    val themeMode: StateFlow<Int> = _themeMode

    // 0 = Stacked (current StackView swipe-through-stack), 1 = Paged (one card at a time)
    private val _widgetDisplayMode = MutableStateFlow(prefs.getInt("widget_display_mode", 0))
    val widgetDisplayMode: StateFlow<Int> = _widgetDisplayMode

    // 0 = Monochrome (app's own black/white/gray language), 1 = Material You (dynamic system color)
    private val _widgetTheme = MutableStateFlow(prefs.getInt("widget_theme", 0))
    val widgetTheme: StateFlow<Int> = _widgetTheme

    private val _locationSuggestionsEnabled = MutableStateFlow(prefs.getBoolean("location_suggestions_enabled", false))
    val locationSuggestionsEnabled: StateFlow<Boolean> = _locationSuggestionsEnabled

    private val _hapticsEnabled = MutableStateFlow(prefs.getBoolean("haptics_enabled", true))
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled

    private val _hapticIntensity = MutableStateFlow(prefs.getString("haptic_intensity", "Medium") ?: "Medium")
    val hapticIntensity: StateFlow<String> = _hapticIntensity

    private val _coachEnabled = MutableStateFlow(prefs.getBoolean("coach_enabled", false))
    val coachEnabled: StateFlow<Boolean> = _coachEnabled

    private val _coachIntensity = MutableStateFlow(prefs.getString("coach_intensity", "Light Touch") ?: "Light Touch")
    val coachIntensity: StateFlow<String> = _coachIntensity

    private val _reduceMotion = MutableStateFlow(prefs.getBoolean("reduce_motion", false))
    val reduceMotion: StateFlow<Boolean> = _reduceMotion

    private val _userIdentity = MutableStateFlow(prefs.getString("user_identity", "") ?: "")
    val userIdentity: StateFlow<String> = _userIdentity

    private val _isOnboarded = MutableStateFlow(prefs.getBoolean("is_onboarded", false))
    val isOnboarded: StateFlow<Boolean> = _isOnboarded

    fun setThemeMode(mode: Int) {
        prefs.edit().putInt("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    fun setWidgetDisplayMode(mode: Int) {
        prefs.edit().putInt("widget_display_mode", mode).apply()
        _widgetDisplayMode.value = mode
    }

    fun setWidgetTheme(theme: Int) {
        prefs.edit().putInt("widget_theme", theme).apply()
        _widgetTheme.value = theme
    }

    fun setLocationSuggestionsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("location_suggestions_enabled", enabled).apply()
        _locationSuggestionsEnabled.value = enabled
    }

    fun setHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("haptics_enabled", enabled).apply()
        _hapticsEnabled.value = enabled
    }

    fun setHapticIntensity(intensity: String) {
        prefs.edit().putString("haptic_intensity", intensity).apply()
        _hapticIntensity.value = intensity
    }

    fun setCoachEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("coach_enabled", enabled).apply()
        _coachEnabled.value = enabled
    }

    fun setCoachIntensity(intensity: String) {
        prefs.edit().putString("coach_intensity", intensity).apply()
        _coachIntensity.value = intensity
    }

    fun setReduceMotion(reduce: Boolean) {
        prefs.edit().putBoolean("reduce_motion", reduce).apply()
        _reduceMotion.value = reduce
    }

    fun setUserIdentity(identity: String) {
        prefs.edit().putString("user_identity", identity).apply()
        _userIdentity.value = identity
    }

    fun setIsOnboarded(onboarded: Boolean) {
        prefs.edit().putBoolean("is_onboarded", onboarded).apply()
        _isOnboarded.value = onboarded
    }
}
