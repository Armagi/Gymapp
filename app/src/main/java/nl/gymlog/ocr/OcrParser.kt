package nl.gymlog.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs

data class ParsedWorkout(
    val calories: Float?,
    val durationSeconds: Int?,
    val distanceKm: Float?,
    val avgPowerWatt: Float?,
    val avgSpeedSpm: Float?,
    val avgHeartRate: Float?,
    val maxHeartRate: Float?,
    val caloriesPerHour: Float?,
    val conditionPI: Float?,
    val moves: Float?,
    val needsReview: Boolean
)

object OcrParser {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Dutch labels as they appear on the Technogym cooldown screen
    private val LABEL_CALORIES = listOf("verbruikte calorie", "verbruikte calorieën", "calorieën")
    private val LABEL_DURATION = listOf("duur van de oefening", "duur")
    private val LABEL_DISTANCE = listOf("afgelegde afstand", "afstand")
    private val LABEL_AVG_POWER = listOf("gemiddeld vermogen")
    private val LABEL_AVG_SPEED = listOf("gemiddelde snelheid")
    private val LABEL_AVG_HR = listOf("gemiddelde hartfrequentie", "gem. hartfrequentie")
    private val LABEL_MAX_HR = listOf("maximale hartfrequentie", "max. hartfrequentie")
    private val LABEL_CALORIES_PER_HOUR = listOf("calorieën per uur", "calorieen per uur")
    private val LABEL_CONDITION = listOf("conditie")
    private val LABEL_MOVES = listOf("moves")

    // Y-axis tolerance in pixels for same-line detection
    private const val Y_TOLERANCE = 30f

    suspend fun parseFromBitmap(bitmap: Bitmap): ParsedWorkout {
        val image = InputImage.fromBitmap(bitmap, 0)
        val visionText = recognizeText(image)
        return parseVisionText(visionText)
    }

    private suspend fun recognizeText(image: InputImage): Text = suspendCoroutine { cont ->
        recognizer.process(image)
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    fun parseVisionText(visionText: Text): ParsedWorkout {
        // Build a list of (centerY, centerX, text) for each line in the OCR result
        data class TextSegment(val centerY: Float, val centerX: Float, val text: String)

        val segments = mutableListOf<TextSegment>()
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox ?: continue
                segments.add(
                    TextSegment(
                        centerY = box.centerY().toFloat(),
                        centerX = box.centerX().toFloat(),
                        text = line.text.trim()
                    )
                )
            }
        }

        // Group segments into logical rows by Y proximity
        data class Row(val avgY: Float, val segments: MutableList<TextSegment> = mutableListOf())

        val rows = mutableListOf<Row>()
        for (seg in segments.sortedBy { it.centerY }) {
            val existingRow = rows.firstOrNull { abs(it.avgY - seg.centerY) <= Y_TOLERANCE }
            if (existingRow != null) {
                existingRow.segments.add(seg)
            } else {
                rows.add(Row(avgY = seg.centerY, segments = mutableListOf(seg)))
            }
        }

        // For each row sort segments left to right
        rows.forEach { row -> row.segments.sortBy { it.centerX } }

        // Helper: find value on same row as label
        fun findValueForLabel(labels: List<String>): String? {
            val fullText = visionText.text.lowercase()
            for (label in labels) {
                if (!fullText.contains(label)) continue
                // Find the row that contains this label
                val targetRow = rows.firstOrNull { row ->
                    row.segments.any { seg -> seg.text.lowercase().contains(label) }
                } ?: continue
                // Value is in the rightmost segment of the row that doesn't match the label
                val valueSegment = targetRow.segments
                    .filter { seg -> !seg.text.lowercase().contains(label) }
                    .maxByOrNull { it.centerX }
                if (valueSegment != null) return valueSegment.text
                // If the value is in the same text segment after the label text, extract it
                val labelSegment = targetRow.segments.firstOrNull { seg ->
                    seg.text.lowercase().contains(label)
                } ?: continue
                val afterLabel = labelSegment.text.substringAfterLast(
                    labelSegment.text.take((label.length + 1).coerceAtMost(labelSegment.text.length)),
                    ""
                ).trim()
                if (afterLabel.isNotEmpty()) return afterLabel
            }
            return null
        }

        // Extract first numeric value from a string
        fun extractFloat(text: String?): Float? {
            if (text == null) return null
            val regex = Regex("""(\d+[.,]?\d*)""")
            return regex.find(text)?.groupValues?.get(1)
                ?.replace(',', '.')
                ?.toFloatOrNull()
        }

        // Parse duration MM:SS → total seconds
        fun parseDuration(text: String?): Int? {
            if (text == null) return null
            val colonRegex = Regex("""(\d+):(\d{2})""")
            val match = colonRegex.find(text)
            if (match != null) {
                val minutes = match.groupValues[1].toIntOrNull() ?: return null
                val seconds = match.groupValues[2].toIntOrNull() ?: return null
                return minutes * 60 + seconds
            }
            return extractFloat(text)?.toInt()?.times(60) // fallback: treat as minutes
        }

        val calories = extractFloat(findValueForLabel(LABEL_CALORIES))
        val durationSeconds = parseDuration(findValueForLabel(LABEL_DURATION))
        val distanceKm = extractFloat(findValueForLabel(LABEL_DISTANCE))
        val avgPowerWatt = extractFloat(findValueForLabel(LABEL_AVG_POWER))
        val avgSpeedSpm = extractFloat(findValueForLabel(LABEL_AVG_SPEED))
        val avgHeartRate = extractFloat(findValueForLabel(LABEL_AVG_HR))
        val maxHeartRate = extractFloat(findValueForLabel(LABEL_MAX_HR))
        val caloriesPerHour = extractFloat(findValueForLabel(LABEL_CALORIES_PER_HOUR))
        val conditionPI = extractFloat(findValueForLabel(LABEL_CONDITION))
        val moves = extractFloat(findValueForLabel(LABEL_MOVES))

        val needsReview = listOf(
            calories, distanceKm, avgPowerWatt, avgSpeedSpm, avgHeartRate,
            maxHeartRate, caloriesPerHour, conditionPI, moves
        ).any { it == null } || durationSeconds == null

        return ParsedWorkout(
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
    }

    /** Format total seconds back to MM:SS string for display */
    fun formatDuration(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }
}
