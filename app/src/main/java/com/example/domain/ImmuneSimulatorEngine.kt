package com.example.domain

import kotlin.math.max
import kotlin.math.min

data class SimParams(
    val presetKey: String = "normal",
    val antigenLoad: Double = 1.2,
    val innateGain: Double = 1.0,
    val tCellGain: Double = 1.0,
    val bCellGain: Double = 1.0,
    val abClear: Double = 1.0,
    val autoTolerance: Double = 1.0,
    val rechallengeDay: Double = 0.0,
    val durationDays: Double = 30.0,
    val initMemory: Double = 0.02,
    val antigenGrowthRate: Double = 0.52,
    val isVaccine: Boolean = false
)

data class SimPreset(
    val key: String,
    val label: String,
    val summary: String,
    val params: SimParams,
    val biologicalContext: String = "",
    val dominantAxis: String = "",
    val expectedOutcome: String = ""
)

data class SimStep(
    val day: Double,
    val antigen: Double,
    val innate: Double,
    val cytokine: Double,
    val tCell: Double,
    val bCell: Double,
    val plasma: Double,
    val antibody: Double,
    val memory: Double,
    val damage: Double,
    val apcActivation: Double = 0.0,
    val complementFlux: Double = 0.0,
    val tregSuppression: Double = 0.0,
    val affinityMaturation: Double = 1.0,
    val phagocyticClearance: Double = 0.0,
    val phaseTag: String = "Phase I: Innate Sensing"
)

data class SimResult(
    val params: SimParams,
    val series: List<SimStep>,
    val peakAntigen: Double,
    val peakCytokine: Double,
    val peakAntibody: Double,
    val peakMemory: Double,
    val peakDamage: Double,
    val events: List<String>,
    val peakTCell: Double = 0.0,
    val peakBCell: Double = 0.0,
    val peakPlasma: Double = 0.0,
    val peakAffinityMaturation: Double = 1.0,
    val timeTo50ClearanceDays: Double? = null,
    val timeToClearanceDays: Double? = null,
    val aucAntigen: Double = 0.0,
    val aucCytokine: Double = 0.0,
    val aucDamage: Double = 0.0,
    val effectorToPathogenRatio: Double = 0.0,
    val cytokineStormRisk: String = "Low",
    val mechanisticNote: String = ""
)

object ImmuneSimulatorEngine {
    const val MODEL_EQUATIONS = """
Educational Coupled 9-Variable ODE System + Derived Immunological Compartments (Explicit Euler, dt = 0.02 days):
• dA/dt = r_A·A·(1 - A/A_max) - (k_IA·I + k_TA·T + k_Ab·Ab)·A + dose(t)
• dI/dt = g_I·A/(K_A + A) - δ_I·(I - I_0)
• dC/dt = α_C·(I·A + T·A) - δ_C·C
• dT/dt = g_T·(I·A)/(1 + I·A) + m_T·M·A - δ_T·T
• dB/dt = g_B·A·(0.35 + T) + m_B·M·A - δ_B·B
• dP/dt = k_BP·B - δ_P·P
• dAb/dt = k_AbP·P - δ_Ab·Ab
• dM/dt = k_TM·(T + B) - δ_M·M
• dD/dt = (k_DA·A + max(0, C - τ_tol·1.6))·(1 + 1.5·max(0, 1 - τ_tol)) - δ_D·D
Derived Readouts:
• APC Priming = σ(I·A) · (0.4 + 0.6·Treg/Tol)
• Complement MAC Flux = (0.35·I + 0.65·Ab)·A / (0.5 + A)
• GC Affinity Index = 1.0 + 4.2·(1 - exp(-0.14·∫ B·T dt))
• Phagocytic Clearance = (0.38·g_I·I + 0.52·k_Ab·Ab)·A
"""

    const val MODEL_ASSUMPTIONS = """
Assumptions & Limitations:
1. Normalized dimensionless concentrations (arbitrary educational units, not patient serum titers or CFU/mL).
2. Well-mixed compartment; germinal center affinity maturation and complement MAC flux are derived phenomenological indices.
3. Illustrative parameters highlight primary vs memory recall, vaccination prime-boost, immunodeficiency, and self-tolerance loss — never validated for clinical prediction.
"""

