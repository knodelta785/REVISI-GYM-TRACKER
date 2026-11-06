package com.ongarand.gymtracker.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ongarand.gymtracker.AppViewModel
import com.ongarand.gymtracker.data.Exercise
import com.ongarand.gymtracker.data.Seed
import com.ongarand.gymtracker.data.Template

private val DAYS = listOf("1" to "Sen", "2" to "Sel", "3" to "Rab", "4" to "Kam", "5" to "Jum", "6" to "Sab", "7" to "Min")

@Composable
fun LibraryScreen(vm: AppViewModel, onPlay: (String) -> Unit, onStarted: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Pustaka", Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp))
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Latihan & Video") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Program") })
        }
        if (tab == 0) ExerciseList(vm, onPlay) else TemplateList(vm, onStarted)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseList(vm: AppViewModel, onPlay: (String) -> Unit) {
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val importing by vm.importing.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Exercise?>(null) }
    var creating by remember { mutableStateOf(false) }

    val multiPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        vm.importVideos(uris)
    }

    val filtered = exercises.filter {
        (muscle == null || it.muscle == muscle) && it.name.contains(query, ignoreCase = true)
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    label = { Text("Cari latihan") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = muscle == null, onClick = { muscle = null }, label = { Text("Semua") })
                    Seed.MUSCLES.forEach { m ->
                        FilterChip(selected = muscle == m, onClick = { muscle = if (muscle == m) null else m }, label = { Text(m) })
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { multiPicker.launch(arrayOf("video/*")) },
                    enabled = !importing,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (importing) "Menyalin video…" else "Impor banyak video sekaligus") }
            }
            items(filtered, key = { it.id }) { e ->
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(e.name, fontWeight = FontWeight.SemiBold)
                            Muted("${e.muscle} · ${e.kind} · ${e.equipment}")
                            if (e.videoUri == null) Muted("Belum ada video")
                        }
                        val v = e.videoUri
                        if (v != null) {
                            IconButton(onClick = { onPlay(v) }) {
                                Icon(Icons.Default.PlayCircle, "Putar video", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        IconButton(onClick = { editing = e }) { Icon(Icons.Default.Edit, "Edit") }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { creating = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) { Icon(Icons.Default.Add, "Tambah latihan") }
    }

    if (creating) {
        ExerciseDialog(
            initial = null,
            onDismiss = { creating = false },
            onSave = { ex, uri, rm -> vm.saveExercise(ex, uri, rm); creating = false },
            onDelete = null
        )
    }
    editing?.let { ex ->
        ExerciseDialog(
            initial = ex,
            onDismiss = { editing = null },
            onSave = { e, uri, rm -> vm.saveExercise(e, uri, rm); editing = null },
            onDelete = { vm.deleteExercise(ex); editing = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseDialog(
    initial: Exercise?,
    onDismiss: () -> Unit,
    onSave: (Exercise, Uri?, Boolean) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var muscle by remember { mutableStateOf(initial?.muscle ?: Seed.MUSCLES[0]) }
    var kind by remember { mutableStateOf(initial?.kind ?: "Compound") }
    var equipment by remember { mutableStateOf(initial?.equipment ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var pending by remember { mutableStateOf<Uri?>(null) }
    var removeVideo by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pending = uri
            removeVideo = false
        }
    }
    val hasVideo = pending != null || (initial?.videoUri != null && !removeVideo)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Latihan baru" else "Edit latihan") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nama") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Muted("Otot utama")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Seed.MUSCLES.forEach { m ->
                        FilterChip(selected = muscle == m, onClick = { muscle = m }, label = { Text(m) })
                    }
                }
                Muted("Jenis")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Seed.KINDS.forEach { k ->
                        FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k) })
                    }
                }
                OutlinedTextField(equipment, { equipment = it }, label = { Text("Alat (Barbell, Dumbbell, …)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notes, { notes = it }, label = { Text("Catatan teknik pribadi") }, modifier = Modifier.fillMaxWidth())
                Muted("Video tutorial")
                Text(
                    when {
                        pending != null -> "Video dipilih ✓ (akan disimpan saat menekan Simpan)"
                        hasVideo -> "Video terpasang ✓"
                        else -> "Belum ada video"
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { picker.launch(arrayOf("video/*")) }) {
                        Text(if (hasVideo) "Ganti video" else "Pilih video")
                    }
                    if (hasVideo) {
                        TextButton(onClick = { pending = null; removeVideo = true }) { Text("Hapus video") }
                    }
                }
                if (onDelete != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        Text("  Hapus latihan", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        Exercise(
                            id = initial?.id ?: 0,
                            name = name.trim(),
                            muscle = muscle,
                            kind = kind,
                            equipment = equipment.trim().ifEmpty { "-" },
                            videoUri = initial?.videoUri,
                            notes = notes.trim()
                        ),
                        pending,
                        removeVideo
                    )
                }
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Hapus latihan?") },
            text = { Text("Riwayat set latihan ini ikut terhapus. Tindakan ini tidak bisa dibatalkan.") },
            confirmButton = {
                TextButton(onClick = onDelete) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Batal") } }
        )
    }
}

@Composable
private fun TemplateList(vm: AppViewModel, onStarted: () -> Unit) {
    val templates by vm.templates.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val active by vm.activeSession.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Template?>(null) }
    var creating by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(templates, key = { it.id }) { t ->
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(t.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            val names = t.ids().mapNotNull { id -> exercises.find { it.id == id }?.name }
                            Muted(names.joinToString(", ").ifEmpty { "Belum ada latihan" })
                            val days = DAYS.filter { it.first in t.dayList() }.joinToString(", ") { it.second }
                            Muted(if (days.isEmpty()) "Tanpa jadwal" else "Jadwal: $days")
                        }
                        IconButton(onClick = { editing = t }) { Icon(Icons.Default.Edit, "Edit program") }
                    }
                    Button(
                        enabled = active == null,
                        onClick = {
                            vm.startSession(t.name, t.ids())
                            onStarted()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (active == null) "Mulai ${t.name}" else "Selesaikan sesi berjalan dulu") }
                }
            }
        }
        FloatingActionButton(
            onClick = { creating = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) { Icon(Icons.Default.Add, "Program baru") }
    }

    if (creating) {
        TemplateDialog(
            initial = null, exercises = exercises,
            onDismiss = { creating = false },
            onSave = { vm.saveTemplate(it); creating = false },
            onDelete = null
        )
    }
    editing?.let { t ->
        TemplateDialog(
            initial = t, exercises = exercises,
            onDismiss = { editing = null },
            onSave = { vm.saveTemplate(it); editing = null },
            onDelete = { vm.deleteTemplate(t.id); editing = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplateDialog(
    initial: Template?,
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onSave: (Template) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    val days = remember { mutableStateListOf<String>().apply { addAll(initial?.dayList() ?: emptyList()) } }
    val selected = remember { mutableStateListOf<Long>().apply { addAll(initial?.ids() ?: emptyList()) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Program baru" else "Edit program") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nama program") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Muted("Hari latihan (untuk pengingat di Beranda)")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DAYS.forEach { (id, label) ->
                        FilterChip(
                            selected = id in days,
                            onClick = { if (id in days) days.remove(id) else days.add(id) },
                            label = { Text(label) }
                        )
                    }
                }
                Muted("Latihan (urutan sesuai urutan pilihan)")
                exercises.forEach { e ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = e.id in selected,
                            onCheckedChange = { if (e.id in selected) selected.remove(e.id) else selected.add(e.id) }
                        )
                        Column {
                            Text(e.name)
                            Muted(e.muscle)
                        }
                    }
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        Text("  Hapus program", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && selected.isNotEmpty(),
                onClick = {
                    onSave(
                        Template(
                            id = initial?.id ?: 0,
                            name = name.trim(),
                            exerciseIds = selected.joinToString(","),
                            days = days.sorted().joinToString(",")
                        )
                    )
                }
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
