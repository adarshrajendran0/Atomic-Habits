package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY id DESC")
    fun getAllActiveHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits ORDER BY id DESC")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Int): Habit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("SELECT * FROM completion_logs WHERE habitId = :habitId")
    fun getLogsByHabitId(habitId: Int): Flow<List<CompletionLog>>

    @Query("SELECT * FROM completion_logs")
    fun getAllCompletionLogs(): Flow<List<CompletionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletionLog(log: CompletionLog)

    @Update
    suspend fun updateCompletionLog(log: CompletionLog)

    @Query("DELETE FROM completion_logs WHERE habitId = :habitId AND dateString = :dateString")
    suspend fun deleteCompletionLog(habitId: Int, dateString: String)

    @Query("SELECT * FROM reflections ORDER BY timestamp DESC")
    fun getAllReflections(): Flow<List<Reflection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReflection(reflection: Reflection)
}
