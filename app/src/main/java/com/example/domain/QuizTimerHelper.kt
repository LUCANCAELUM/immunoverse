package com.example.domain

import com.example.data.kb.QuizQuestion

enum class QuizTimeLimitMode(
    val label: String,
    val isTimed: Boolean,
    val isPerQuestion: Boolean,
    val defaultSeconds: Int
) {
    UNTIMED("Untimed", false, false, 0),
    PER_QUESTION_20S("20s / Question", true, true, 20),
    PER_QUESTION_45S("45s / Question", true, true, 45),
    SESSION_2M("2 min Sprint", true, false, 120),
    SESSION_5M("5 min Quiz", true, false, 300)
}

data class TimedQuizScoreSummary(
    val totalQuestions: Int,
    val answeredCount: Int,
    val correctCount: Int,
    val timedOutCount: Int,
    val scorePercent: Int,
    val elapsedSeconds: Int
)

object QuizTimerHelper {
    fun formatSeconds(seconds: Int): String {
        val safe = seconds.coerceAtLeast(0)
        val mins = safe / 60
        val secs = safe % 60
        return "%02d:%02d".format(mins, secs)
    }

    fun filterQuestions(
        questions: List<QuizQuestion>,
        trackId: String,
        difficulty: String
    ): List<QuizQuestion> {
        return questions.filter { q ->
            val trackMatch = trackId == "all" || q.trackId == trackId
            val diffMatch = difficulty == "All" || q.difficulty == difficulty
            trackMatch && diffMatch
        }
    }

    fun computeSummary(
        questions: List<QuizQuestion>,
        answers: Map<String, Int?>,
        timedOutIds: Set<String>,
        elapsedSeconds: Int
    ): TimedQuizScoreSummary {
        val total = questions.size.coerceAtLeast(1)
        val answered = questions.count { answers[it.id] != null }
        val correct = questions.count { q -> answers[q.id] == q.correctIndex }
        val timedOut = questions.count { q -> q.id in timedOutIds && answers[q.id] == null }
        val pct = (correct * 100) / total
        return TimedQuizScoreSummary(
            totalQuestions = questions.size,
            answeredCount = answered,
            correctCount = correct,
            timedOutCount = timedOut,
            scorePercent = pct,
            elapsedSeconds = elapsedSeconds.coerceAtLeast(0)
        )
    }
}
