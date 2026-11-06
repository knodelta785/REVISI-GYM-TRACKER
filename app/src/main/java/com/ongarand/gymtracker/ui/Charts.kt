package com.ongarand.gymtracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ongarand.gymtracker.logic.fmtKg

data class Series(
    val values: List<Float?>,
    val color: Color,
    val dots: Boolean = true,
    val line: Boolean = true
)

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun LineChart(
    series: List<Series>,
    xLabels: List<String>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 160.dp
) {
    val grid = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val all = series.flatMap { it.values.filterNotNull() }
    if (all.isEmpty()) {
        EmptyHint("Belum ada data", modifier.height(chartHeight))
        return
    }
    val rawMin = all.min()
    val rawMax = all.max()
    val pad = if (rawMax - rawMin < 0.5f) 1f else (rawMax - rawMin) * 0.12f
    val lo = rawMin - pad
    val hi = rawMax + pad

    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(chartHeight)) {
            val w = size.width
            val h = size.height
            val n = xLabels.size.coerceAtLeast(1)
            fun px(i: Int): Float = if (n == 1) w / 2f else w * i / (n - 1)
            fun py(v: Float): Float = h - (v - lo) / (hi - lo) * h

            for (g in 0..3) {
                val y = h * g / 3f
                drawLine(grid, Offset(0f, y), Offset(w, y), 1.dp.toPx())
            }
            series.forEach { s ->
                val path = Path()
                var started = false
                s.values.forEachIndexed { i, v ->
                    if (v != null) {
                        if (!started) {
                            path.moveTo(px(i), py(v))
                            started = true
                        } else {
                            path.lineTo(px(i), py(v))
                        }
                    }
                }
                if (s.line && started) {
                    drawPath(path, s.color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                if (s.dots) {
                    s.values.forEachIndexed { i, v ->
                        if (v != null) drawCircle(s.color, 3.5.dp.toPx(), Offset(px(i), py(v)))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(xLabels.firstOrNull() ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${fmtKg(rawMin.toDouble())} – ${fmtKg(rawMax.toDouble())}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(xLabels.lastOrNull() ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun BarChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 140.dp
) {
    val bar = MaterialTheme.colorScheme.primary
    val dim = bar.copy(alpha = 0.45f)
    val maxV = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(chartHeight)) {
            val n = values.size.coerceAtLeast(1)
            val slot = size.width / n
            val bw = slot * 0.6f
            values.forEachIndexed { i, v ->
                val bh = (v / maxV * size.height).coerceAtLeast(2f)
                drawRoundRect(
                    color = if (i == values.lastIndex) bar else dim,
                    topLeft = Offset(i * slot + (slot - bw) / 2f, size.height - bh),
                    size = Size(bw, bh),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            labels.forEach {
                Text(
                    it,
                    Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ProgressRing(progress: Float, label: String, sub: String, diameter: Dp = 120.dp) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val color = MaterialTheme.colorScheme.primary
    Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val arc = Size(size.width - stroke, size.height - stroke)
            val tl = Offset(stroke / 2f, stroke / 2f)
            drawArc(track, 0f, 360f, false, topLeft = tl, size = arc, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(
                color, -90f, 360f * progress.coerceIn(0f, 1f), false,
                topLeft = tl, size = arc, style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun HBar(label: String, value: Float, max: Float, valueText: String, labelWidth: Dp = 84.dp) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, Modifier.width(labelWidth), style = MaterialTheme.typography.bodySmall, maxLines = 1)
        Box(
            Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val frac = if (max <= 0f) 0f else (value / max).coerceIn(0f, 1f)
            Box(
                Modifier
                    .fillMaxWidth(frac)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Text(valueText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
