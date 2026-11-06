package com.ongarand.gymtracker.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PrefState(
    val weeklyGoal: Int = 4,
    val restSeconds: Int = 90,
    val incrementKg: Double = 2.5,
    val reminderOn: Boolean = false,
    val reminderHour: Int = 18,
    val reminderMinute: Int = 0,
    val male: Boolean = true,
    val age: Int = 25,
    val heightCm: Int = 170,
    val activity: Double = 1.55,
    val goal: String = "maintain",   // "cut" | "maintain" | "bulk"
    val copyVideos: Boolean = true
)

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("ongarand_prefs", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<PrefState> = _state.asStateFlow()

    private fun read() = PrefState(
        weeklyGoal = sp.getInt("weeklyGoal", 4),
        restSeconds = sp.getInt("restSeconds", 90),
        incrementKg = sp.getFloat("incrementKg", 2.5f).toDouble(),
        reminderOn = sp.getBoolean("reminderOn", false),
        reminderHour = sp.getInt("reminderHour", 18),
        reminderMinute = sp.getInt("reminderMinute", 0),
        male = sp.getBoolean("male", true),
        age = sp.getInt("age", 25),
        heightCm = sp.getInt("heightCm", 170),
        activity = sp.getFloat("activity", 1.55f).toDouble(),
        goal = sp.getString("goal", "maintain") ?: "maintain",
        copyVideos = sp.getBoolean("copyVideos", true)
    )

    fun update(block: (PrefState) -> PrefState) {
        val n = block(_state.value)
        _state.value = n
        sp.edit()
            .putInt("weeklyGoal", n.weeklyGoal)
            .putInt("restSeconds", n.restSeconds)
            .putFloat("incrementKg", n.incrementKg.toFloat())
            .putBoolean("reminderOn", n.reminderOn)
            .putInt("reminderHour", n.reminderHour)
            .putInt("reminderMinute", n.reminderMinute)
            .putBoolean("male", n.male)
            .putInt("age", n.age)
            .putInt("heightCm", n.heightCm)
            .putFloat("activity", n.activity.toFloat())
            .putString("goal", n.goal)
            .putBoolean("copyVideos", n.copyVideos)
            .apply()
    }
}
