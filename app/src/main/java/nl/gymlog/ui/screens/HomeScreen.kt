package nl.gymlog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import nl.gymlog.data.WorkoutSession
import nl.gymlog.ocr.OcrParser
import nl.gymlog.ui.components.MetricCard
import nl.gymlog.ui.components.Trend
import nl.gymlog.ui.components.computeTrend
import nl.gymlog.ui.theme.Background
import nl.gymlog.ui.theme.CaloriesColor
import nl.gymlog.ui.theme.SurfaceCard
import nl.gymlog.ui.theme.TextMuted
import nl.gymlog.ui.theme.TextPrimary
import nl.gymlog.ui.theme.TextSecondary
import nl.gymlog.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToCapture: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val sessionCount by viewModel.sessionCount.collectAsState()

    Scaffold(
        containerColor = Background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCapture,
                containerColor = CaloriesColor,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = "Foto maken")
            }
        }
    ) { paddingValues ->
        if (sessions.isEmpty()) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            val latest = sessions.last()
            val previous = if (sessions.size >= 2) sessions[sessions.size - 2] else null

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Header
                item {
                    HomeHeader(latest = latest)
                }

                // Metric cards
                items(ALL_METRICS) { metric ->
                    val values = sessions.mapNotNull { metric.getValue(it) }
                    val latest10 = values.takeLast(10)
                    val currentVal = metric.getValue(latest)
                    val prevVal = previous?.let { metric.getValue(it) }
                    val trend = computeTrend(currentVal, prevVal)

                    MetricCard(
                        metricName = metric.displayName,
                        latestValue = currentVal?.let { metric.formatValue(it) },
                        unit = metric.unit,
                        accentColor = Color(metric.colorHex),
                        trend = trend,
                        sparklineValues = latest10,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        onClick = { onNavigateToDetail(metric.key) }
                    )
                }

                // Footer
                item {
                    Text(
                        text = "$sessionCount sessies gelogd",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center,
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(latest: WorkoutSession) {
    val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("nl"))
    val dateStr = dateFormat.format(Date(latest.date))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text(
            text = "GymLog",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Laatste sessie: $dateStr",
            fontSize = 13.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        HighlightRow(session = latest)
    }
}

@Composable
private fun HighlightRow(session: WorkoutSession) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HighlightChip(
            label = "Calorieën",
            value = session.calories?.let { "%.0f kcal".format(it) } ?: "—",
            color = Color(0xFFFF6B35),
            modifier = Modifier.weight(1f)
        )
        HighlightChip(
            label = "Gem. Hartslag",
            value = session.avgHeartRate?.let { "%.0f spm".format(it) } ?: "—",
            color = Color(0xFFFF4757),
            modifier = Modifier.weight(1f)
        )
        HighlightChip(
            label = "MOVEs",
            value = session.moves?.let { "%.0f".format(it) } ?: "—",
            color = Color(0xFFA29BFE),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HighlightChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 11.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "GymLog",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Nog geen sessies.",
                fontSize = 16.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Maak je eerste foto.",
                fontSize = 16.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Metric registry ───────────────────────────────────────────────────────────

data class MetricDef(
    val key: String,
    val displayName: String,
    val unit: String,
    val colorHex: Long,
    val getValue: (WorkoutSession) -> Float?,
    val formatValue: (Float) -> String = { "%.0f".format(it) }
)

val ALL_METRICS = listOf(
    MetricDef("calories", "Calorieën", "kcal", 0xFFFF6B35, { it.calories }),
    MetricDef("durationSeconds", "Duur", "", 0xFF4ECDC4, { it.durationSeconds?.toFloat() },
        { OcrParser.formatDuration(it.toInt()) }),
    MetricDef("distanceKm", "Afstand", "km", 0xFF45B7D1, { it.distanceKm },
        { "%.2f".format(it) }),
    MetricDef("avgPowerWatt", "Gem. vermogen", "watt", 0xFF96CEB4, { it.avgPowerWatt }),
    MetricDef("avgSpeedSpm", "Gem. snelheid", "spm", 0xFFFFEAA7, { it.avgSpeedSpm }),
    MetricDef("avgHeartRate", "Gem. hartslag", "spm", 0xFFFF4757, { it.avgHeartRate }),
    MetricDef("maxHeartRate", "Max. hartslag", "spm", 0xFFFF6B81, { it.maxHeartRate }),
    MetricDef("caloriesPerHour", "Kcal per uur", "kcal/h", 0xFFFFA502, { it.caloriesPerHour }),
    MetricDef("conditionPI", "Conditie (PI)", "PI", 0xFF7BED9F, { it.conditionPI }),
    MetricDef("moves", "MOVEs", "", 0xFFA29BFE, { it.moves }),
)
