package com.ongarand.gymtracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.ongarand.gymtracker.data.BodyEntry
import com.ongarand.gymtracker.logic.fmt1
import com.ongarand.gymtracker.logic.fmtKg
import com.ongarand.gymtracker.logic.movingAvg
import com.ongarand.gymtracker.logic.parseD
import com.ongarand.gymtracker.logic.short
import com.ongarand.gymtracker.logic.targets
import com.ongarand.gymtracker.logic.trendText
import com.ongarand.gymtracker.logic.weeklyChange
import java.time.LocalDate

private val dec = KeyboardOptions(keyboardType = KeyboardType.Decimal)
private val num = KeyboardOptions(keyboardType = KeyboardType.Number)

@Composable
fun BodyScreen(vm: AppViewModel) {
    val body by vm.body.collectAsStateWithLifecycle()
    val nutrition by vm.nutrition.collectAsStateWithLifecycle()
    val prefs by vm.prefs.state.collectAsStateWithLifecycle()

    val todayEpoch = LocalDate.now().toEpochDay()
    val todayEntry = body.find { it.dayEpoch == todayEpoch }
    val todayNut = nutrition.find { it.dayEpoch == todayEpoch }

    val ma = remember(body) { movingAvg(body) }
    val delta = remember(body) { weeklyChange(body) }
    val shown = body.takeLast(30)
    val shownMa = ma.takeLast(30)
    val latestW = body.lastOrNull()?.weightKg
    val tg = latestW?.let { targets(prefs, it) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenTitle("Tubuh & Nutrisi") }

        item {
            AppCard {
                SectionTitle("Catat hari ini")
                var w by remember(todayEntry?.id) { mutableStateOf(todayEntry?.weightKg?.let { fmtKg(it) } ?: "") }
                var waist by remember(todayEntry?.id) { mutableStateOf(todayEntry?.waistCm?.let { fmtKg(it) } ?: "") }
                var chest by remember(todayEntry?.id) { mutableStateOf(todayEntry?.chestCm?.let { fmtKg(it) } ?: "") }
                var arm by remember(todayEntry?.id) { mutableStateOf(todayEntry?.armCm?.let { fmtKg(it) } ?: "") }
                var thigh by remember(todayEntry?.id) { mutableStateOf(todayEntry?.thighCm?.let { fmtKg(it) } ?: "") }
                OutlinedTextField(
                    w, { w = it }, label = { Text("Berat badan (kg)") }, singleLine = true,
                    keyboardOptions = dec, modifier = Modifier.fillMaxWidth()
                )
                Muted("Ukuran tubuh (cm, opsional)")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(waist, { waist = it }, label = { Text("Pinggang") }, singleLine = true, keyboardOptions = dec, modifier = Modifier.weight(1f))
                    OutlinedTextField(chest, { chest = it }, label = { Text("Dada") }, singleLine = true, keyboardOptions = dec, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(arm, { arm = it }, label = { Text("Lengan") }, singleLine = true, keyboardOptions = dec, modifier = Modifier.weight(1f))
                    OutlinedTextField(thigh, { thigh = it }, label = { Text("Paha") }, singleLine = true, keyboardOptions = dec, modifier = Modifier.weight(1f))
                }
                Button(
                    onClick = {
                        val wd = parseD(w)
                        if (wd != null && wd > 0) vm.saveBody(wd, parseD(waist), parseD(chest), parseD(arm), parseD(thigh))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Simpan") }
            }
        }

        item {
            AppCard {
                SectionTitle("Tren berat badan")
                LineChart(
                    series = listOf(
                        Series(shown.map { it.weightKg.toFloat() }, MaterialTheme.colorScheme.onSurfaceVariant, dots = true, line = false),
                        Series(shownMa.map { it.toFloat() }, MaterialTheme.colorScheme.primary, dots = false, line = true)
                    ),
                    xLabels = shown.map { LocalDate.ofEpochDay(it.dayEpoch).short() }
                )
                Muted("Titik = berat harian, garis oranye = rata-rata 7 hari (meredam fluktuasi air).")
                Text(trendText(prefs.goal, delta))
            }
        }

        item {
            AppCard {
                SectionTitle("Asupan hari ini")
                if (tg == null) {
                    Muted("Catat berat badan dulu agar target kalori & protein bisa dihitung.")
                } else {
                    var kcal by remember(todayNut?.dayEpoch) { mutableStateOf(todayNut?.kcal?.toString() ?: "") }
                    var prot by remember(todayNut?.dayEpoch) { mutableStateOf(todayNut?.proteinG?.toString() ?: "") }
                    val kc = kcal.toIntOrNull() ?: 0
                    val pr = prot.toIntOrNull() ?: 0
                    HBar("Kalori", kc.toFloat(), tg.kcal.toFloat(), "$kc / ${tg.kcal}")
                    HBar("Protein", pr.toFloat(), tg.protein.toFloat(), "$pr / ${tg.protein} g")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(kcal, { kcal = it }, label = { Text("kkal") }, singleLine = true, keyboardOptions = num, modifier = Modifier.weight(1f))
                        OutlinedTextField(prot, { prot = it }, label = { Text("protein (g)") }, singleLine = true, keyboardOptions = num, modifier = Modifier.weight(1f))
                        Button(onClick = {
                            val k = kcal.toIntOrNull()
                            val p = prot.toIntOrNull()
                            if (k != null && p != null) vm.saveNutrition(k, p)
                        }) { Text("Simpan") }
                    }
                    Muted(
                        "Target sesuai profil & tujuan di Pengaturan (estimasi Mifflin-St Jeor, protein " +
                            "${if (prefs.goal == "cut") "2,0" else "1,8"} g/kg). Hanya perkiraan, bukan saran medis."
                    )
                }
            }
        }

        item {
            AppCard {
                SectionTitle("Ukuran tubuh")
                fun first(sel: (BodyEntry) -> Double?): Double? = body.firstNotNullOfOrNull(sel)
                fun last(sel: (BodyEntry) -> Double?): Double? = body.asReversed().firstNotNullOfOrNull(sel)
                listOf<Pair<String, (BodyEntry) -> Double?>>(
                    "Pinggang" to { b: BodyEntry -> b.waistCm },
                    "Dada" to { b: BodyEntry -> b.chestCm },
                    "Lengan" to { b: BodyEntry -> b.armCm },
                    "Paha" to { b: BodyEntry -> b.thighCm }
                ).forEach { (label, sel) ->
                    val a = first(sel)
                    val b = last(sel)
                    if (a != null && b != null) {
                        val d = b - a
                        Text("$label: ${fmt1(b)} cm  (${if (d >= 0) "+" else ""}${fmt1(d)} sejak awal)")
                    }
                }
                if (body.none { it.waistCm != null || it.chestCm != null || it.armCm != null || it.thighCm != null }) {
                    Muted("Belum ada data ukuran. Tren lingkar lengan & pinggang lebih jujur daripada angka timbangan saat membangun otot.")
                }
            }
        }

        item { SectionTitle("Riwayat berat") }
        items(body.asReversed().take(14), key = { it.id }) { b ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(LocalDate.ofEpochDay(b.dayEpoch).short(), Modifier.weight(1f))
                Text("${fmtKg(b.weightKg)} kg", fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { vm.deleteBody(b.id) }) {
                    Icon(Icons.Default.Delete, "Hapus", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
