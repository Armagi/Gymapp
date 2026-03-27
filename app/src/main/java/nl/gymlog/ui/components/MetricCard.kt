package nl.gymlog.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import nl.gymlog.ui.theme.Border
import nl.gymlog.ui.theme.SurfaceCard
import nl.gymlog.ui.theme.TextMuted
import nl.gymlog.ui.theme.TextPrimary
import nl.gymlog.ui.theme.TextSecondary

@Composable
fun MetricCard(
    metricName: String,
    latestValue: String?,
    unit: String,
    accentColor: Color,
    trend: Trend,
    sparklineValues: List<Float>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row: name + trend arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = metricName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondary
                )
                TrendArrow(trend = trend)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Value row
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = latestValue ?: "—",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (latestValue != null) TextPrimary else TextMuted
                )
                if (unit.isNotEmpty() && latestValue != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }

            // Sparkline
            if (sparklineValues.size >= 2) {
                Spacer(modifier = Modifier.height(12.dp))
                Sparkline(
                    values = sparklineValues,
                    color = accentColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                )
            }
        }
    }
}

@Composable
fun Sparkline(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas

        val minVal = values.min()
        val maxVal = values.max()
        val range = (maxVal - minVal).coerceAtLeast(0.001f)

        val stepX = size.width / (values.size - 1).toFloat()
        val paddingY = 4.dp.toPx()
        val availableHeight = size.height - paddingY * 2

        fun yFor(v: Float): Float = paddingY + availableHeight * (1f - (v - minVal) / range)

        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = yFor(v)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Dot markers
        values.forEachIndexed { i, v ->
            drawCircle(
                color = color,
                radius = 3.dp.toPx(),
                center = Offset(i * stepX, yFor(v))
            )
        }
    }
}
