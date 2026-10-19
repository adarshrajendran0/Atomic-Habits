package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class HabitRepository(private val habitDao: HabitDao) {
    val allActiveHabits: Flow<List<Habit>> = habitDao.getAllActiveHabits()
    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()
    val allCompletionLogs: Flow<List<CompletionLog>> = habitDao.getAllCompletionLogs()
    val allReflections: Flow<List<Reflection>> = habitDao.getAllReflections()

    suspend fun getHabitById(id: Int): Habit? = habitDao.getHabitById(id)

    suspend fun insertHabit(habit: Habit): Int {
        return habitDao.insertHabit(habit).toInt()
    }

    suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(habit)
    }

    suspend fun deleteHabit(habit: Habit) {
        habitDao.deleteHabit(habit)
    }

    fun getLogsForHabit(habitId: Int): Flow<List<CompletionLog>> {
        return habitDao.getLogsByHabitId(habitId)
    }

    suspend fun logCompletionWithQuantity(habitId: Int, dateString: String, quantity: Int) {
        val existingLogs = habitDao.getLogsByHabitId(habitId).first()
        val existingLog = existingLogs.find { it.dateString == dateString }

        if (existingLog != null) {
            habitDao.updateCompletionLog(existingLog.copy(loggedQuantity = quantity))
        } else {
            habitDao.insertCompletionLog(CompletionLog(habitId = habitId, dateString = dateString, loggedQuantity = quantity))
        }

        // Recompute streaks
        val updatedLogs = habitDao.getLogsByHabitId(habitId).first()
        val habit = habitDao.getHabitById(habitId) ?: return

        val (currentStreak, bestStreak) = calculateStreaks(updatedLogs.map { it.dateString }, habit.cheatDaySet())
        val lastCompleted = updatedLogs.map { it.dateString }.sortedDescending().firstOrNull()

        val updatedHabit = habit.copy(
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            lastCompletedDate = lastCompleted
        )
        habitDao.updateHabit(updatedHabit)
    }

    suspend fun toggleHabitCompletion(habitId: Int, dateString: String) {
        val existingLogs = habitDao.getLogsByHabitId(habitId).first()
        val alreadyLogged = existingLogs.any { it.dateString == dateString }

        if (alreadyLogged) {
            habitDao.deleteCompletionLog(habitId, dateString)
        } else {
            habitDao.insertCompletionLog(CompletionLog(habitId = habitId, dateString = dateString))
        }

        // Recompute streaks
        val updatedLogs = habitDao.getLogsByHabitId(habitId).first()
        val habit = habitDao.getHabitById(habitId) ?: return

        val (currentStreak, bestStreak) = calculateStreaks(updatedLogs.map { it.dateString }, habit.cheatDaySet())
        val lastCompleted = updatedLogs.map { it.dateString }.sortedDescending().firstOrNull()

        val updatedHabit = habit.copy(
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            lastCompletedDate = lastCompleted
        )
        habitDao.updateHabit(updatedHabit)
    }

    suspend fun insertReflection(reflection: Reflection) {
        habitDao.insertReflection(reflection)
    }

    /**
     * Exact calculations for current and maximum streak lengths
     */
    fun calculateStreaks(dateStrings: List<String>, cheatDays: Set<java.time.DayOfWeek> = emptySet()): Pair<Int, Int> {
        val dateSet = dateStrings.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
        if (dateSet.isEmpty()) return Pair(0, 0)

        // --- Current streak: walk backward from today, passing through cheat days without breaking or counting them ---
        var currentStreak = 0
        var cursor = LocalDate.now()
        if (!dateSet.contains(cursor) && cursor.dayOfWeek !in cheatDays) cursor = cursor.minusDays(1)
        while (true) {
            when {
                dateSet.contains(cursor) -> { currentStreak++; cursor = cursor.minusDays(1) }
                cursor.dayOfWeek in cheatDays -> cursor = cursor.minusDays(1)   // exempt day — skip silently
                else -> break                                                   // a real miss — streak ends here
            }
        }

        // --- Best streak: two completions are "contiguous" if every day between them is a cheat day ---
        var maxStreak = 0
        var tempStreak = 0
        var lastCounted: LocalDate? = null
        for (date in dateSet.sorted()) {
            tempStreak = if (lastCounted == null) {
                1
            } else {
                var probe = lastCounted.plusDays(1)
                var bridgedEntirelyByCheatDays = true
                while (probe.isBefore(date)) {
                    if (probe.dayOfWeek !in cheatDays) { bridgedEntirelyByCheatDays = false; break }
                    probe = probe.plusDays(1)
                }
                if (bridgedEntirelyByCheatDays) tempStreak + 1 else 1
            }
            if (tempStreak > maxStreak) maxStreak = tempStreak
            lastCounted = date
        }
        return Pair(currentStreak, maxStreak)
    }
}