    val PRESETS = listOf(
        SimPreset(
            key = "normal",
            label = "Normal Response",
            summary = "Balanced innate containment followed by Day 7–14 adaptive clearance.",
            params = SimParams("normal", 1.2, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 30.0, 0.02, 0.50),
            biologicalContext = "Acute uncomplicated respiratory or soft-tissue microbe encountering an immunocompetent host.",
            dominantAxis = "PRR/Macrophage → cDC Priming → Th1/Tfh + IgG",
            expectedOutcome = "Pathogen peaks near Day 2–4, clears below threshold by Day 9–14 with durable memory pool."
        ),
        SimPreset(
            key = "primary",
            label = "Primary Response",
            summary = "Naive repertoire lag (~6–8 days) before IgM/IgG expansion.",
            params = SimParams("primary", 1.4, 0.9, 0.9, 0.85, 0.95, 1.0, 0.0, 28.0, 0.0, 0.55),
            biologicalContext = "First lifetime encounter with a novel pathogen; zero baseline memory B or T clones.",
            dominantAxis = "Naive Clone Recruitment (~1 in 10^5) → Germinal Center Maturation",
            expectedOutcome = "Higher initial pathogen peak and tissue stress before IgG affinity maturation resolves infection."
        ),
        SimPreset(
            key = "secondary",
            label = "Secondary Memory Recall",
            summary = "Pre-existing memory B/T pool triggers rapid high-titer antibody clearance.",
            params = SimParams("secondary", 1.4, 1.0, 1.2, 1.3, 1.2, 1.0, 0.0, 25.0, 0.85, 0.55),
            biologicalContext = "Re-exposure in an immune host harboring class-switched memory B cells & Tcm/Trm.",
            dominantAxis = "Anamnestic Memory B → Rapid Plasma Burst + High-Avidity IgG",
            expectedOutcome = "Minimal symptom burden; 2–3x higher peak antibody achieved within days instead of weeks."
        ),
        SimPreset(
            key = "vaccine",
            label = "Vaccination + Challenge",
            summary = "Non-replicating prime followed by live pathogen rechallenge at Day 21.",
            params = SimParams("vaccine", 1.0, 1.0, 1.1, 1.15, 1.1, 1.0, 21.0, 38.0, 0.02, 0.05, isVaccine = true),
            biologicalContext = "Adjuvanted subunit or mRNA prime establishing germinal centers prior to Day-21 live exposure.",
            dominantAxis = "Non-replicating Antigen Prime → Germinal Center Memory → Blunted Day-21 Challenge",
            expectedOutcome = "Low tissue damage during vaccination; pre-formed IgG & memory flatten the Day-21 challenge spike."
        ),
        SimPreset(
            key = "immunodef",
            label = "Immunodeficiency",
            summary = "Impaired T/B effector priming yields sluggish clearance and prolonged pathogen load.",
            params = SimParams("immunodef", 1.3, 0.65, 0.25, 0.25, 0.35, 1.0, 0.0, 35.0, 0.0, 0.58),
            biologicalContext = "Combined cellular + humoral defect (analogous to SCID, CVID, or severe T-cell depletion).",
            dominantAxis = "Unchecked Pathogen Replication + Compensatory Innate Exhaustion",
            expectedOutcome = "Failure to establish adaptive sterilizing immunity; sustained pathogen AUC and progressive damage."
        ),
        SimPreset(
            key = "autoimmune",
            label = "Autoimmune Flare",
            summary = "Reduced tolerance threshold drives self-sustaining cytokine and tissue-damage loops.",
            params = SimParams("autoimmune", 0.9, 1.2, 1.4, 1.3, 1.0, 0.25, 0.0, 35.0, 0.15, 0.30),
            biologicalContext = "Broken peripheral/central tolerance (e.g., Treg/FOXP3 deficit or CTLA-4/PD-1 checkpoint loss).",
            dominantAxis = "Autoreactive T/B Loop + Cytokine-Driven Tissue Injury Despite Low Pathogen",
            expectedOutcome = "Disproportionately high cytokine & tissue-damage AUC with inadequate Treg counter-regulation."
        ),
        SimPreset(
            key = "chronic",
            label = "Chronic Inflammation",
            summary = "High replication + muted antibody clearance sustains prolonged IL-6/TNF elevation.",
            params = SimParams("chronic", 1.5, 1.3, 0.75, 0.65, 0.45, 0.65, 0.0, 40.0, 0.05, 0.68),
            biologicalContext = "Persistent pathogen or granulomatous focus evading humoral neutralization.",
            dominantAxis = "Sustained Macrophage/Cytokine Activation + Incomplete Pathogen Eradication",
            expectedOutcome = "Oscillating or plateaued inflammatory cytokines driving cumulative organ stress."
        ),
        SimPreset(
            key = "viral",
            label = "Acute Viral Infection",
            summary = "Fast intracellular replication countered by early innate cytokine wave and CD8+ T burst.",
            params = SimParams("viral", 1.6, 1.15, 1.35, 0.95, 1.0, 0.95, 0.0, 30.0, 0.02, 0.75),
            biologicalContext = "Rapidly replicating respiratory/systemic virus triggering Type I IFN and cytotoxic CTL expansion.",
            dominantAxis = "IFN-α/β Innate Wave → cDC1 Cross-Presentation → Perforin/Granzyme CTL Burst",
            expectedOutcome = "Steep early viral peak followed by robust CD8+ T-cell contraction once virions drop."
        ),
        SimPreset(
            key = "bacterial",
            label = "Extracellular Bacterial",
            summary = "Strong neutrophil innate influx + high opsonizing antibody clearance.",
            params = SimParams("bacterial", 1.5, 1.4, 0.95, 1.3, 1.4, 0.95, 0.0, 28.0, 0.02, 0.66),
            biologicalContext = "Encapsulated extracellular bacterium activating TLR2/4, complement C3b, and IgG opsonization.",
            dominantAxis = "Neutrophil Respiratory Burst + C3b Complement + Opsonizing IgG",
            expectedOutcome = "Rapid innate braking aided by strong phagocytic clearance and brisk antibody neutralization."
        )
    )

