package nl.gymlog.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import nl.gymlog.data.WorkoutDatabase
import nl.gymlog.data.WorkoutRepository
import nl.gymlog.data.WorkoutSession

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WorkoutRepository(
        WorkoutDatabase.getDatabase(application).workoutDao()
    )

    val sessions = repository.allSessions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val sessionCount = repository.sessionCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 0
    )
}

// ── Metric descriptor ──────────────────────────────────────────────────────────

data class MetricInfo(
    val key: String,
    val displayName: String,
    val unit: String,
    val colorHex: Long,
    val getValue: (WorkoutSession) -> Float?,
    val formatValue: (Float) -> String = { "%.0f".format(it) }
)
