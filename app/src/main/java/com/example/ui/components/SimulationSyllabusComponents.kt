package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.kb.Simulation3DSyllabusUnit
import com.example.ui.theme.LocalImmunoColors

@Composable
fun Simulation3DSyllabusCard(
    unit: Simulation3DSyllabusUnit,
    selectedObjectId: String? = null,
    onFocusObject: ((String) -> Unit)? = null,
    initiallyExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val semantic = LocalImmunoColors.current
    var expanded by remember(unit.id) { mutableStateOf(initiallyExpanded) }
    var activeSection by remember(unit.id) { mutableStateOf("Objectives") }
    val checkedSteps = remember(unit.id) { mutableStateMapOf<Int, Boolean>() }
    val pickedQuiz = remember(unit.id) { mutableStateMapOf<Int, Int>() }

    val completedSteps = unit.guidedProtocol.indices.count { checkedSteps[it] == true }

    GlassCard(
        accentColor = MaterialTheme.colorScheme.secondary,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.School,
                    contentDescription = "3D Simulation Syllabus",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(unit.unitLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text("3D Syllabus: ${unit.title}", style = MaterialTheme.typography.titleSmall)
                }
            }
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.testTag("syllabus_toggle_${unit.id}")
            ) {
                Text(if (expanded) "Hide" else "Syllabus")
                Spacer(Modifier.width(2.dp))
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse syllabus" else "Expand syllabus",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EvidencePill(unit.difficulty)
            EvidencePill("${unit.estimatedMinutes} min")
            Text(
                text = "Checklist: $completedSteps/${unit.guidedProtocol.size}",
                style = MaterialTheme.typography.labelSmall,
                color = semantic.mutedText
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(unit.overview, style = MaterialTheme.typography.bodySmall)
                Text(
                    "Prerequisites: ${unit.prerequisites}",
                    style = MaterialTheme.typography.labelSmall,
                    color = semantic.mutedText
                )

                PillSelectorRow(
                    options = listOf("Objectives", "3D Structures", "Guided Lab", "Benchmarks", "Self-Check"),
                    selected = activeSection,
                    onSelect = { activeSection = it },
                    tagPrefix = "syl_sec_${unit.id}"
                )

                when (activeSection) {
                    "Objectives" -> {
                        Text("Learning Objectives", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        unit.learningObjectives.forEachIndexed { idx, obj ->
                            Text("${idx + 1}. $obj", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    "3D Structures" -> {
                        Text(
                            "Labeled 3D Structures" + if (onFocusObject != null) " (Tap to Highlight in 3D)" else "",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        unit.labeledStructures.forEach { st ->
                            val isFocused = selectedObjectId == st.objectId
                            GlassCard(
                                onClick = onFocusObject?.let { cb -> { cb(st.objectId) } },
                                accentColor = if (isFocused) MaterialTheme.colorScheme.primary else null,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(st.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                    EvidencePill(st.compartment)
                                }
                                Text("Markers / Motif: ${st.keyMarkers}", style = MaterialTheme.typography.labelSmall, color = semantic.mutedText)
                                Text(st.biologicalFunction, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    "Guided Lab" -> {
                        Text("Step-by-Step 3D Inspection Protocol", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        LinearProgressIndicator(
                            progress = { completedSteps.toFloat() / unit.guidedProtocol.size.coerceAtLeast(1) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        unit.guidedProtocol.forEachIndexed { idx, step ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Checkbox(
                                    checked = checkedSteps[idx] == true,
                                    onCheckedChange = { checkedSteps[idx] = it },
                                    modifier = Modifier.testTag("syl_step_${unit.id}_$idx")
                                )
                                Text(
                                    text = "${idx + 1}. $step",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }
                        }
                    }

                    "Benchmarks" -> {
                        Text("Quantitative Immunological Reference Ranges", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        unit.quantitativeBenchmarks.forEach { b ->
                            Text("• $b", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    "Self-Check" -> {
                        Text("3D Simulation Concept Check", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        unit.selfCheck.forEachIndexed { qIdx, q ->
                            val picked = pickedQuiz[qIdx]
                            GlassCard(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text("Q${qIdx + 1}. ${q.question}", style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(4.dp))
                                q.options.forEachIndexed { oIdx, opt ->
                                    val mark = when {
                                        picked == null -> ""
                                        oIdx == q.correctIndex -> " ✓"
                                        picked == oIdx -> " ✗"
                                        else -> ""
                                    }
                                    OutlinedButton(
                                        onClick = { pickedQuiz[qIdx] = oIdx },
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                                    ) {
                                        Text("${'A' + oIdx}. $opt$mark")
                                    }
                                }
                                if (picked != null) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = q.explanation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (picked == q.correctIndex) semantic.established else semantic.illustrative
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
