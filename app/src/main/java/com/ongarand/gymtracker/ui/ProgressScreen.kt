package com.ongarand.gymtracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ongarand.gymtracker.AppViewModel
import com.ongarand.gymtracker.logic.fmtKg
import com.ongarand.gymtracker.logic.isPlateau
import com.ongarand.gymtracker.logic.sessionPoints
import com.ongarand.gymtracker.logic.short
import com.ongarand.gymtracker.logic.toLocalDate
import com.ongarand.gymtracker.logic.weekStart
import java.time.LocalDate

@Composable
fun ProgressScreen(vm: AppViewModel) {
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val sets by vm.allSets.collectAsStateWithLifecycle()

    var selectedId by rememberSaveable { mutableStateOf(-1L) }
    var menuOpen by remember { mutableStateOf(false) }

    val doneIds = remember(sets) { sets.map { it.exerciseId }.toSet() }
    val withData = exercises.filter { it.id in doneIds }
    val ex = withData.find { it.id == selectedId } ?: withData.firstOrNull()
    val points = remember(sets, ex?.id) { if (ex == null) emptyList() else sessionPoints(sets, ex.id) }

    val today = LocalDate.now()
    val ws = today.weekStart()
    val weekKeys = (7 downTo 0).map { ws.minusWeeks(it.toLong()) }
    val volByWeek = remember(sets) {
        val m = sets.groupBy { it.loggedAt.toLocalDate().weekStart() }
            .mapValues { (_, v) -> v.sumOf { it.weight * it.reps } }
        weekKeys.map { (m[it] ?: 0.0).toFloat() }
    }
    val muscleOf = exercises.associate { it.id to it.muscle }
    val setsByMuscle = remember(sets, exercises) {
        sets.filter { it.loggedAt.toLocalDate().weekStart() == ws }
            .groupingBy { muscleOf[it.exerciseId] ?: "Lainnya" }.eachCount()
            .entries.sortedByDescending { it.value }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenTitle("Progres") }

        if (ex == null) {
            item { AppCard { Muted("Belum ada data latihan. Catat set pertamamu dan grafik progres akan muncul di sini.") } }
        } else {
            item {
                Box {
                    OutlinedButton(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("${ex.name}  ▾")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        withData.forEach { e ->
                            DropdownMenuItem(
                                text = { Text(e.name) },
                                onClick = {
                                    selectedId = e.id
                                    menuOpen = false
                                }
                            )
                        }
                    }
                }
            }
            item {
                AppCard {
                    SectionTitle("Estimasi 1RM per sesi (kg)")
                    LineChart(
                        series = listOf(Series(points.map { it.bestE1rm.toFloat() }, MaterialTheme.colorScheme.primary)),
                        xLabels = points.map { it.time.toLocalDate().short() }
                    )
                    val pr = points.maxOfOrNull { it.bestE1rm }
                    val topW = points.maxOfOrNull { it.topWeight }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(if (pr != null) "${fmtKg(pr)} kg" else "-", fontWeight = FontWeight.Bold)
                            Muted("PR est. 1RM")
                        }
                        Column {
                            Text(if (topW != null) "${fmtKg(topW)} kg" else "-", fontWeight = FontWeight.Bold)
                            Muted("Beban terberat")
                        }
                        Column {
                            Text("${points.size}", fontWeight = FontWeight.Bold)
                            Muted("Sesi")
                        }
                    }
                    if (isPlateau(points)) {
                        Text(
                            "⚠️ Plateau: 3 sesi terakhir tanpa rekor. Coba deload 10%, ubah rep range, atau variasi latihan.",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            item {
                AppCard {
                    SectionTitle("Volume per sesi (kg × reps)")
                    LineChart(
                        series = listOf(Series(points.map { it.volume.toFloat() }, MaterialTheme.colorScheme.secondary)),
                        xLabels = points.map { it.time.toLocalDate().short() }
                    )
                }
            }
            item {
                AppCard {
                    SectionTitle("Riwayat terakhir")
                    points.takeLast(5).reversed().forEach { p ->
                        Text(p.time.toLocalDate().short(), fontWeight = FontWeight.SemiBold)
                        Muted(p.sets.joinToString("  ·  ") { "${fmtKg(it.weight)}×${it.reps}" })
                    }
                }
            }
        }

        item {
            AppCard {
                SectionTitle("Volume mingguan (kg, 8 minggu)")
                BarChart(values = volByWeek, labels = weekKeys.map { it.dayOfMonth.toString() })
            }
        }
        item {
            AppCard {
                SectionTitle("Set per otot minggu ini")
                if (setsByMuscle.isEmpty()) {
                    Muted("Belum ada set minggu ini.")
                } else {
                    val max = setsByMuscle.first().value.toFloat()
                    setsByMuscle.forEach { (m, c) -> HBar(m, c.toFloat(), max, "$c set") }
                    Muted("Patokan umum: ±10–20 set per kelompok otot per minggu untuk hipertrofi.")
                }
            }
        }
    }
}
