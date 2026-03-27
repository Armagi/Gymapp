package nl.gymlog.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import nl.gymlog.data.WorkoutDatabase
import nl.gymlog.data.WorkoutRepository

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
