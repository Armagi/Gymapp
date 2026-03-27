package nl.gymlog.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.compose.component.shape.shader.fromBrush
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.component.shape.shader.DynamicShaders
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import nl.gymlog.data.WorkoutSession
import nl.gymlog.ocr.OcrParser
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
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 8.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Terug",
                    tint = TextPrimary
                )
            }
            val currentMetric = ALL_METRICS[pagerState.currentPage]
            Text(
                text = currentMetric.displayName,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        // Swipeable pages per metric
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val metric = ALL_METRICS[page]
            MetricDetailPage(
                metric = metric,
                sessions = sessions
            )
        }
    }
}

@Composable
private fun MetricDetailPage(
    metric: MetricDef,
    sessions: List<WorkoutSession>
) {
    val accentColor = Color(metric.colorHex)
    val values = sessions.mapNotNull { metric.getValue(it) }
    val latestValue = values.lastOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Latest value display
        if (latestValue != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Start
            ) {
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
            if (sessions.isNotEmpty()) {
                val lastSession = sessions.lastOrNull { metric.getValue(it) != null }
                if (lastSession != null) {
                    val dateFormat = SimpleDateFormat("d MMM yyyy", Locale("nl"))
                    Text(
                        text = dateFormat.format(Date(lastSession.date)),
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (values.size >= 2) {
            MetricLineChart(
                values = values,
                sessions = sessions,
                metric = metric,
                accentColor = accentColor
            )
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

        // Stats row
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
private fun MetricLineChart(
    values: List<Float>,
    sessions: List<WorkoutSession>,
    metric: MetricDef,
    accentColor: Color
) {
    val producer = remember(values) {
        ChartEntryModelProducer(
            values.mapIndexed { idx, v -> entryOf(idx.toFloat(), v) }
        )
    }

    Chart(
        chart = lineChart(
            lines = listOf(
                lineSpec(
                    lineColor = accentColor,
                    lineThicknessDp = 2f,
                    point = null,
                    lineBackgroundShader = DynamicShaders.fromBrush(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.2f),
                                accentColor.copy(alpha = 0f)
                            )
                        )
                    )
                )
            )
        ),
        chartModelProducer = producer,
        startAxis = rememberStartAxis(
            label = com.patrykandpatrick.vico.compose.component.rememberTextComponent(
                color = TextSecondary,
                textSize = 11.sp
            ),
            guideline = com.patrykandpatrick.vico.compose.component.rememberLineComponent(
                color = Border
            )
        ),
        bottomAxis = rememberBottomAxis(
            label = com.patrykandpatrick.vico.compose.component.rememberTextComponent(
                color = TextSecondary,
                textSize = 11.sp
            ),
            guideline = null
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(SurfaceCard, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .padding(8.dp)
    )
}

@Composable
private fun StatsRow(values: List<Float>, unit: String, accentColor: Color) {
    val min = values.min()
    val max = values.max()
    val avg = values.average().toFloat()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatChip(label = "Min", value = "%.0f".format(min), unit = unit, color = TextSecondary)
        StatChip(label = "Gem", value = "%.0f".format(avg), unit = unit, color = accentColor)
        StatChip(label = "Max", value = "%.0f".format(max), unit = unit, color = TextSecondary)
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
