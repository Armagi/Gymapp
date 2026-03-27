package nl.gymlog.data

import kotlinx.coroutines.flow.Flow

class WorkoutRepository(private val dao: WorkoutDao) {

    val allSessions: Flow<List<WorkoutSession>> = dao.getAllSessionsFlow()
    val sessionCount: Flow<Int> = dao.getSessionCountFlow()

    suspend fun insertSession(session: WorkoutSession): Long = dao.insertSession(session)

    suspend fun getLatestSession(): WorkoutSession? = dao.getLatestSession()
}
