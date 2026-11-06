package com.ongarand.gymtracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscle: String,
    val kind: String,          // "Compound" atau "Isolasi"
    val equipment: String,
    val videoUri: String? = null,
    val notes: String = ""
)

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startedAt: Long,
    val endedAt: Long? = null
)

/** Daftar latihan yang dipilih untuk satu sesi (urutan = position). */
@Entity(tableName = "session_exercises", primaryKeys = ["sessionId", "exerciseId"])
data class SessionExercise(
    val sessionId: Long,
    val exerciseId: Long,
    val position: Int
)

@Entity(
    tableName = "sets",
    foreignKeys = [
        ForeignKey(
            entity = Session::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("exerciseId")]
)
data class SetEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val weight: Double,
    val reps: Int,
    val loggedAt: Long
)

@Entity(tableName = "body", indices = [Index(value = ["dayEpoch"], unique = true)])
data class BodyEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayEpoch: Long,
    val weightKg: Double,
    val waistCm: Double? = null,
    val chestCm: Double? = null,
    val armCm: Double? = null,
    val thighCm: Double? = null
)

@Entity(tableName = "nutrition")
data class NutritionEntry(
    @PrimaryKey val dayEpoch: Long,
    val kcal: Int,
    val proteinG: Int
)

/** Program latihan. days = hari ISO dipisah koma ("1,3,5" = Sen, Rab, Jum). */
@Entity(tableName = "templates")
data class Template(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val exerciseIds: String,
    val days: String = ""
) {
    fun ids(): List<Long> = exerciseIds.split(",").mapNotNull { it.trim().toLongOrNull() }
    fun dayList(): List<String> = days.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
