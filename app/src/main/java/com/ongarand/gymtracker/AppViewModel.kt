package com.ongarand.gymtracker

import android.app.Application
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ongarand.gymtracker.data.AppDatabase
import com.ongarand.gymtracker.data.BodyEntry
import com.ongarand.gymtracker.data.Exercise
import com.ongarand.gymtracker.data.NutritionEntry
import com.ongarand.gymtracker.data.Prefs
import com.ongarand.gymtracker.data.Seed
import com.ongarand.gymtracker.data.Session
import com.ongarand.gymtracker.data.SessionExercise
import com.ongarand.gymtracker.data.SetEntry
import com.ongarand.gymtracker.data.Template
import com.ongarand.gymtracker.logic.e1rm
import com.ongarand.gymtracker.logic.fmtKg
import com.ongarand.gymtracker.logic.prSetIds
import com.ongarand.gymtracker.logic.toLocalDate
import com.ongarand.gymtracker.work.Reminders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx = app.applicationContext
    private val dao = AppDatabase.get(ctx).dao()
    val prefs = Prefs(ctx)

    private fun <T> Flow<T>.hot(init: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), init)

    val exercises = dao.exercises().hot(emptyList())
    val templates = dao.templates().hot(emptyList())
    val finishedSessions = dao.finishedSessions().hot(emptyList())
    val allSets = dao.allSets().hot(emptyList())
    val body = dao.body().hot(emptyList())
    val nutrition = dao.nutrition().hot(emptyList())
    val activeSession = dao.activeSession().hot(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessionExercises = activeSession
        .flatMapLatest { s ->
            if (s == null) flowOf(emptyList<SessionExercise>()) else dao.sessionExercises(s.id)
        }
        .hot(emptyList())

    val activeSets = combine(activeSession, allSets) { s, sets ->
        if (s == null) emptyList<SetEntry>() else sets.filter { it.sessionId == s.id }
    }.hot(emptyList())

    /** Pesan singkat (PR, error, dll.) yang ditampilkan sebagai snackbar. */
    val events = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val summary = MutableStateFlow<String?>(null)
    val restLeft = MutableStateFlow(0)
    val importing = MutableStateFlow(false)
    private var restJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (dao.exerciseCount() == 0) Seed.run(dao)
        }
        val p = prefs.state.value
        Reminders.apply(ctx, p.reminderOn, p.reminderHour, p.reminderMinute)
    }

    // ---------------- Sesi latihan ----------------

    fun startSession(title: String, exerciseIds: List<Long>) {
        viewModelScope.launch {
            if (dao.activeSessionOnce() != null) return@launch
            val id = dao.insertSession(Session(title = title, startedAt = System.currentTimeMillis()))
            exerciseIds.forEachIndexed { i, e -> dao.addSessionExercise(SessionExercise(id, e, i)) }
        }
    }

    fun addExerciseToSession(exerciseId: Long) {
        viewModelScope.launch {
            val s = dao.activeSessionOnce() ?: return@launch
            dao.addSessionExercise(SessionExercise(s.id, exerciseId, dao.sessionExerciseCount(s.id)))
        }
    }

    fun removeExerciseFromSession(exerciseId: Long) {
        viewModelScope.launch {
            val s = dao.activeSessionOnce() ?: return@launch
            dao.removeSessionExercise(s.id, exerciseId)
        }
    }

    fun logSet(sessionId: Long, exerciseId: Long, weight: Double, reps: Int) {
        viewModelScope.launch {
            val prevMax = dao.setsForExercise(exerciseId).maxOfOrNull { e1rm(it.weight, it.reps) } ?: 0.0
            dao.insertSet(
                SetEntry(
                    sessionId = sessionId, exerciseId = exerciseId,
                    weight = weight, reps = reps, loggedAt = System.currentTimeMillis()
                )
            )
            val e = e1rm(weight, reps)
            if (prevMax > 0 && e > prevMax + 0.001) {
                val name = exercises.value.find { it.id == exerciseId }?.name ?: "latihan ini"
                events.tryEmit("🔥 PR baru di $name! Est. 1RM ${fmtKg(e)} kg")
            }
            startRest()
        }
    }

    fun deleteSet(id: Long) {
        viewModelScope.launch { dao.deleteSet(id) }
    }

    fun finishSession() {
        viewModelScope.launch {
            val s = dao.activeSessionOnce() ?: return@launch
            val sets = dao.setsOfSession(s.id)
            skipRest()
            if (sets.isEmpty()) {
                removeSession(s.id)
                events.tryEmit("Sesi kosong dibuang")
                return@launch
            }
            val end = System.currentTimeMillis()
            dao.updateSession(s.copy(endedAt = end))
            val prIds = prSetIds(allSets.value)
            val prs = sets.count { it.id in prIds }
            val vol = sets.sumOf { it.weight * it.reps }
            val minutes = ((end - s.startedAt) / 60_000).coerceAtLeast(1)
            summary.value = "${s.title} selesai!\n\n" +
                "⏱ $minutes menit  ·  ${sets.size} set\n" +
                "🏋️ Volume ${fmtKg(vol)} kg\n" +
                "🏆 $prs PR baru\n\n" +
                "Konsistensi adalah kuncinya. Sampai jumpa di sesi berikutnya!"
        }
    }

    fun discardSession() {
        viewModelScope.launch {
            val s = dao.activeSessionOnce() ?: return@launch
            skipRest()
            removeSession(s.id)
        }
    }

    private suspend fun removeSession(id: Long) {
        dao.deleteSessionExercises(id)
        dao.deleteSession(id)
    }

    fun dismissSummary() {
        summary.value = null
    }

    // ---------------- Timer istirahat ----------------

    private fun startRest() {
        restJob?.cancel()
        restLeft.value = prefs.state.value.restSeconds
        restJob = viewModelScope.launch {
            while (restLeft.value > 0) {
                delay(1000)
                restLeft.value -= 1
            }
            beep()
            events.tryEmit("Istirahat selesai — lanjut set berikutnya 💪")
        }
    }

    fun skipRest() {
        restJob?.cancel()
        restLeft.value = 0
    }

    private fun beep() {
        try {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80).startTone(ToneGenerator.TONE_PROP_BEEP, 300)
        } catch (e: Exception) {
            // abaikan jika perangkat tidak mendukung
        }
    }

    // ---------------- Library latihan & video ----------------

    fun saveExercise(ex: Exercise, pendingVideo: Uri?, removeVideo: Boolean) {
        viewModelScope.launch {
            var e = ex
            if (removeVideo || pendingVideo != null) {
                deleteVideoFile(ex.videoUri)
                e = e.copy(videoUri = null)
            }
            val id = if (e.id == 0L) dao.insertExercise(e) else {
                dao.updateExercise(e)
                e.id
            }
            if (pendingVideo != null) {
                importing.value = true
                val saved = materializeVideo(pendingVideo, "ex$id")
                importing.value = false
                if (saved != null) dao.updateExercise(e.copy(id = id, videoUri = saved))
                else events.tryEmit("Gagal mengimpor video")
            }
        }
    }

    fun deleteExercise(ex: Exercise) {
        viewModelScope.launch {
            deleteVideoFile(ex.videoUri)
            dao.deleteExercise(ex.id)
        }
    }

    /** Impor banyak video sekaligus: tiap video menjadi satu latihan baru (nama dari nama file). */
    fun importVideos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            importing.value = true
            var ok = 0
            uris.forEach { uri ->
                val name = displayName(uri)
                val id = dao.insertExercise(
                    Exercise(name = name, muscle = "Lainnya", kind = "Compound", equipment = "-")
                )
                val saved = materializeVideo(uri, "ex$id")
                if (saved != null) {
                    dao.updateExercise(
                        Exercise(id = id, name = name, muscle = "Lainnya", kind = "Compound", equipment = "-", videoUri = saved)
                    )
                    ok++
                }
            }
            importing.value = false
            events.tryEmit("$ok dari ${uris.size} video diimpor. Edit latihan untuk mengatur otot & nama.")
        }
    }

    private fun displayName(uri: Uri): String {
        val raw = try {
            ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (e: Exception) {
            null
        } ?: "Latihan baru"
        return raw.substringBeforeLast('.').replace('_', ' ').replace('-', ' ').trim().ifEmpty { "Latihan baru" }
    }

    private suspend fun materializeVideo(uri: Uri, tag: String): String? = withContext(Dispatchers.IO) {
        try {
            if (prefs.state.value.copyVideos) {
                val ext = MimeTypeMap.getSingleton()
                    .getExtensionFromMimeType(ctx.contentResolver.getType(uri)) ?: "mp4"
                val dir = File(ctx.filesDir, "videos").apply { mkdirs() }
                val out = File(dir, "${tag}_${System.currentTimeMillis()}.$ext")
                val input = ctx.contentResolver.openInputStream(uri) ?: return@withContext null
                input.use { i -> out.outputStream().use { o -> i.copyTo(o) } }
                Uri.fromFile(out).toString()
            } else {
                ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                uri.toString()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun deleteVideoFile(uriString: String?) {
        if (uriString == null || !uriString.startsWith("file:")) return
        try {
            Uri.parse(uriString).path?.let { File(it).delete() }
        } catch (e: Exception) {
            // abaikan
        }
    }

    // ---------------- Program ----------------

    fun saveTemplate(t: Template) {
        viewModelScope.launch { dao.upsertTemplate(t) }
    }

    fun deleteTemplate(id: Long) {
        viewModelScope.launch { dao.deleteTemplate(id) }
    }

    // ---------------- Tubuh & nutrisi ----------------

    fun saveBody(weight: Double, waist: Double?, chest: Double?, arm: Double?, thigh: Double?) {
        viewModelScope.launch {
            val day = LocalDate.now().toEpochDay()
            val old = dao.bodyOnDay(day)
            dao.upsertBody(
                BodyEntry(
                    dayEpoch = day,
                    weightKg = weight,
                    waistCm = waist ?: old?.waistCm,
                    chestCm = chest ?: old?.chestCm,
                    armCm = arm ?: old?.armCm,
                    thighCm = thigh ?: old?.thighCm
                )
            )
            events.tryEmit("Data tubuh tersimpan ✅")
        }
    }

    fun deleteBody(id: Long) {
        viewModelScope.launch { dao.deleteBody(id) }
    }

    fun saveNutrition(kcal: Int, protein: Int) {
        viewModelScope.launch {
            dao.upsertNutrition(NutritionEntry(LocalDate.now().toEpochDay(), kcal, protein))
            events.tryEmit("Asupan hari ini tersimpan ✅")
        }
    }

    // ---------------- Pengaturan ----------------

    fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        prefs.update { it.copy(reminderOn = enabled, reminderHour = hour, reminderMinute = minute) }
        Reminders.apply(ctx, enabled, hour, minute)
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val names = exercises.value.associate { it.id to it.name }
                val sb = StringBuilder("tanggal,latihan,beban_kg,reps,est_1rm\n")
                allSets.value.forEach { s ->
                    val n = (names[s.exerciseId] ?: "?").replace("\"", "\"\"")
                    sb.append(s.loggedAt.toLocalDate()).append(",\"").append(n).append("\",")
                        .append(fmtKg(s.weight)).append(',').append(s.reps).append(',')
                        .append(fmtKg(e1rm(s.weight, s.reps))).append('\n')
                }
                sb.append("\ntanggal,berat_kg,pinggang_cm,dada_cm,lengan_cm,paha_cm\n")
                body.value.forEach { b ->
                    sb.append(LocalDate.ofEpochDay(b.dayEpoch)).append(',').append(fmtKg(b.weightKg)).append(',')
                        .append(b.waistCm?.let { fmtKg(it) } ?: "").append(',')
                        .append(b.chestCm?.let { fmtKg(it) } ?: "").append(',')
                        .append(b.armCm?.let { fmtKg(it) } ?: "").append(',')
                        .append(b.thighCm?.let { fmtKg(it) } ?: "").append('\n')
                }
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(sb.toString().toByteArray()) }
                events.tryEmit("Data diekspor ✅")
            } catch (e: Exception) {
                events.tryEmit("Gagal ekspor: ${e.message}")
            }
        }
    }
}
