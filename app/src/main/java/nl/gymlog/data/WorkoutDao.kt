package nl.gymlog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession): Long

    @Query("SELECT * FROM workout_sessions ORDER BY date ASC")
    fun getAllSessionsFlow(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions ORDER BY date DESC LIMIT 1")
    suspend fun getLatestSession(): WorkoutSession?

    @Query("SELECT COUNT(*) FROM workout_sessions")
    fun getSessionCountFlow(): Flow<Int>
}
