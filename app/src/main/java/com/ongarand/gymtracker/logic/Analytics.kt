package com.ongarand.gymtracker.logic

import com.ongarand.gymtracker.data.BodyEntry
import com.ongarand.gymtracker.data.Exercise
import com.ongarand.gymtracker.data.PrefState
import com.ongarand.gymtracker.data.Session
import com.ongarand.gymtracker.data.SetEntry
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

// ---------- Util ----------

fun LocalDate.weekStart(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

private val ID = Locale.forLanguageTag("id-ID")
private val shortFmt = DateTimeFormatter.ofPattern("d MMM", ID)
private val longFmt = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", ID)

fun LocalDate.short(): String = format(shortFmt)
fun LocalDate.long(): String = format(longFmt)

fun fmtKg(d: Double): String =
    if (d % 1.0 == 0.0) d.toLong().toString() else String.format(Locale.US, "%.1f", d)

fun fmt1(d: Double): String = String.format(Locale.US, "%.1f", d)

fun parseD(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()

// ---------- Kekuatan / overload ----------

/** Estimasi 1RM (rumus Epley). */
fun e1rm(weight: Double, reps: Int): Double = if (reps <= 1) weight else weight * (1 + reps / 30.0)

data class SessionPoint(
    val sessionId: Long,
    val time: Long,
    val bestE1rm: Double,
    val topWeight: Double,
    val volume: Double,
    val sets: List<SetEntry>
)

fun sessionPoints(sets: List<SetEntry>, exerciseId: Long): List<SessionPoint> =
    sets.filter { it.exerciseId == exerciseId }
        .groupBy { it.sessionId }
        .map { (sid, list) ->
            SessionPoint(
                sessionId = sid,
                time = list.minOf { it.loggedAt },
                bestE1rm = list.maxOf { e1rm(it.weight, it.reps) },
                topWeight = list.maxOf { it.weight },
                volume = list.sumOf { it.weight * it.reps },
                sets = list.sortedBy { it.loggedAt }
            )
        }
        .sortedBy { it.time }

data class Suggestion(val weight: Double, val reps: Int, val text: String)

/** Saran progressive overload berdasarkan sesi terakhir (selain sesi yang sedang berjalan). */
fun suggest(sets: List<SetEntry>, exerciseId: Long, currentSessionId: Long?, increment: Double): Suggestion? {
    val prev = sessionPoints(sets.filter { it.sessionId != currentSessionId }, exerciseId).lastOrNull()
        ?: return null
    val best = prev.sets.maxByOrNull { e1rm(it.weight, it.reps) } ?: return null
    val last = "${fmtKg(best.weight)} kg × ${best.reps}"
    return if (best.reps >= 12) {
        val w = best.weight + increment
        Suggestion(w, 8, "Terakhir $last. Sudah 12 rep → naikkan ke ${fmtKg(w)} kg, target 8 rep.")
    } else {
        Suggestion(best.weight, best.reps + 1, "Terakhir $last. Coba ${best.reps + 1} rep dengan beban sama.")
    }
}

/** ID set yang memecahkan rekor 1RM-estimasi (set pertama kali dikerjakan tidak dihitung PR). */
fun prSetIds(sets: List<SetEntry>): Set<Long> {
    val res = mutableSetOf<Long>()
    sets.groupBy { it.exerciseId }.forEach { (_, list) ->
        var max = 0.0
        list.sortedBy { it.loggedAt }.forEach { s ->
            val e = e1rm(s.weight, s.reps)
            if (max > 0 && e > max + 0.001) res += s.id
            if (e > max) max = e
        }
    }
    return res
}

/** Plateau: 3 sesi terakhir tidak melampaui rekor sebelumnya. */
fun isPlateau(points: List<SessionPoint>): Boolean {
    if (points.size < 4) return false
    val last3 = points.takeLast(3)
    val before = points.dropLast(3)
    return last3.maxOf { it.bestE1rm } <= before.maxOf { it.bestE1rm } + 0.001
}

// ---------- Konsistensi ----------

fun dayStreak(daysDesc: List<LocalDate>, today: LocalDate): Int {
    if (daysDesc.isEmpty()) return 0
    if (ChronoUnit.DAYS.between(daysDesc.first(), today) > 3) return 0
    var streak = 1
    for (i in 1 until daysDesc.size) {
        if (ChronoUnit.DAYS.between(daysDesc[i], daysDesc[i - 1]) <= 3) streak++ else break
    }
    return streak
}

fun weekStreak(distinctDays: List<LocalDate>, goal: Int, today: LocalDate): Int {
    val g = goal.coerceAtLeast(1)
    val perWeek = distinctDays.groupingBy { it.weekStart() }.eachCount()
    var week = today.weekStart()
    var streak = 0
    if ((perWeek[week] ?: 0) >= g) streak++
    week = week.minusWeeks(1)
    while ((perWeek[week] ?: 0) >= g) {
        streak++
        week = week.minusWeeks(1)
    }
    return streak
}

data class WeekStats(val sessions: Int, val volume: Double, val sets: Int, val prs: Int)

fun weekStats(sessions: List<Session>, sets: List<SetEntry>, prIds: Set<Long>, weekStart: LocalDate): WeekStats {
    val end = weekStart.plusDays(7)
    fun inWeek(ms: Long): Boolean {
        val d = ms.toLocalDate()
        return !d.isBefore(weekStart) && d.isBefore(end)
    }
    val ss = sets.filter { inWeek(it.loggedAt) }
    return WeekStats(
        sessions = sessions.count { inWeek(it.startedAt) },
        volume = ss.sumOf { it.weight * it.reps },
        sets = ss.size,
        prs = ss.count { it.id in prIds }
    )
}

data class Badge(val emoji: String, val title: String, val desc: String, val unlocked: Boolean)

fun badges(totalSessions: Int, weekStreak: Int, prCount: Int, totalVolume: Double): List<Badge> = listOf(
    Badge("🌱", "Langkah Pertama", "1 sesi selesai", totalSessions >= 1),
    Badge("💪", "Rutin 10", "10 sesi selesai", totalSessions >= 10),
    Badge("🏋️", "Veteran 50", "50 sesi selesai", totalSessions >= 50),
    Badge("🔥", "Konsisten 4 Minggu", "Target mingguan 4 minggu beruntun", weekStreak >= 4),
    Badge("⚡", "Konsisten 8 Minggu", "Target mingguan 8 minggu beruntun", weekStreak >= 8),
    Badge("👑", "Konsisten 12 Minggu", "Target mingguan 12 minggu beruntun", weekStreak >= 12),
    Badge("🏆", "Pemecah Rekor", "1 PR", prCount >= 1),
    Badge("🚀", "PR Hunter", "10 PR", prCount >= 10),
    Badge("🧱", "10 Ton", "Total volume 10.000 kg", totalVolume >= 10_000),
    Badge("🏔️", "100 Ton", "Total volume 100.000 kg", totalVolume >= 100_000)
)

data class HomeStats(
    val thisWeek: WeekStats,
    val lastWeek: WeekStats,
    val dayStreak: Int,
    val weekStreak: Int,
    val totalSessions: Int,
    val totalPrs: Int,
    val totalVolume: Double,
    val plateaus: List<String>,
    val badges: List<Badge>,
    val daysSinceLast: Long?
)

fun homeStats(
    sessions: List<Session>,
    sets: List<SetEntry>,
    exercises: List<Exercise>,
    goal: Int,
    today: LocalDate
): HomeStats {
    val prIds = prSetIds(sets)
    val ws = today.weekStart()
    val thisWeek = weekStats(sessions, sets, prIds, ws)
    val lastWeek = weekStats(sessions, sets, prIds, ws.minusWeeks(1))
    val days = sessions.map { it.startedAt.toLocalDate() }.distinct().sortedDescending()
    val plateaus = exercises.mapNotNull { ex ->
        val pts = sessionPoints(sets, ex.id)
        if (pts.size >= 4 && pts.last().time.toLocalDate().isAfter(today.minusDays(22)) && isPlateau(pts)) ex.name else null
    }.take(3)
    val totalVol = sets.sumOf { it.weight * it.reps }
    val wk = weekStreak(days, goal, today)
    return HomeStats(
        thisWeek = thisWeek,
        lastWeek = lastWeek,
        dayStreak = dayStreak(days, today),
        weekStreak = wk,
        totalSessions = sessions.size,
        totalPrs = prIds.size,
        totalVolume = totalVol,
        plateaus = plateaus,
        badges = badges(sessions.size, wk, prIds.size, totalVol),
        daysSinceLast = days.firstOrNull()?.let { ChronoUnit.DAYS.between(it, today) }
    )
}

// ---------- Berat badan & nutrisi ----------

fun movingAvg(entries: List<BodyEntry>): List<Double> = entries.map { cur ->
    entries.filter { it.dayEpoch in (cur.dayEpoch - 6)..cur.dayEpoch }.map { it.weightKg }.average()
}

/** Perubahan berat (rata-rata bergerak) per minggu, atau null bila data belum cukup. */
fun weeklyChange(e: List<BodyEntry>): Double? {
    if (e.size < 2) return null
    val ma = movingAvg(e)
    val lastI = e.lastIndex
    val ref = e.indexOfLast { it.dayEpoch <= e[lastI].dayEpoch - 7 }
    val from = if (ref >= 0) ref else 0
    val span = e[lastI].dayEpoch - e[from].dayEpoch
    if (span < 4) return null
    return (ma[lastI] - ma[from]) / (span / 7.0)
}

fun trendText(goal: String, delta: Double?): String {
    if (delta == null) return "Catat berat badan beberapa hari (idealnya 2 minggu) untuk melihat tren."
    val d = String.format(Locale.US, "%+.2f", delta)
    return when (goal) {
        "cut" -> when {
            delta > -0.1 -> "Tren $d kg/minggu: berat belum turun. Kurangi ±150 kkal/hari atau tambah aktivitas."
            delta < -1.0 -> "Tren $d kg/minggu: terlalu cepat, risiko kehilangan otot. Naikkan kalori sedikit."
            else -> "Tren $d kg/minggu: laju penurunan sehat. Pertahankan protein tinggi."
        }
        "bulk" -> when {
            delta < 0.1 -> "Tren $d kg/minggu: surplus belum cukup. Tambah ±150–200 kkal/hari."
            delta > 0.7 -> "Tren $d kg/minggu: naik terlalu cepat, lemak ikut bertambah. Kurangi surplus."
            else -> "Tren $d kg/minggu: laju bulking ideal."
        }
        else -> if (kotlin.math.abs(delta) <= 0.25)
            "Tren $d kg/minggu: berat stabil, sesuai target maintain."
        else
            "Tren $d kg/minggu: berat bergeser dari maintain. Sesuaikan asupan kalori."
    }
}

data class Targets(val kcal: Int, val protein: Int)

/** Estimasi kebutuhan (Mifflin-St Jeor). Hanya perkiraan, bukan saran medis. */
fun targets(p: PrefState, weightKg: Double): Targets {
    val bmr = 10 * weightKg + 6.25 * p.heightCm - 5 * p.age + (if (p.male) 5 else -161)
    val tdee = bmr * p.activity
    val mult = when (p.goal) { "cut" -> 0.85; "bulk" -> 1.10; else -> 1.0 }
    val protein = weightKg * (if (p.goal == "cut") 2.0 else 1.8)
    return Targets((tdee * mult).roundToInt(), protein.roundToInt())
}
