package com.ongarand.gymtracker.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ongarand.gymtracker.AppViewModel
import com.ongarand.gymtracker.logic.fmtKg
import com.ongarand.gymtracker.logic.homeStats
import java.time.LocalDate
import kotlin.math.roundToInt

private val QUOTES = listOf(
    "Disiplin adalah jembatan antara tujuan dan pencapaian.",
    "Satu sesi hari ini lebih berharga dari seribu niat besok.",
    "Otot dibangun di dapur dan di tempat tidur, tetapi dimulai dari pintu gym.",
    "Jangan tunggu termotivasi. Mulai dulu, semangat menyusul.",
    "Progres kecil yang konsisten mengalahkan ledakan sesaat.",
    "Kamu tidak harus hebat untuk memulai, tapi harus memulai untuk jadi hebat.",
    "Hari ini berat? Lakukan versi singkatnya. Tetap hadir itu menang.",
    "Beban yang sama dengan form lebih baik tetap adalah progres.",
    "Tubuhmu mengikuti apa yang kamu ulangi setiap hari.",
    "Tidur, makan, latihan. Ulangi."
)

@Composable
fun HomeScreen(vm: AppViewModel, onStart: () -> Unit, onSettings: () -> Unit) {
    val sessions by vm.finishedSessions.collectAsStateWithLifecycle()
    val sets by vm.allSets.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val templates by vm.templates.collectAsStateWithLifecycle()
    val active by vm.activeSession.collectAsStateWithLifecycle()
    val prefs by vm.prefs.state.collectAsStateWithLifecycle()

    val today = LocalDate.now()
    val stats = remember(sessions, sets, exercises, prefs.weeklyGoal, today) {
        homeStats(sessions, sets, exercises, prefs.weeklyGoal, today)
    }
    val todayTemplate = templates.firstOrNull { today.dayOfWeek.value.toString() in it.dayList() }
    val quote = QUOTES[today.dayOfYear % QUOTES.size]
    val goal = prefs.weeklyGoal.coerceAtLeast(1)
    val runningSession = active

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ONGARAND", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    ScreenTitle("Gym Tracker")
                }
                IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, contentDescription = "Pengaturan") }
            }
        }

        item {
            AppCard {
                Text(quote, fontStyle = FontStyle.Italic, style = MaterialTheme.typography.bodyMedium)
            }
        }

        item {
            AppCard {
                when {
                    runningSession != null -> {
                        SectionTitle("Latihan sedang berjalan")
                        Muted(runningSession.title)
                        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Lanjutkan latihan") }
                    }
                    stats.daysSinceLast == 0L -> {
                        SectionTitle("Sudah latihan hari ini ✅")
                        Muted("Istirahat, makan cukup protein, dan tidur yang baik. Itu bagian dari program.")
                    }
                    todayTemplate != null -> {
                        SectionTitle("Hari ini: ${todayTemplate.name}")
                        Muted("${todayTemplate.ids().size} latihan terjadwal")
                        Button(
                            onClick = {
                                vm.startSession(todayTemplate.name, todayTemplate.ids())
                                onStart()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Mulai ${todayTemplate.name}") }
                    }
                    else -> {
                        SectionTitle("Belum ada jadwal hari ini")
                        Muted("Mulai sesi bebas, atau atur hari program di menu Pustaka → Program.")
                        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Mulai latihan") }
                    }
                }
            }
        }

        item {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    ProgressRing(
                        progress = stats.thisWeek.sessions / goal.toFloat(),
                        label = "${stats.thisWeek.sessions}/$goal",
                        sub = "sesi minggu ini"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalFireDepartment, null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.padding(start = 8.dp)) {
                                Text("${stats.dayStreak} hari", fontWeight = FontWeight.Bold)
                                Muted("streak latihan")
                            }
                        }
                        Column {
                            Text("${stats.weekStreak} minggu", fontWeight = FontWeight.Bold)
                            Muted("target mingguan beruntun")
                        }
                    }
                }
            }
        }

        item {
            AppCard {
                SectionTitle("Ringkasan minggu ini")
                val tw = stats.thisWeek
                val lw = stats.lastWeek
                Text("Sesi: ${tw.sessions} (minggu lalu ${lw.sessions})")
                val volLine = if (lw.volume > 0) {
                    val pct = ((tw.volume - lw.volume) / lw.volume * 100).roundToInt()
                    "Volume: ${fmtKg(tw.volume)} kg (${if (pct >= 0) "+" else ""}$pct% vs minggu lalu)"
                } else "Volume: ${fmtKg(tw.volume)} kg"
                Text(volLine)
                Text("PR minggu ini: ${tw.prs}")
                val focus = when {
                    stats.plateaus.isNotEmpty() ->
                        "${stats.plateaus.first()} stagnan: coba deload ±10% atau ubah rentang rep."
                    tw.sessions < goal -> "Kejar target: tinggal ${goal - tw.sessions} sesi lagi minggu ini."
                    else -> "Target tercapai! Coba naikkan beban di latihan utama."
                }
                Muted("🎯 Fokus: $focus")
            }
        }

        if (stats.plateaus.isNotEmpty()) {
            item {
                AppCard {
                    SectionTitle("⚠️ Terdeteksi plateau")
                    stats.plateaus.forEach { Text("• $it — 3 sesi terakhir tanpa rekor baru") }
                    Muted("Saran: deload 1 minggu (beban −10%), tambah variasi, atau cek tidur & asupan protein.")
                }
            }
        }

        item {
            SectionTitle("Lencana")
        }
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                stats.badges.forEach { b ->
                    Card(
                        Modifier.width(120.dp).alpha(if (b.unlocked) 1f else 0.35f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(b.emoji, style = MaterialTheme.typography.headlineMedium)
                            Text(b.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                            Text(b.desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
