package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.kb.AcademyKb
import com.example.data.kb.QuizQuestion
import com.example.data.kb.Simulation3DSyllabusKb
import com.example.data.model.AcademyProgress
import com.example.data.model.CustomContent
import com.example.ui.components.AcademyTimedExamPanel
import com.example.ui.components.AcademyTimedQuizBank
import com.example.ui.components.EvidencePill
import com.example.ui.components.GlassCard
import com.example.ui.components.GlossaryContentView
import com.example.ui.components.ModuleHeader
import com.example.ui.components.PillSelectorRow
import com.example.ui.components.Simulation3DSyllabusCard
import com.example.ui.theme.LocalImmunoColors
import com.example.util.ExportHelper

@Composable
fun AcademyScreen(
    progressList: List<AcademyProgress>,
    customContent: List<CustomContent>,
    onMarkLessonDone: (String, String, List<String>) -> Unit,
    onRecordQuizScore: (String, Int, List<String>) -> Unit,
    onToast: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val semantic = LocalImmunoColors.current

    var subTab by remember { mutableStateOf("Tracks & Lessons") }
    var selectedTrackId by remember { mutableStateOf(AcademyKb.TRACKS.first().id) }

    // Flashcard state
    var cardIdx by remember { mutableIntStateOf(0) }
    var cardFlipped by remember { mutableStateOf(false) }

    // Case study state
    val caseChoice = remember { mutableStateMapOf<String, Int>() }

    val mergedQuestions = remember(customContent) {
        val customQs = customContent.filter { it.contentType == "quiz" }.map { cc ->
            QuizQuestion(
                id = cc.id,
                trackId = cc.trackId,
                difficulty = cc.difficulty,
                question = cc.prompt,
                options = cc.options.ifEmpty { listOf("True", "False") },
                correctIndex = cc.correctIndex.coerceIn(0, (cc.options.size - 1).coerceAtLeast(0)),
                explanation = cc.explanation
            )
        }
        customQs + AcademyKb.QUIZ_BANK
    }

    val activeTrack = remember(selectedTrackId) { AcademyKb.TRACKS.first { it.id == selectedTrackId } }
    val trackProg = progressList.firstOrNull { it.trackId == activeTrack.id }
    val completedLessons = trackProg?.completedLessons ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ModuleHeader(
            title = "Learning Academy",
            subtitle = "10 tracks, timed quizzes & exams, flashcards, clinical cases & 3D syllabus.",
            badge = "Adaptive LMS"
        )

        PillSelectorRow(
            options = listOf("Tracks & Lessons", "Glossary", "Quiz Bank", "Timed Exam", "3D Syllabus", "Flashcards", "Clinical Cases"),
            selected = subTab,
            onSelect = { subTab = it },
            tagPrefix = "acad_tab"
        )

        when (subTab) {
            "Tracks & Lessons" -> {
                PillSelectorRow(
                    options = AcademyKb.TRACKS.map { it.title },
                    selected = activeTrack.title,
                    onSelect = { t -> selectedTrackId = AcademyKb.TRACKS.first { it.title == t }.id },
                    tagPrefix = "acad_track"
                )
                GlassCard(accentColor = MaterialTheme.colorScheme.primary) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(activeTrack.title, style = MaterialTheme.typography.titleLarge)
                        EvidencePill(activeTrack.category)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("${completedLessons.size}/${activeTrack.lessons.size} lessons done • Best quiz: ${trackProg?.bestScore ?: 0}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { completedLessons.size.toFloat() / activeTrack.lessons.size.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                activeTrack.lessons.forEach { lesson ->
                    val done = lesson.id in completedLessons
                    GlassCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(lesson.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            EvidencePill(lesson.level)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(lesson.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        lesson.keyPoints.forEach { kp -> Text("• $kp", style = MaterialTheme.typography.bodySmall) }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onMarkLessonDone(activeTrack.id, lesson.id, completedLessons) },
                                enabled = !done,
                                modifier = Modifier.testTag("acad_done_${lesson.id}")
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (done) "Completed ✓" else "Mark Completed")
                            }
                            OutlinedButton(onClick = {
                                val md = "# ${lesson.title}\nLevel: ${lesson.level}\n\n${lesson.summary}\n\n" +
                                    lesson.keyPoints.joinToString("\n") { "- $it" }
                                ExportHelper.shareTextFile(context, "notes_${lesson.id}.md", "text/markdown", md, "Export Study Notes")
                                    .onSuccess { onToast("Exported $it") }
                            }) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Notes")
                            }
                        }
                    }
                }
            }

            "Glossary" -> {
                GlossaryContentView()
            }

            "Quiz Bank" -> {
                AcademyTimedQuizBank(
                    tracks = AcademyKb.TRACKS,
                    allQuestions = mergedQuestions,
                    activeTrackId = activeTrack.id,
                    completedLessons = completedLessons,
                    onRecordQuizScore = onRecordQuizScore,
                    onToast = onToast
                )
            }

            "Timed Exam" -> {
                AcademyTimedExamPanel(
                    tracks = AcademyKb.TRACKS,
                    allQuestions = mergedQuestions,
                    activeTrackId = activeTrack.id,
                    completedLessons = completedLessons,
                    onRecordQuizScore = onRecordQuizScore,
                    onToast = onToast
                )
            }

            "3D Syllabus" -> {
                GlassCard {
                    Text("Comprehensive 3D Simulation Curriculum", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Structured 6-unit interactive 3D syllabus covering lymphoid organs, cellular receptors, organelles, HIV-1 structural proteome, PDB structures, and coupled ODE kinetics.",
                        style = MaterialTheme.typography.bodySmall,
                        color = semantic.mutedText
                    )
                }
                Simulation3DSyllabusKb.UNITS.forEachIndexed { idx, unit ->
                    Simulation3DSyllabusCard(
                        unit = unit,
                        initiallyExpanded = idx == 0
                    )
                }
            }

            "Flashcards" -> {
                val customCards = customContent.filter { it.contentType == "flashcard" }.map { it.prompt to it.explanation }
                val builtInCards = AcademyKb.QUIZ_BANK.take(25).map { it.question to "${it.options[it.correctIndex]} — ${it.explanation}" }
                val allCards = customCards + builtInCards
                val cur = allCards.getOrNull(cardIdx % allCards.size.coerceAtLeast(1))
                if (cur != null) {
                    GlassCard(
                        onClick = { cardFlipped = !cardFlipped },
                        accentColor = if (cardFlipped) semantic.established else MaterialTheme.colorScheme.primary
                    ) {
                        Text("Flashcard ${(cardIdx % allCards.size) + 1} / ${allCards.size} (Tap to flip)", style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (cardFlipped) cur.second else cur.first,
                            style = if (cardFlipped) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(onClick = { cardIdx = (cardIdx - 1 + allCards.size) % allCards.size; cardFlipped = false }) { Text("Prev") }
                        Button(onClick = { cardIdx = (cardIdx + 1) % allCards.size; cardFlipped = false }, modifier = Modifier.testTag("acad_next_card_btn")) { Text("Next Card") }
                    }
                }
            }

            "Clinical Cases" -> {
                AcademyKb.CLINICAL_CASES.forEach { cs ->
                    val picked = caseChoice[cs.id]
                    GlassCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(cs.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            EvidencePill(cs.difficulty)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(cs.vignette, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        cs.labFindings.forEach { f -> Text("• $f", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                        Spacer(Modifier.height(6.dp))
                        Text(cs.question, style = MaterialTheme.typography.titleSmall)
                        cs.options.forEachIndexed { idx, opt ->
                            OutlinedButton(
                                onClick = { caseChoice[cs.id] = idx },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                val mark = if (picked == null) "" else if (idx == cs.correctIndex) " ✓" else if (picked == idx) " ✗" else ""
                                Text("$opt$mark")
                            }
                        }
                        if (picked != null) {
                            Spacer(Modifier.height(4.dp))
                            Text("Case Synthesis: ${cs.synthesis}", style = MaterialTheme.typography.bodySmall, color = semantic.established)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