    fun runSim(params: SimParams): SimResult {
        val dt = 0.02
        val totalSteps = ((params.durationDays / dt).toInt()).coerceIn(250, 4500)
        var a = params.antigenLoad.coerceAtLeast(0.0)
        var i = 0.15
        var c = 0.05
        var t = 0.02
        var b = 0.02
        var p = 0.01
        var ab = if (params.initMemory > 0.2) 0.45 else 0.02
        var m = params.initMemory.coerceAtLeast(0.0)
        var d = 0.0
        var gcIntegral = if (params.initMemory > 0.2) 4.0 else 0.0

        val out = ArrayList<SimStep>(totalSteps / 5 + 2)
        val events = mutableListOf<String>()
        var innatePeaked = false
        var adaptiveStarted = false
        var gcPeaked = false
        var clearedFlag = false
        var rechallengeInjected = false

        var maxA = a
        var maxC = c
        var maxAb = ab
        var maxM = m
        var maxD = d
        var maxT = t
        var maxB = b
        var maxP = p
        var maxAff = 1.0
        var t50Clear: Double? = null
        var tClear: Double? = null
        var aucA = 0.0
        var aucC = 0.0
        var aucD = 0.0

        for (step in 0..totalSteps) {
            val day = step * dt
            if (!rechallengeInjected && params.rechallengeDay > 0.5 && day >= params.rechallengeDay) {
                a += 1.45
                rechallengeInjected = true
                events += "Day ${"%.1f".format(day)}: Secondary antigen rechallenge administered (+1.45 Ag)."
            }

            maxA = max(maxA, a)
            maxC = max(maxC, c)
            maxAb = max(maxAb, ab)
            maxM = max(maxM, m)
            maxD = max(maxD, d)
            maxT = max(maxT, t)
            maxB = max(maxB, b)
            maxP = max(maxP, p)

            val autoDrive = if (params.autoTolerance < 0.5) (0.5 - params.autoTolerance) * 0.35 else 0.0
            val effectiveA = a + autoDrive
            val apcAct = ((i * effectiveA) / (0.55 + i * effectiveA)).coerceIn(0.0, 1.0)
            val compFlux = (((0.35 * i + 0.65 * ab) * a) / (0.65 + a)).coerceAtLeast(0.0)
            val tregSupp = (params.autoTolerance * (0.45 + 0.55 * (t / (1.0 + t)))).coerceIn(0.05, 1.8)
            val affMat = (1.0 + 4.2 * (1.0 - kotlin.math.exp(-0.14 * gcIntegral))).coerceAtLeast(1.0)
            val phagClear = ((0.38 * params.innateGain * i + 0.52 * params.abClear * ab) * a).coerceAtLeast(0.0)
            maxAff = max(maxAff, affMat)

            val phaseLabel = when {
                params.autoTolerance < 0.5 && c > 1.2 -> "Dysregulated Auto-Inflammatory Flare"
                rechallengeInjected && day - params.rechallengeDay < 5.0 -> "Anamnestic Recall Burst"
                a < 0.08 && ab > 0.45 && m > 0.25 -> "Phase V: Contraction & Memory"
                ab > 0.8 && a < maxA * 0.65 -> "Phase IV: Humoral Neutralization"
                t > 0.45 || b > 0.35 -> "Phase III: Clonal Expansion & GC"
                apcAct > 0.35 -> "Phase II: APC Priming & Synapse"
                else -> "Phase I: Innate Sensing"
            }

            if (step % 5 == 0) {
                out += SimStep(
                    day = day,
                    antigen = a,
                    innate = i,
                    cytokine = c,
                    tCell = t,
                    bCell = b,
                    plasma = p,
                    antibody = ab,
                    memory = m,
                    damage = d,
                    apcActivation = apcAct,
                    complementFlux = compFlux,
                    tregSuppression = tregSupp,
                    affinityMaturation = affMat,
                    phagocyticClearance = phagClear,
                    phaseTag = phaseLabel
                )
            }

            aucA += a * dt
            aucC += c * dt
            aucD += d * dt

            if (t50Clear == null && day > 1.5 && maxA > 0.4 && a <= maxA * 0.5) {
                t50Clear = day
            }
            if (!clearedFlag && step > 100 && a < 0.05 && maxA > 0.3) {
                clearedFlag = true
                tClear = day
                events += "Day ${"%.1f".format(day)}: Pathogen cleared below sterile threshold (<0.05)."
            }
            if (!innatePeaked && i > 0.75) {
                innatePeaked = true
                events += "Day ${"%.1f".format(day)}: Innate sentinel & complement MAC activation engaged."
            }
            if (!adaptiveStarted && t > 0.45) {
                adaptiveStarted = true
                events += "Day ${"%.1f".format(day)}: CD4/CD8 clonal expansion & germinal center entry."
            }
            if (!gcPeaked && affMat >= 2.2) {
                gcPeaked = true
                events += "Day ${"%.1f".format(day)}: Germinal center affinity maturation crossed 2.2x baseline."
            }

            val replication = if (params.isVaccine && !rechallengeInjected) {
                -0.18 * a
            } else {
                params.antigenGrowthRate * a * (1.0 - a / 8.0)
            }

            val dA = replication - (0.38 * params.innateGain * i + 0.42 * params.tCellGain * t + 0.52 * params.abClear * ab) * a
            val dI = 0.85 * params.innateGain * (effectiveA / (0.45 + effectiveA)) - 0.36 * (i - 0.15)
            val dC = 0.72 * (i * effectiveA + 0.65 * t * effectiveA) - 0.55 * c
            val dT = 0.62 * params.tCellGain * ((i * effectiveA) / (0.6 + i * effectiveA)) +
                0.85 * params.tCellGain * m * effectiveA - 0.24 * t
            val dB = 0.58 * params.bCellGain * effectiveA * (0.35 + t) +
                0.90 * params.bCellGain * m * effectiveA - 0.22 * b
            val dP = 0.52 * b - 0.28 * p
            val dAb = 0.88 * p - 0.12 * ab
            val dM = 0.09 * (t + b) - 0.015 * m
            val tolPenalty = max(0.0, 1.0 - params.autoTolerance)
            val excessCyt = max(0.0, c - params.autoTolerance * 1.6)
            val dD = (0.14 * a + 0.28 * excessCyt) * (1.0 + 1.5 * tolPenalty) - 0.18 * d

            gcIntegral += (b * (0.35 + t)) * dt

            a = min(10.0, max(0.0, a + dA * dt))
            i = min(8.0, max(0.05, i + dI * dt))
            c = min(8.0, max(0.0, c + dC * dt))
            t = min(8.0, max(0.0, t + dT * dt))
            b = min(8.0, max(0.0, b + dB * dt))
            p = min(8.0, max(0.0, p + dP * dt))
            ab = min(12.0, max(0.0, ab + dAb * dt))
            m = min(5.0, max(0.0, m + dM * dt))
            d = min(10.0, max(0.0, d + dD * dt))
        }

        if (events.isEmpty()) {
            events += "Sub-threshold antigenic stimulation resolved smoothly."
        }

        val finalStep = out.lastOrNull()
        val effToAg = if (finalStep != null) {
            (finalStep.tCell + finalStep.antibody * 0.5) / max(0.05, finalStep.antigen)
        } else 0.0

        val crsRisk = when {
            maxC >= 3.0 || maxD >= 2.2 -> "Severe CRS Risk"
            maxC >= 1.9 || maxD >= 1.2 -> "Elevated"
            maxC >= 1.0 -> "Moderate"
            else -> "Low"
        }

        val synthNote = when {
            params.autoTolerance < 0.5 ->
                "Tolerance loss (τ=${"%.2f".format(params.autoTolerance)}) amplifies cytokine-mediated collateral damage (AUC Damage ${"%.1f".format(aucD)}) even as antigen wanes."
            tClear != null && maxM > 0.35 ->
                "Sterile clearance achieved by Day ${"%.1f".format(tClear)} with ${"%.2f".format(maxAff)}x affinity index and durable memory reserve (${"%.2f".format(maxM)})."
            tClear == null && aucA > 20.0 ->
                "Incomplete pathogen eradication (Ag AUC ${"%.1f".format(aucA)}); prolonged innate sensing sustains inflammatory stress."
            else ->
                "Balanced innate-to-adaptive handoff reached peak antibody ${"%.2f".format(maxAb)} with $crsRisk inflammatory load."
        }

        return SimResult(
            params = params,
            series = out,
            peakAntigen = maxA,
            peakCytokine = maxC,
            peakAntibody = maxAb,
            peakMemory = maxM,
            peakDamage = maxD,
            events = events,
            peakTCell = maxT,
            peakBCell = maxB,
            peakPlasma = maxP,
            peakAffinityMaturation = maxAff,
            timeTo50ClearanceDays = t50Clear,
            timeToClearanceDays = tClear,
            aucAntigen = aucA,
            aucCytokine = aucC,
            aucDamage = aucD,
            effectorToPathogenRatio = effToAg,
            cytokineStormRisk = crsRisk,
            mechanisticNote = synthNote
        )
    }

    fun toCsv(result: SimResult): String = buildString {
        appendLine("day,phase,antigen,innate,apc_activation,complement_flux,cytokine,t_cell,treg_suppression,b_cell,plasma,antibody,affinity_maturation,phagocytic_clearance,memory,damage")
        for (s in result.series) {
            appendLine(
                "${"%.2f".format(s.day)},\"${s.phaseTag}\",${"%.4f".format(s.antigen)},${"%.4f".format(s.innate)}," +
                    "${"%.4f".format(s.apcActivation)},${"%.4f".format(s.complementFlux)},${"%.4f".format(s.cytokine)}," +
                    "${"%.4f".format(s.tCell)},${"%.4f".format(s.tregSuppression)},${"%.4f".format(s.bCell)}," +
                    "${"%.4f".format(s.plasma)},${"%.4f".format(s.antibody)},${"%.4f".format(s.affinityMaturation)}," +
                    "${"%.4f".format(s.phagocyticClearance)},${"%.4f".format(s.memory)},${"%.4f".format(s.damage)}"
            )
        }
    }
}
