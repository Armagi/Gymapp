package nl.gymlog.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long, // epoch millis
    val calories: Float?,
    val durationSeconds: Int?,       // stored as total seconds, display as MM:SS
    val distanceKm: Float?,
    val avgPowerWatt: Float?,
    val avgSpeedSpm: Float?,
    val avgHeartRate: Float?,
    val maxHeartRate: Float?,
    val caloriesPerHour: Float?,
    val conditionPI: Float?,
    val moves: Float?,
    val needsReview: Boolean = false  // true if OCR had null fields
)
