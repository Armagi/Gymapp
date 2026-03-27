package nl.gymlog.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import nl.gymlog.data.WorkoutSession
import nl.gymlog.ui.theme.Background
import nl.gymlog.ui.theme.Border
import nl.gymlog.ui.theme.SurfaceCard
import nl.gymlog.ui.theme.TextMuted
import nl.gymlog.ui.theme.TextPrimary
import nl.gymlog.ui.theme.TextSecondary
import nl.gymlog.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailScreen(
    initialMetricKey: String,
    onBack: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val initialPage = ALL_METRICS.indexOfFirst { it.key == initialMetricKey }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage) { ALL_METRICS.size }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 8.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Terug", tint = TextPrimary)
            }
            Text(
                text = ALL_METRICS[pagerState.currentPage].displayName,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            MetricDetailPage(metric = ALL_METRICS[page], sessions = sessions)
        }
    }
}

@Composable
private fun MetricDetailPage(metric: MetricDef, sessions: List<WorkoutSession>) {
    val accentColor = Color(metric.colorHex)
    val values = sessions.mapNotNull { metric.getValue(it) }
    val latestValue = values.lastOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (latestValue != null) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = metric.formatValue(latestValue),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                if (metric.unit.isNotEmpty()) {
                    Text(
                        text = " ${metric.unit}",
                        fontSize = 20.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            val lastSession = sessions.lastOrNull { metric.getValue(it) != null }
            if (lastSession != null) {
                Text(
                    text = SimpleDateFormat("d MMM yyyy", Locale("nl")).format(Date(lastSession.date)),
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (values.size >= 2) {
            MetricLineChart(values = values, accentColor = accentColor)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Minimaal 2 sessies nodig voor een grafiek.",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (values.isNotEmpty()) {
            StatsRow(values = values, unit = metric.unit, accentColor = accentColor)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Swipe links/rechts voor andere statistieken",
            fontSize = 11.sp,
            color = TextMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun MetricLineChart(values: List<Float>, accentColor: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(SurfaceCard, RoundedCornerShape(16.dp))
            .padding(8.dp)
    ) {
        val minVal = values.min()
        val maxVal = values.max()
        val range = (maxVal - minVal).coerceAtLeast(0.001f)
        val stepX = size.width / (values.size - 1).toFloat()
        val padY = 8.dp.toPx()
        val availH = size.height - padY * 2

        fun yFor(v: Float) = padY + availH * (1f - (v - minVal) / range)

        // Horizontal gridlines
        repeat(4) { i ->
            val y = padY + availH * (i / 3f)
            drawLine(
                color = Color(0xFF2C2C2C),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Line path
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = yFor(v)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = accentColor,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Dot markers
        values.forEachIndexed { i, v ->
            drawCircle(
                color = accentColor,
                radius = 4.dp.toPx(),
                center = Offset(i * stepX, yFor(v))
            )
        }
    }
}

@Composable
private fun StatsRow(values: List<Float>, unit: String, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatChip("Min", "%.0f".format(values.min()), unit, TextSecondary)
        StatChip("Gem", "%.0f".format(values.average().toFloat()), unit, accentColor)
        StatChip("Max", "%.0f".format(values.max()), unit, TextSecondary)
    }
}

@Composable
private fun StatChip(label: String, value: String, unit: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = TextMuted)
        Text(
            text = if (unit.isNotEmpty()) "$value $unit" else value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}
