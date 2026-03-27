package nl.gymlog.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.gymlog.data.WorkoutDatabase
import nl.gymlog.data.WorkoutRepository
import nl.gymlog.data.WorkoutSession
import nl.gymlog.ocr.OcrParser
import nl.gymlog.ocr.ParsedWorkout

sealed class CaptureUiState {
    object Idle : CaptureUiState()
    object Parsing : CaptureUiState()
    data class Parsed(val parsed: ParsedWorkout, val bitmap: Bitmap) : CaptureUiState()
    data class Error(val message: String) : CaptureUiState()
    object Saved : CaptureUiState()
}

class CaptureViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WorkoutRepository(
        WorkoutDatabase.getDatabase(application).workoutDao()
    )

    private val _uiState = MutableStateFlow<CaptureUiState>(CaptureUiState.Idle)
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    fun processCapture(bitmap: Bitmap) {
        _uiState.value = CaptureUiState.Parsing
        viewModelScope.launch {
            try {
                val parsed = OcrParser.parseFromBitmap(bitmap)
                _uiState.value = CaptureUiState.Parsed(parsed, bitmap)
            } catch (e: Exception) {
                _uiState.value = CaptureUiState.Error(e.message ?: "OCR mislukt")
            }
        }
    }

    fun saveSession(
        date: Long,
        calories: Float?,
        durationSeconds: Int?,
        distanceKm: Float?,
        avgPowerWatt: Float?,
        avgSpeedSpm: Float?,
        avgHeartRate: Float?,
        maxHeartRate: Float?,
        caloriesPerHour: Float?,
        conditionPI: Float?,
        moves: Float?,
        needsReview: Boolean
    ) {
        viewModelScope.launch {
            val session = WorkoutSession(
                date = date,
                calories = calories,
                durationSeconds = durationSeconds,
                distanceKm = distanceKm,
                avgPowerWatt = avgPowerWatt,
                avgSpeedSpm = avgSpeedSpm,
                avgHeartRate = avgHeartRate,
                maxHeartRate = maxHeartRate,
                caloriesPerHour = caloriesPerHour,
                conditionPI = conditionPI,
                moves = moves,
                needsReview = needsReview
            )
            repository.insertSession(session)
            _uiState.value = CaptureUiState.Saved
        }
    }

    fun reset() {
        _uiState.value = CaptureUiState.Idle
    }
}
