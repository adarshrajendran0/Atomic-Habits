package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val identityTag: String = "",       // e.g. "runner", "reader"
    val timeOfDay: String = "morning", // morning, afternoon, evening
    val place: String = "",             // Implementation intention: "in [place]"
    val timeOfDaySpecific: String = "", // Implementation intention: "at [time]"
    val frequency: String = "Daily",
    val starterVersion: String = "",    // 2-minute starter version
    val stackAnchorRef: String = "",    // optional: After I [existing habit]
    val cheatDays: String = "",         // comma-separated DayOfWeek names, e.g. "SATURDAY,SUNDAY"
    val linkedWifiSsid: String? = null, // e.g. "HomeNet_5G"
    val linkedLatitude: Double? = null,
    val linkedLongitude: Double? = null,
    val linkedRadiusMeters: Float? = null,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastCompletedDate: String? = null, // YYYY-MM-DD
    val notes: String = "",
    val isArchived: Boolean = false,
    val targetQuantity: Int? = null,
    val targetUnit: String = "",
    val goalDurationDays: Int? = null,
    val cueTimes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

fun Habit.cheatDaySet(): Set<java.time.DayOfWeek> =
    cheatDays.split(",")
        .mapNotNull { it.trim().takeIf(String::isNotEmpty) }
        .mapNotNull { runCatching { java.time.DayOfWeek.valueOf(it) }.getOrNull() }
        .toSet()

fun Habit.cueTimeList(): List<LocalTime> =
    cueTimes.split(",").mapNotNull { it.trim().takeIf(String::isNotEmpty) }
        .mapNotNull { runCatching { LocalTime.parse(it, DateTimeFormatter.ofPattern("h:mm a")) }.getOrNull() }

fun Set<java.time.DayOfWeek>.toCheatDaysString(): String = joinToString(",") { it.name }

@Entity(tableName = "completion_logs")
data class CompletionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val habitId: Int,
    val dateString: String,             // YYYY-MM-DD
    val loggedQuantity: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reflections")
data class Reflection(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateString: String,             // YYYY-MM-DD of the reflection
    val easyHabits: String,
    val forcedHabits: String,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
