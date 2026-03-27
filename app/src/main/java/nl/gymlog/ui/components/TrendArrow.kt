package nl.gymlog.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

enum class Trend { UP, DOWN, FLAT }

fun computeTrend(current: Float?, previous: Float?): Trend {
    if (current == null || previous == null) return Trend.FLAT
    return when {
        current > previous * 1.005f -> Trend.UP
        current < previous * 0.995f -> Trend.DOWN
        else -> Trend.FLAT
    }
}

@Composable
fun TrendArrow(
    trend: Trend,
    modifier: Modifier = Modifier,
    positiveIsUp: Boolean = true  // for HR, higher isn't necessarily "good"
) {
    val (icon, color) = when (trend) {
        Trend.UP -> Pair(Icons.Filled.ArrowDropUp, if (positiveIsUp) Color(0xFF7BED9F) else Color(0xFFFF4757))
        Trend.DOWN -> Pair(Icons.Filled.ArrowDropDown, if (positiveIsUp) Color(0xFFFF4757) else Color(0xFF7BED9F))
        Trend.FLAT -> Pair(Icons.Filled.Remove, Color(0xFF8A8A8A))
    }
    Icon(
        imageVector = icon,
        contentDescription = trend.name,
        tint = color,
        modifier = modifier
    )
}
