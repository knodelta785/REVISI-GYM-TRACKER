package com.ongarand.gymtracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ongarand.gymtracker.AppViewModel
import com.ongarand.gymtracker.data.Exercise
import com.ongarand.gymtracker.data.Session
import com.ongarand.gymtracker.data.SetEntry
import com.ongarand.gymtracker.logic.fmtKg
import com.ongarand.gymtracker.logic.parseD
import com.ongarand.gymtracker.logic.prSetIds
import com.ongarand.gymtracker.logic.suggest
import com.ongarand.gymtracker.logic.toLocalDate
import com.ongarand.gymtracker.logic.long
import kotlinx.coroutines.delay

@Composable
fun WorkoutScreen(vm: AppViewModel, onPlay: (String) -> Unit) {
    val active by vm.activeSession.collectAsStateWithLifecycle()
    val s = active
    if (s == null) IdleWorkout(vm) else ActiveWorkout(vm, s, onPlay)
}

@Composable
private fun IdleWorkout(vm: AppViewModel) {
    val templates by vm.templates.collectAsStateWithLifecycle()
    val sessions by vm.finishedSessions.collectAsStateWithLifecycle()
    val sets by vm.allSets.collectAsStateWithLifecycle()

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenTitle("Latihan") }
        item {
            Button(onClick = { vm.startSession("Latihan bebas", emptyList()) }, modifier = Modifier.fillMaxWidth()) {
                Text("Mulai latihan kosong")
            }
        }
        item { SectionTitle("Mulai dari program") }
        items(templates, key = { it.id }) { t ->
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Muted("${t.ids().size} latihan")
                    }
                    Button(onClick = { vm.startSession(t.name, t.ids()) }) { Text("Mulai") }
                }
            }
        }
        item { SectionTitle("Riwayat terakhir") }
        if (sessions.isEmpty()) {
            item { Muted("Belum ada sesi selesai. Sesi pertamamu menunggu 💪") }
        }
        items(sessions.take(10), key = { it.id }) { s ->
            val ss = sets.filter { it.sessionId == s.id }
            AppCard {
                Text("${s.title} · ${s.startedAt.toLocalDate().long()}", fontWeight = FontWeight.SemiBold)
                Muted("${ss.size} set · volume ${fmtKg(ss.sumOf { it.weight * it.reps })} kg")
            }
        }
    }
}

