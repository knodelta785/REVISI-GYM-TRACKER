package com.ongarand.gymtracker.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GymDao {
    // ---- Latihan (library) ----
    @Query("SELECT * FROM exercises ORDER BY muscle, name")
    fun exercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises")
    suspend fun exercisesOnce(): List<Exercise>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun exerciseCount(): Int

    @Insert
    suspend fun insertExercise(e: Exercise): Long

    @Update
    suspend fun updateExercise(e: Exercise)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteExercise(id: Long)

    // ---- Sesi ----
    @Insert
    suspend fun insertSession(s: Session): Long

    @Update
    suspend fun updateSession(s: Session)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun activeSession(): Flow<Session?>

    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun activeSessionOnce(): Session?

    @Query("SELECT * FROM sessions WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    fun finishedSessions(): Flow<List<Session>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSessionExercise(se: SessionExercise)

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sid ORDER BY position")
    fun sessionExercises(sid: Long): Flow<List<SessionExercise>>

    @Query("SELECT COUNT(*) FROM session_exercises WHERE sessionId = :sid")
    suspend fun sessionExerciseCount(sid: Long): Int

    @Query("DELETE FROM session_exercises WHERE sessionId = :sid AND exerciseId = :eid")
    suspend fun removeSessionExercise(sid: Long, eid: Long)

    @Query("DELETE FROM session_exercises WHERE sessionId = :sid")
    suspend fun deleteSessionExercises(sid: Long)

    // ---- Set ----
    @Insert
    suspend fun insertSet(s: SetEntry): Long

    @Query("DELETE FROM sets WHERE id = :id")
    suspend fun deleteSet(id: Long)

    @Query("SELECT * FROM sets ORDER BY loggedAt")
    fun allSets(): Flow<List<SetEntry>>

    @Query("SELECT * FROM sets WHERE exerciseId = :eid")
    suspend fun setsForExercise(eid: Long): List<SetEntry>

    @Query("SELECT * FROM sets WHERE sessionId = :sid")
    suspend fun setsOfSession(sid: Long): List<SetEntry>

    @Query("SELECT COUNT(*) FROM sets WHERE loggedAt >= :since")
    suspend fun setsSince(since: Long): Int

    @Query("SELECT MAX(loggedAt) FROM sets")
    suspend fun lastSetTime(): Long?

    // ---- Tubuh ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBody(b: BodyEntry)

    @Query("SELECT * FROM body ORDER BY dayEpoch")
    fun body(): Flow<List<BodyEntry>>

    @Query("SELECT * FROM body WHERE dayEpoch = :d LIMIT 1")
    suspend fun bodyOnDay(d: Long): BodyEntry?

    @Query("DELETE FROM body WHERE id = :id")
    suspend fun deleteBody(id: Long)

    // ---- Nutrisi ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNutrition(n: NutritionEntry)

    @Query("SELECT * FROM nutrition ORDER BY dayEpoch DESC LIMIT 60")
    fun nutrition(): Flow<List<NutritionEntry>>

    // ---- Program ----
    @Query("SELECT * FROM templates ORDER BY name")
    fun templates(): Flow<List<Template>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplate(t: Template)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun deleteTemplate(id: Long)
}

@Database(
    entities = [
        Exercise::class, Session::class, SessionExercise::class, SetEntry::class,
        BodyEntry::class, NutritionEntry::class, Template::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): GymDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ongarand.db"
                ).build().also { instance = it }
            }
    }
}
