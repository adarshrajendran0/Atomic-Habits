package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE habits ADD COLUMN cheatDays TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE habits ADD COLUMN linkedWifiSsid TEXT")
        database.execSQL("ALTER TABLE habits ADD COLUMN linkedLatitude REAL")
        database.execSQL("ALTER TABLE habits ADD COLUMN linkedLongitude REAL")
        database.execSQL("ALTER TABLE habits ADD COLUMN linkedRadiusMeters REAL")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE habits ADD COLUMN targetQuantity INTEGER")
        database.execSQL("ALTER TABLE habits ADD COLUMN targetUnit TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE habits ADD COLUMN goalDurationDays INTEGER")
        database.execSQL("ALTER TABLE habits ADD COLUMN cueTimes TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE completion_logs ADD COLUMN loggedQuantity INTEGER")
    }
}

@Database(
    entities = [Habit::class, CompletionLog::class, Reflection::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "atomic_habits_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