@Composable
private fun ActiveWorkout(vm: AppViewModel, session: Session, onPlay: (String) -> Unit) {
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val sessionEx by vm.sessionExercises.collectAsStateWithLifecycle()
    val activeSets by vm.activeSets.collectAsStateWithLifecycle()
    val allSets by vm.allSets.collectAsStateWithLifecycle()
    val rest by vm.restLeft.collectAsStateWithLifecycle()
    val prefs by vm.prefs.state.collectAsStateWithLifecycle()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    var showPicker by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    val prIds = remember(allSets) { prSetIds(allSets) }
    val elapsed = ((now - session.startedAt) / 1000).coerceAtLeast(0)
    val clock = "%02d:%02d".format(elapsed / 60, elapsed % 60)

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(session.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Muted("⏱ $clock")
                    }
                    Button(onClick = { vm.finishSession() }) { Text("Selesai") }
                }
                if (rest > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.secondary)
                        Text(
                            "  Istirahat ${rest / 60}:${"%02d".format(rest % 60)}",
                            Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = { vm.skipRest() }) { Text("Lewati") }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showPicker = true }) { Text("+ Tambah latihan") }
                    TextButton(onClick = { confirmDiscard = true }) {
                        Text("Batalkan", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        if (sessionEx.isEmpty()) {
            item { Muted("Belum ada latihan. Tekan “+ Tambah latihan” untuk memilih dari pustaka.") }
        }

        items(sessionEx, key = { it.exerciseId }) { se ->
            val ex = exercises.find { it.id == se.exerciseId }
            if (ex != null) {
                val mySets = activeSets.filter { it.exerciseId == ex.id }.sortedBy { it.loggedAt }
                val sugg = remember(allSets, ex.id, session.id, prefs.incrementKg) {
                    suggest(allSets, ex.id, session.id, prefs.incrementKg)
                }
                ExerciseLogCard(
                    ex = ex,
                    mySets = mySets,
                    prIds = prIds,
                    defaultWeight = mySets.lastOrNull()?.weight ?: sugg?.weight,
                    defaultReps = mySets.lastOrNull()?.reps ?: sugg?.reps,
                    hint = sugg?.text,
                    onPlay = onPlay,
                    onLog = { w, r -> vm.logSet(session.id, ex.id, w, r) },
                    onDeleteSet = { vm.deleteSet(it) },
                    onRemove = { vm.removeExerciseFromSession(ex.id) }
                )
            }
        }
    }

    if (showPicker) {
        ExercisePickerDialog(
            exercises = exercises.filter { e -> sessionEx.none { it.exerciseId == e.id } },
            onPick = {
                vm.addExerciseToSession(it.id)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Batalkan sesi?") },
            text = { Text("Semua set di sesi ini akan dihapus.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.discardSession()
                    confirmDiscard = false
                }) { Text("Ya, batalkan", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Lanjut latihan") } }
        )
    }
}

@Composable
private fun ExerciseLogCard(
    ex: Exercise,
    mySets: List<SetEntry>,
    prIds: Set<Long>,
    defaultWeight: Double?,
    defaultReps: Int?,
    hint: String?,
    onPlay: (String) -> Unit,
    onLog: (Double, Int) -> Unit,
    onDeleteSet: (Long) -> Unit,
    onRemove: () -> Unit
) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(ex.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Muted("${ex.muscle} · ${ex.kind} · ${ex.equipment}")
                }
                val v = ex.videoUri
                if (v != null) {
                    IconButton(onClick = { onPlay(v) }) {
                        Icon(Icons.Default.PlayCircle, contentDescription = "Lihat teknik", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = onRemove) { Icon(Icons.Default.Close, contentDescription = "Hapus dari sesi") }
            }
            if (ex.notes.isNotBlank()) Muted("📝 ${ex.notes}")
            if (hint != null && mySets.isEmpty()) {
                Text(hint, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
            }

            mySets.forEachIndexed { i, s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Set ${i + 1}", Modifier.padding(end = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${fmtKg(s.weight)} kg × ${s.reps}", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    if (s.id in prIds) Text("🏆 PR", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { onDeleteSet(s.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus set", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Input set berikutnya; terisi otomatis dari set terakhir / saran overload.
            var w by remember(ex.id, mySets.size) { mutableStateOf(defaultWeight?.let { fmtKg(it) } ?: "") }
            var r by remember(ex.id, mySets.size) { mutableStateOf(defaultReps?.toString() ?: "") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = w, onValueChange = { w = it },
                    label = { Text("Beban (kg)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = r, onValueChange = { r = it },
                    label = { Text("Reps") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.7f)
                )
                Button(onClick = {
                    val wd = parseD(w)
                    val rd = r.trim().toIntOrNull()
                    if (wd != null && wd >= 0 && rd != null && rd > 0) onLog(wd, rd)
                }) { Text("+ Set") }
            }
        }
    }
}

@Composable
fun ExercisePickerDialog(exercises: List<Exercise>, onPick: (Exercise) -> Unit, onDismiss: () -> Unit) {
    var q by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih latihan") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = q, onValueChange = { q = it },
                    label = { Text("Cari") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                val list = exercises.filter { it.name.contains(q, ignoreCase = true) || it.muscle.contains(q, ignoreCase = true) }
                if (list.isEmpty()) Muted("Tidak ada latihan yang cocok.", Modifier.padding(top = 12.dp))
                list.forEach { e ->
                    TextButton(onClick = { onPick(e) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(e.name)
                            Muted("${e.muscle} · ${e.equipment}")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Tutup") } }
    )
}
