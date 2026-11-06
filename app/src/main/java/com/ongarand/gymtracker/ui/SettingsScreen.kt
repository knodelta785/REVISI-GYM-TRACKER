package com.ongarand.gymtracker.ui

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ongarand.gymtracker.AppViewModel
import com.ongarand.gymtracker.logic.fmtKg

@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val p by vm.prefs.state.collectAsStateWithLifecycle()

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) vm.exportCsv(uri)
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") }
                ScreenTitle("Pengaturan")
            }
        }

        item {
            AppCard {
                SectionTitle("Latihan")
                Stepper("Target sesi / minggu", "${p.weeklyGoal}",
                    { vm.prefs.update { it.copy(weeklyGoal = (it.weeklyGoal - 1).coerceAtLeast(1)) } },
                    { vm.prefs.update { it.copy(weeklyGoal = (it.weeklyGoal + 1).coerceAtMost(7)) } })
                Stepper("Waktu istirahat (detik)", "${p.restSeconds}",
                    { vm.prefs.update { it.copy(restSeconds = (it.restSeconds - 15).coerceAtLeast(15)) } },
                    { vm.prefs.update { it.copy(restSeconds = (it.restSeconds + 15).coerceAtMost(300)) } })
                Stepper("Kenaikan beban (kg)", fmtKg(p.incrementKg),
                    { vm.prefs.update { it.copy(incrementKg = (it.incrementKg - 0.5).coerceAtLeast(0.5)) } },
                    { vm.prefs.update { it.copy(incrementKg = (it.incrementKg + 0.5).coerceAtMost(10.0)) } })
            }
        }

        item {
            AppCard {
                SectionTitle("Pengingat harian")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Aktifkan pengingat", Modifier.weight(1f))
                    Switch(checked = p.reminderOn, onCheckedChange = { on ->
                        if (on && Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        vm.setReminder(on, p.reminderHour, p.reminderMinute)
                    })
                }
                OutlinedButton(
                    onClick = {
                        TimePickerDialog(ctx, { _, h, m -> vm.setReminder(p.reminderOn, h, m) }, p.reminderHour, p.reminderMinute, true).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Jam: %02d:%02d".format(p.reminderHour, p.reminderMinute)) }
                Muted("Pengingat hanya muncul bila kamu belum mencatat latihan hari itu. Waktu tepatnya bisa bergeser beberapa menit (aturan hemat baterai Android).")
            }
        }

        item {
            AppCard {
                SectionTitle("Profil & tujuan")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = p.male, onClick = { vm.prefs.update { it.copy(male = true) } }, label = { Text("Pria") })
                    FilterChip(selected = !p.male, onClick = { vm.prefs.update { it.copy(male = false) } }, label = { Text("Wanita") })
                }
                Stepper("Usia", "${p.age}",
                    { vm.prefs.update { it.copy(age = (it.age - 1).coerceAtLeast(14)) } },
                    { vm.prefs.update { it.copy(age = (it.age + 1).coerceAtMost(90)) } })
                Stepper("Tinggi (cm)", "${p.heightCm}",
                    { vm.prefs.update { it.copy(heightCm = (it.heightCm - 1).coerceAtLeast(120)) } },
                    { vm.prefs.update { it.copy(heightCm = (it.heightCm + 1).coerceAtMost(230)) } })
                Muted("Tingkat aktivitas")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1.2 to "Ringan", 1.375 to "Sedang", 1.55 to "Aktif", 1.725 to "Sangat").forEach { (v, label) ->
                        FilterChip(
                            selected = p.activity == v,
                            onClick = { vm.prefs.update { it.copy(activity = v) } },
                            label = { Text(label) }
                        )
                    }
                }
                Muted("Tujuan")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("cut" to "Cutting", "maintain" to "Maintain", "bulk" to "Bulking").forEach { (v, label) ->
                        FilterChip(
                            selected = p.goal == v,
                            onClick = { vm.prefs.update { it.copy(goal = v) } },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        item {
            AppCard {
                SectionTitle("Video tutorial")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Salin video ke penyimpanan aplikasi", Modifier.weight(1f))
                    Switch(checked = p.copyVideos, onCheckedChange = { c -> vm.prefs.update { it.copy(copyVideos = c) } })
                }
                Muted(
                    "Aktif (disarankan): video tetap aman walau file aslinya dihapus, tetapi memakai ruang penyimpanan. " +
                        "Nonaktif: hanya menyimpan referensi file; video gagal diputar jika file dipindah. Berlaku untuk video yang diimpor berikutnya."
                )
            }
        }

        item {
            AppCard {
                SectionTitle("Data")
                OutlinedButton(onClick = { exporter.launch("ongarand-gym-data.csv") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Ekspor riwayat ke CSV")
                }
                Muted("Semua data disimpan lokal di perangkat ini, tanpa akun dan tanpa internet.")
            }
        }
    }
}
