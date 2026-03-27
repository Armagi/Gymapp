package nl.gymlog.ui.screens

import android.app.DatePickerDialog
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.gymlog.ocr.OcrParser
import nl.gymlog.ocr.ParsedWorkout
import nl.gymlog.ui.theme.AmberWarning
import nl.gymlog.ui.theme.Background
import nl.gymlog.ui.theme.Border
import nl.gymlog.ui.theme.SurfaceCard
import nl.gymlog.ui.theme.TextPrimary
import nl.gymlog.ui.theme.TextSecondary
import nl.gymlog.viewmodel.CaptureUiState
import nl.gymlog.viewmodel.CaptureViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ReviewScreen(
    viewModel: CaptureViewModel,
    onSaved: () -> Unit,
    onRetake: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is CaptureUiState.Saved) {
            viewModel.reset()
            onSaved()
        }
    }

    when (val state = uiState) {
        is CaptureUiState.Parsed -> {
            ReviewContent(
                bitmap = state.bitmap,
                parsed = state.parsed,
                onSave = { date, cal, dur, dist, pwr, spd, hr, maxHr, cph, cond, moves, review ->
                    viewModel.saveSession(date, cal, dur, dist, pwr, spd, hr, maxHr, cph, cond, moves, review)
                },
                onRetake = {
                    viewModel.reset()
                    onRetake()
                }
            )
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Verwerken...", color = TextPrimary, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun ReviewContent(
    bitmap: Bitmap,
    parsed: ParsedWorkout,
    onSave: (Long, Float?, Int?, Float?, Float?, Float?, Float?, Float?, Float?, Float?, Float?, Boolean) -> Unit,
    onRetake: () -> Unit
) {
    val context = LocalContext.current

    var sessionDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var calories by remember { mutableStateOf(parsed.calories?.let { "%.0f".format(it) } ?: "") }
    var duration by remember { mutableStateOf(parsed.durationSeconds?.let { OcrParser.formatDuration(it) } ?: "") }
    var distance by remember { mutableStateOf(parsed.distanceKm?.let { "%.2f".format(it) } ?: "") }
    var avgPower by remember { mutableStateOf(parsed.avgPowerWatt?.let { "%.0f".format(it) } ?: "") }
    var avgSpeed by remember { mutableStateOf(parsed.avgSpeedSpm?.let { "%.0f".format(it) } ?: "") }
    var avgHr by remember { mutableStateOf(parsed.avgHeartRate?.let { "%.0f".format(it) } ?: "") }
    var maxHr by remember { mutableStateOf(parsed.maxHeartRate?.let { "%.0f".format(it) } ?: "") }
    var calPerHour by remember { mutableStateOf(parsed.caloriesPerHour?.let { "%.0f".format(it) } ?: "") }
    var condition by remember { mutableStateOf(parsed.conditionPI?.let { "%.0f".format(it) } ?: "") }
    var moves by remember { mutableStateOf(parsed.moves?.let { "%.0f".format(it) } ?: "") }

    val datePicker = DatePickerDialog(
        context,
        { _, year, month, day ->
            sessionDate = Calendar.getInstance().apply { set(year, month, day) }.timeInMillis
        },
        Calendar.getInstance().get(Calendar.YEAR),
        Calendar.getInstance().get(Calendar.MONTH),
        Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Captured image",
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentScale = ContentScale.Crop
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Controleer je sessie",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Date picker
            Text(text = "Datum", fontSize = 13.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = SimpleDateFormat("d MMMM yyyy", Locale("nl")).format(Date(sessionDate)),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceCard, RoundedCornerShape(8.dp))
                    .border(1.dp, Border, RoundedCornerShape(8.dp))
                    .clickable { datePicker.show() }
                    .padding(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            ReviewField("Calorieën (kcal)", calories, { calories = it }, parsed.calories == null)
            ReviewField("Duur (MM:SS)", duration, { duration = it }, parsed.durationSeconds == null, isText = true)
            ReviewField("Afstand (km)", distance, { distance = it }, parsed.distanceKm == null)
            ReviewField("Gem. vermogen (watt)", avgPower, { avgPower = it }, parsed.avgPowerWatt == null)
            ReviewField("Gem. snelheid (spm)", avgSpeed, { avgSpeed = it }, parsed.avgSpeedSpm == null)
            ReviewField("Gem. hartslag (spm)", avgHr, { avgHr = it }, parsed.avgHeartRate == null)
            ReviewField("Max. hartslag (spm)", maxHr, { maxHr = it }, parsed.maxHeartRate == null)
            ReviewField("Kcal per uur", calPerHour, { calPerHour = it }, parsed.caloriesPerHour == null)
            ReviewField("Conditie (PI)", condition, { condition = it }, parsed.conditionPI == null)
            ReviewField("MOVEs", moves, { moves = it }, parsed.moves == null)

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val durSeconds = parseDurationInput(duration)
                    val calVal = calories.replace(',', '.').toFloatOrNull()
                    val distVal = distance.replace(',', '.').toFloatOrNull()
                    val pwrVal = avgPower.replace(',', '.').toFloatOrNull()
                    val spdVal = avgSpeed.replace(',', '.').toFloatOrNull()
                    val hrVal = avgHr.replace(',', '.').toFloatOrNull()
                    val maxHrVal = maxHr.replace(',', '.').toFloatOrNull()
                    val cphVal = calPerHour.replace(',', '.').toFloatOrNull()
                    val condVal = condition.replace(',', '.').toFloatOrNull()
                    val movesVal = moves.replace(',', '.').toFloatOrNull()
                    val review = listOf(calVal, distVal, pwrVal, spdVal, hrVal,
                        maxHrVal, cphVal, condVal, movesVal).any { it == null } || durSeconds == null
                    onSave(sessionDate, calVal, durSeconds, distVal, pwrVal, spdVal,
                        hrVal, maxHrVal, cphVal, condVal, movesVal, review)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B35))
            ) {
                Text("Sessie opslaan", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Text("Opnieuw fotograferen", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ReviewField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    needsReview: Boolean,
    isText: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = if (needsReview) "$label ⚠" else label,
                color = if (needsReview) AmberWarning else TextSecondary,
                fontSize = 12.sp
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        keyboardOptions = if (isText) KeyboardOptions.Default
        else KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (needsReview) AmberWarning else Color(0xFFFF6B35),
            unfocusedBorderColor = if (needsReview) AmberWarning.copy(alpha = 0.6f) else Border,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = Color(0xFFFF6B35),
            focusedContainerColor = SurfaceCard,
            unfocusedContainerColor = SurfaceCard
        ),
        supportingText = if (needsReview) {
            { Text("Controleer deze waarde", color = AmberWarning, fontSize = 11.sp) }
        } else null
    )
}

private fun parseDurationInput(input: String): Int? {
    val colonMatch = Regex("""^(\d+):(\d{2})$""").find(input.trim())
    if (colonMatch != null) {
        val minutes = colonMatch.groupValues[1].toIntOrNull() ?: return null
        val seconds = colonMatch.groupValues[2].toIntOrNull() ?: return null
        return minutes * 60 + seconds
    }
    return input.trim().toIntOrNull()?.times(60)
}
