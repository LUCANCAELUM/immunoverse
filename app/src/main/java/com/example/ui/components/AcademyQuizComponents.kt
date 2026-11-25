package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.kb.AcademyTrack
import com.example.data.kb.QuizQuestion
import com.example.domain.QuizTimeLimitMode
import com.example.domain.QuizTimerHelper
import com.example.ui.theme.LocalImmunoColors
import kotlinx.coroutines.delay

@Composable
fun AcademyTimedQuizBank(
    tracks: List<AcademyTrack>,
    allQuestions: List<QuizQuestion>,
    activeTrackId: String,
    completedLessons: List<String>,
    onRecordQuizScore: (String, Int, List<String>) -> Unit,
    onToast: (String) -> Unit
) {
    val semantic = LocalImmunoColors.current

    var quizDiff by remember { mutableStateOf("All") }
    var selectedTrackFilter by remember { mutableStateOf("all") }
    var timeMode by remember { mutableStateOf(QuizTimeLimitMode.PER_QUESTION_20S) }
    var qIndex by remember { mutableIntStateOf(0) }
    var timerRunning by remember { mutableStateOf(false) }
    var secondsRemaining by remember { mutableIntStateOf(timeMode.defaultSeconds) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var sessionFinished by remember { mutableStateOf(false) }

    val answers = remember { mutableStateMapOf<String, Int?>() }
    val timedOutIds = remember { mutableStateMapOf<String, Boolean>() }

    val filteredQuiz = remember(allQuestions, selectedTrackFilter, quizDiff) {
        QuizTimerHelper.filterQuestions(allQuestions, selectedTrackFilter, quizDiff)
    }

    val safeIndex = qIndex.coerceIn(0, (filteredQuiz.size - 1).coerceAtLeast(0))
    val currentQ = filteredQuiz.getOrNull(safeIndex)

    fun resetQuizSession(startImmediately: Boolean, newMode: QuizTimeLimitMode = timeMode) {
        timeMode = newMode
        answers.clear()
        timedOutIds.clear()
        qIndex = 0
        elapsedSeconds = 0
        sessionFinished = false
        secondsRemaining = newMode.defaultSeconds
        timerRunning = startImmediately && newMode.isTimed
    }

    fun finishAndSave() {
        timerRunning = false
        sessionFinished = true
        val summary = QuizTimerHelper.computeSummary(
            questions = filteredQuiz,
            answers = answers,
            timedOutIds = timedOutIds.keys,
            elapsedSeconds = elapsedSeconds
        )
        val saveTrackId = if (selectedTrackFilter == "all") activeTrackId else selectedTrackFilter
        onRecordQuizScore(saveTrackId, summary.scorePercent, completedLessons)
        onToast("Quiz complete: ${summary.correctCount}/${summary.totalQuestions} (${summary.scorePercent}%)")
    }

    LaunchedEffect(timerRunning, timeMode, safeIndex, filteredQuiz.size, sessionFinished) {
        if (!timerRunning || !timeMode.isTimed || sessionFinished || currentQ == null) return@LaunchedEffect
        while (timerRunning && !sessionFinished) {
            delay(1000L)
            elapsedSeconds++
            if (secondsRemaining > 0) {
                secondsRemaining--
            }
            if (secondsRemaining == 0) {
                if (timeMode.isPerQuestion) {
                    if (answers[currentQ.id] == null) {
                        timedOutIds[currentQ.id] = true
                    }
                    timerRunning = false
                } else {
                    finishAndSave()
                }
            }
        }
    }

    val trackFilterOptions = remember(tracks) { listOf("All Tracks") + tracks.map { it.title } }
    val selectedTrackTitle = if (selectedTrackFilter == "all") {
        "All Tracks"
    } else {
        tracks.firstOrNull { it.id == selectedTrackFilter }?.title ?: "All Tracks"
    }

    PillSelectorRow(
        options = listOf("All", "Beginner", "Undergraduate", "Advanced"),
        selected = quizDiff,
        onSelect = {
            quizDiff = it
            resetQuizSession(startImmediately = false)
        },
        tagPrefix = "acad_diff"
    )

    PillSelectorRow(
        options = trackFilterOptions,
        selected = selectedTrackTitle,
        onSelect = { title ->
            selectedTrackFilter = if (title == "All Tracks") "all" else tracks.first { it.title == title }.id
            resetQuizSession(startImmediately = false)
        },
        tagPrefix = "acad_quiz_track"
    )

    // Time Limit Selector & Live Timer HUD
    GlassCard(accentColor = if (timeMode.isTimed) MaterialTheme.colorScheme.primary else null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Timer, contentDescription = "Quiz timer", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text("Quiz Time Limit", style = MaterialTheme.typography.titleSmall)
            }
            if (timeMode.isTimed) {
                EvidencePill("${QuizTimerHelper.formatSeconds(secondsRemaining)} left")
            } else {
                EvidencePill("Untimed Practice")
            }
        }
        Spacer(Modifier.height(6.dp))
        PillSelectorRow(
            options = QuizTimeLimitMode.entries.map { it.label },
            selected = timeMode.label,
            onSelect = { label ->
                val chosen = QuizTimeLimitMode.entries.first { it.label == label }
                resetQuizSession(startImmediately = chosen.isTimed, newMode = chosen)
            },
            tagPrefix = "acad_quiz_timer_mode"
        )
        if (timeMode.isTimed) {
            Spacer(Modifier.height(6.dp))
            val maxSec = timeMode.defaultSeconds.coerceAtLeast(1)
            LinearProgressIndicator(
                progress = { secondsRemaining.toFloat() / maxSec },
                color = if (secondsRemaining <= 10) semantic.critical else MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = {
                            if (secondsRemaining == 0 && timeMode.isPerQuestion) {
                                secondsRemaining = timeMode.defaultSeconds
                            }
                            timerRunning = !timerRunning
                        },
                        modifier = Modifier.testTag("acad_quiz_timer_toggle")
                    ) {
                        Icon(if (timerRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (timerRunning) "Pause" else if (elapsedSeconds == 0) "Start Timed Quiz" else "Resume")
                    }
                    IconButton(
                        onClick = { resetQuizSession(startImmediately = true) },
                        modifier = Modifier.size(44.dp).testTag("acad_quiz_timer_reset")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Restart quiz timer")
                    }
                }
                OutlinedButton(
                    onClick = { finishAndSave() },
                    enabled = !sessionFinished && filteredQuiz.isNotEmpty(),
                    modifier = Modifier.testTag("acad_quiz_finish_btn")
                ) {
                    Text("Finish & Score")
                }
            }
        }
    }

    if (sessionFinished && filteredQuiz.isNotEmpty()) {
        val summary = QuizTimerHelper.computeSummary(filteredQuiz, answers, timedOutIds.keys, elapsedSeconds)
        GlassCard(accentColor = semantic.established) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Timed Quiz Result", style = MaterialTheme.typography.titleMedium, color = semantic.established)
                EvidencePill("${summary.scorePercent}%")
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Correct: ${summary.correctCount} / ${summary.totalQuestions} • Answered: ${summary.answeredCount} • Timed Out: ${summary.timedOutCount} • Time: ${QuizTimerHelper.formatSeconds(summary.elapsedSeconds)}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = { resetQuizSession(startImmediately = timeMode.isTimed) },
                modifier = Modifier.testTag("acad_quiz_retake_btn")
            ) {
                Text("Retake Timed Quiz")
            }
        }
    }

    if (currentQ != null) {
        val pickedOption = answers[currentQ.id]
        val isTimedOut = timedOutIds[currentQ.id] == true
        val isLocked = sessionFinished || isTimedOut || pickedOption != null
        val answeredCount = filteredQuiz.count { answers[it.id] != null }
        val correctSoFar = filteredQuiz.count { answers[it.id] == it.correctIndex }

        GlassCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Question ${safeIndex + 1} / ${filteredQuiz.size} • Score $correctSoFar/$answeredCount",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                EvidencePill(currentQ.difficulty)
            }
            Spacer(Modifier.height(6.dp))
            Text(currentQ.question, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            currentQ.options.forEachIndexed { idx, opt ->
                val isCorrect = idx == currentQ.correctIndex
                val isPicked = pickedOption == idx
                OutlinedButton(
                    onClick = {
                        if (!isLocked) {
                            if (timeMode.isTimed && !timerRunning && elapsedSeconds == 0) {
                                timerRunning = true
                            }
                            answers[currentQ.id] = idx
                            if (timeMode.isPerQuestion) {
                                timerRunning = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).testTag("acad_quiz_opt_$idx")
                ) {
                    val tag = when {
                        pickedOption == null && !isTimedOut -> ""
                        isCorrect -> " ✓"
                        isPicked -> " ✗"
                        else -> ""
                    }
                    Text("${'A' + idx}. $opt$tag")
                }
            }
            if (isTimedOut && pickedOption == null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Time's Up! Correct answer: ${'A' + currentQ.correctIndex}. ${currentQ.options[currentQ.correctIndex]} — ${currentQ.explanation}",
                    style = MaterialTheme.typography.bodySmall,
                    color = semantic.critical
                )
            } else if (pickedOption != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (pickedOption == currentQ.correctIndex) "Correct! ${currentQ.explanation}" else "Review: ${currentQ.explanation}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (pickedOption == currentQ.correctIndex) semantic.established else semantic.illustrative
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(
                    onClick = {
                        if (safeIndex > 0) {
                            qIndex--
                            if (timeMode.isPerQuestion) {
                                secondsRemaining = timeMode.defaultSeconds
                                timerRunning = answers[filteredQuiz[safeIndex - 1].id] == null && timedOutIds[filteredQuiz[safeIndex - 1].id] != true
                            }
                        }
                    },
                    enabled = safeIndex > 0
                ) { Text("Prev") }
                Button(
                    onClick = {
                        val nextIdx = (safeIndex + 1) % filteredQuiz.size
                        qIndex = nextIdx
                        if (timeMode.isPerQuestion) {
                            secondsRemaining = timeMode.defaultSeconds
                            val nextQ = filteredQuiz[nextIdx]
                            timerRunning = !sessionFinished && answers[nextQ.id] == null && timedOutIds[nextQ.id] != true
                        }
                    },
                    modifier = Modifier.testTag("acad_next_q_btn")
                ) { Text("Next") }
            }
        }
    } else {
        GlassCard {
            Text("No questions match this track and difficulty filter.", style = MaterialTheme.typography.bodyMedium, color = semantic.mutedText)
        }
    }
}

@Composable
fun AcademyTimedExamPanel(
    tracks: List<AcademyTrack>,
    allQuestions: List<QuizQuestion>,
    activeTrackId: String,
    completedLessons: List<String>,
    onRecordQuizScore: (String, Int, List<String>) -> Unit,
    onToast: (String) -> Unit
) {
    val semantic = LocalImmunoColors.current

    var examTrackId by remember { mutableStateOf("all") }
    var examDiff by remember { mutableStateOf("All") }
    var examCount by remember { mutableIntStateOf(10) }
    var examDurationMin by remember { mutableIntStateOf(3) }

    val examPool = remember(allQuestions, examTrackId, examDiff, examCount) {
        QuizTimerHelper.filterQuestions(allQuestions, examTrackId, examDiff).take(examCount)
    }

    var examRunning by remember { mutableStateOf(false) }
    var examSecondsLeft by remember { mutableIntStateOf(examDurationMin * 60) }
    val examAnswers = remember { mutableStateMapOf<Int, Int>() }
    var examSubmitted by remember { mutableStateOf(false) }

    fun submitExam(autoTimedOut: Boolean = false) {
        examRunning = false
        examSubmitted = true
        val correct = examPool.indices.count { i -> examAnswers[i] == examPool[i].correctIndex }
        val pct = (correct * 100) / examPool.size.coerceAtLeast(1)
        val saveTrack = if (examTrackId == "all") activeTrackId else examTrackId
        onRecordQuizScore(saveTrack, pct, completedLessons)
        if (autoTimedOut) {
            onToast("Time expired! Exam auto-submitted ($correct/${examPool.size}, $pct%)")
        }
    }

    LaunchedEffect(examRunning) {
        while (examRunning && examSecondsLeft > 0) {
            delay(1000L)
            examSecondsLeft--
            if (examSecondsLeft == 0) {
                submitExam(autoTimedOut = true)
            }
        }
    }

    val totalDurationSec = (examDurationMin * 60).coerceAtLeast(60)
    val trackLabels = remember(tracks) { listOf("All Tracks") + tracks.map { it.title } }
    val selectedTrackTitle = if (examTrackId == "all") "All Tracks" else tracks.firstOrNull { it.id == examTrackId }?.title ?: "All Tracks"

    GlassCard(accentColor = MaterialTheme.colorScheme.primary) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${examPool.size}-Question Timed Exam", style = MaterialTheme.typography.titleMedium)
            EvidencePill(QuizTimerHelper.formatSeconds(examSecondsLeft))
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { examSecondsLeft.toFloat() / totalDurationSec },
            color = if (examSecondsLeft <= 30 && examRunning) semantic.critical else MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text("Time Limit & Scope", style = MaterialTheme.typography.labelMedium, color = semantic.mutedText)
        PillSelectorRow(
            options = listOf("1 min", "3 min", "5 min", "10 min"),
            selected = "$examDurationMin min",
            onSelect = { opt ->
                val mins = opt.substringBefore(" ").toIntOrNull() ?: 3
                examDurationMin = mins
                if (!examRunning) {
                    examSecondsLeft = mins * 60
                }
            },
            tagPrefix = "exam_duration"
        )
        PillSelectorRow(
            options = listOf("5 Qs", "10 Qs", "15 Qs"),
            selected = "$examCount Qs",
            onSelect = { opt ->
                examCount = opt.substringBefore(" ").toIntOrNull() ?: 10
                if (!examRunning) examAnswers.clear()
            },
            tagPrefix = "exam_count"
        )
        PillSelectorRow(
            options = listOf("All", "Beginner", "Undergraduate", "Advanced"),
            selected = examDiff,
            onSelect = {
                examDiff = it
                if (!examRunning) examAnswers.clear()
            },
            tagPrefix = "exam_diff"
        )
        PillSelectorRow(
            options = trackLabels,
            selected = selectedTrackTitle,
            onSelect = { title ->
                examTrackId = if (title == "All Tracks") "all" else tracks.first { it.title == title }.id
                if (!examRunning) examAnswers.clear()
            },
            tagPrefix = "exam_track"
        )
        Spacer(Modifier.height(6.dp))
        Button(
            onClick = {
                examAnswers.clear()
                examSubmitted = false
                examSecondsLeft = examDurationMin * 60
                examRunning = true
            },
            modifier = Modifier.fillMaxWidth().testTag("acad_start_exam_btn")
        ) {
            Text(if (examRunning) "Restart Timer ($examDurationMin min)" else "Start Timed Exam ($examDurationMin min)")
        }
    }

    if (examSubmitted) {
        val correct = examPool.indices.count { i -> examAnswers[i] == examPool[i].correctIndex }
        val pct = (correct * 100) / examPool.size.coerceAtLeast(1)
        val timeUsed = (totalDurationSec - examSecondsLeft).coerceAtLeast(0)
        GlassCard(accentColor = semantic.established) {
            Text("Exam Result: $correct / ${examPool.size} ($pct%)", style = MaterialTheme.typography.titleLarge, color = semantic.established)
            Text("Time Used: ${QuizTimerHelper.formatSeconds(timeUsed)} / ${QuizTimerHelper.formatSeconds(totalDurationSec)}", style = MaterialTheme.typography.bodySmall)
        }
    }

    examPool.forEachIndexed { idx, q ->
        GlassCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${idx + 1}. ${q.question}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                EvidencePill(q.difficulty)
            }
            Spacer(Modifier.height(4.dp))
            q.options.forEachIndexed { oIdx, opt ->
                val chosen = examAnswers[idx] == oIdx
                val suffix = when {
                    !examSubmitted -> ""
                    oIdx == q.correctIndex -> " ✓"
                    chosen -> " ✗"
                    else -> ""
                }
                OutlinedButton(
                    onClick = {
                        if (!examSubmitted) {
                            if (!examRunning) examRunning = true
                            examAnswers[idx] = oIdx
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                ) {
                    Text("${if (chosen) "[●] " else "[ ] "}$opt$suffix")
                }
            }
            if (examSubmitted) {
                Spacer(Modifier.height(4.dp))
                val isOk = examAnswers[idx] == q.correctIndex
                Text(
                    text = (if (isOk) "Correct: " else "Review: ") + q.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOk) semantic.established else semantic.illustrative
                )
            }
        }
    }

    Button(
        onClick = { submitExam(autoTimedOut = false) },
        enabled = examPool.isNotEmpty() && !examSubmitted,
        modifier = Modifier.fillMaxWidth().testTag("acad_submit_exam_btn")
    ) { Text("Submit Timed Exam") }
}
