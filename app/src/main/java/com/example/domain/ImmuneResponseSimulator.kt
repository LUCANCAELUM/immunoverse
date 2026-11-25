package com.example.domain

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * Parameterizes the activation responsiveness (gain) and baseline priming levels
 * of major innate and adaptive immune cell lineages.
 */
data class ImmuneCellActivationProfile(
    val macrophageNeutrophilLevel: Double = 1.0, // Phagocytosis & ROS innate response (0.1..2.5)
    val dendriticApcLevel: Double = 1.0,         // Antigen presentation & co-stimulation (0.1..2.5)
    val cd4HelperActivation: Double = 1.0,       // Th1/Tfh cytokine help & B-cell licensing (0.1..2.5)
    val cd8CytotoxicActivation: Double = 1.0,    // CTL killing of infected host targets (0.1..2.5)
    val bCellPlasmaActivation: Double = 1.0,     // Antibody-secreting plasma cell output (0.1..2.5)
    val tregSuppressionLevel: Double = 1.0,      // Peripheral Treg brake preventing cytokine storm (0.1..2.0)
    val baselineMemoryPool: Double = 0.05        // Pre-existing antigen-specific memory (0.0..1.5)
) {
    val compositeActivationIndex: Double
        get() = (macrophageNeutrophilLevel + dendriticApcLevel + cd4HelperActivation +
            cd8CytotoxicActivation + bCellPlasmaActivation) / 5.0
}

/**
 * Parameterizes initial and dynamic pathogen burden characteristics.
 */
data class PathogenLoadParameters(
    val initialPathogenLoad: Double = 1.25,      // Initial infectious inoculum (0.0..8.0)
    val replicationRate: Double = 0.54,          // Intrinsic exponential growth factor (0.0..1.5)
    val carryingCapacity: Double = 8.5,          // Tissue niche saturation ceiling
    val immuneEvasionFactor: Double = 0.15       // Inhibits early PRR / MHC signaling (0.0..0.85)
)

/**
 * Snapshot of the simulated host-pathogen state at a given time point.
 */
data class ImmuneSimulationState(
    val day: Double,
    val pathogenLoad: Double,
    val innateCellActivation: Double,
    val apcActivation: Double,
    val cd4HelperActivation: Double,
    val cd8CytotoxicActivation: Double,
    val bCellActivation: Double,
    val antibodyTiter: Double,
    val cytokineLoad: Double,
    val memoryPool: Double,
    val tissueStress: Double,
    val clearanceFraction: Double,
    val phase: String
)

data class ImmuneSimulationReport(
    val outcomeLabel: String,
    val dominantAxis: String,
    val peakPathogenLoad: Double,
    val finalPathogenLoad: Double,
    val peakInnateActivation: Double,
    val peakCd8Activation: Double,
    val peakBCellActivation: Double,
    val peakAntibodyTiter: Double,
    val peakCytokineLoad: Double,
    val timeToClearanceDays: Double?,
    val cytokineStormRisk: String
)

/**
 * Stateful simulation handler class for modeling coupled pathogen-load kinetics
 * and multi-lineage immune cell activation dynamics.
 *
 * Supports both step-by-step live modulation (e.g., mid-infection rechallenge or
 * immunomodulatory intervention) and full time-course execution.
 */
class ImmuneResponseSimulator(
    initialPathogenParams: PathogenLoadParameters = PathogenLoadParameters(),
    initialCellActivation: ImmuneCellActivationProfile = ImmuneCellActivationProfile()
) {
    var pathogenParams: PathogenLoadParameters = initialPathogenParams
        private set

    var cellActivationProfile: ImmuneCellActivationProfile = initialCellActivation
        private set

    private val internalHistory = mutableListOf<ImmuneSimulationState>()
    val history: List<ImmuneSimulationState>
        get() = internalHistory

    var currentState: ImmuneSimulationState = createInitialState()
        private set

    private var peakLoadObserved: Double = initialPathogenParams.initialPathogenLoad.coerceAtLeast(0.0)

    init {
        internalHistory += currentState
    }

    private fun createInitialState(): ImmuneSimulationState {
        val initLoad = pathogenParams.initialPathogenLoad.coerceAtLeast(0.0)
        val initInnate = (0.15 * cellActivationProfile.macrophageNeutrophilLevel).coerceIn(0.02, 2.0)
        val initMem = cellActivationProfile.baselineMemoryPool.coerceIn(0.0, 2.0)
        val initAb = if (initMem > 0.25) 0.35 * cellActivationProfile.bCellPlasmaActivation else 0.02
        return ImmuneSimulationState(
            day = 0.0,
            pathogenLoad = initLoad,
            innateCellActivation = initInnate,
            apcActivation = 0.05 * cellActivationProfile.dendriticApcLevel,
            cd4HelperActivation = 0.03 * cellActivationProfile.cd4HelperActivation,
            cd8CytotoxicActivation = 0.02 * cellActivationProfile.cd8CytotoxicActivation,
            bCellActivation = 0.02 * cellActivationProfile.bCellPlasmaActivation,
            antibodyTiter = initAb,
            cytokineLoad = 0.04,
            memoryPool = initMem,
            tissueStress = 0.0,
            clearanceFraction = 0.0,
            phase = "Phase I: Initial Inoculation & PRR Sensing"
        )
    }

    fun reset(
        newPathogenParams: PathogenLoadParameters = this.pathogenParams,
        newCellActivation: ImmuneCellActivationProfile = this.cellActivationProfile
    ) {
        this.pathogenParams = newPathogenParams
        this.cellActivationProfile = newCellActivation
        this.peakLoadObserved = newPathogenParams.initialPathogenLoad.coerceAtLeast(0.0)
        this.internalHistory.clear()
        this.currentState = createInitialState()
        this.internalHistory += currentState
    }

    /**
     * Dynamically injects additional pathogen burden (e.g., secondary re-exposure or co-infection).
     */
    fun boostPathogenLoad(additionalLoad: Double) {
        val nextLoad = (currentState.pathogenLoad + additionalLoad).coerceIn(0.0, 12.0)
        peakLoadObserved = max(peakLoadObserved, nextLoad)
        currentState = currentState.copy(
            pathogenLoad = nextLoad,
            phase = "Secondary Antigenic Rechallenge"
        )
    }

    /**
     * Dynamically modulates immune cell activation gains mid-simulation
     * (e.g., checkpoint blockade, cytokine boost, or Treg suppression).
     */
    fun modulateImmuneCellActivation(
        transform: (ImmuneCellActivationProfile) -> ImmuneCellActivationProfile
    ) {
        cellActivationProfile = transform(cellActivationProfile)
    }

    /**
     * Advances the simulation by [dtDays] using sub-stepped Euler integration
     * for numerical stability.
     */
    fun step(dtDays: Double = 0.1, subSteps: Int = 5): ImmuneSimulationState {
        val h = (dtDays / subSteps.coerceAtLeast(1)).coerceIn(0.002, 0.05)
        var s = currentState

        repeat(subSteps.coerceAtLeast(1)) {
            val act = cellActivationProfile
            val pat = pathogenParams
            val evasionDamp = (1.0 - pat.immuneEvasionFactor.coerceIn(0.0, 0.85))
            val sensedPathogen = s.pathogenLoad * evasionDamp

            val cap = pat.carryingCapacity.coerceAtLeast(1.0)
            val growth = pat.replicationRate * s.pathogenLoad * (1.0 - s.pathogenLoad / cap)
            val killing = (
                0.34 * act.macrophageNeutrophilLevel * s.innateCellActivation +
                    0.46 * act.cd8CytotoxicActivation * s.cd8CytotoxicActivation +
                    0.50 * s.antibodyTiter
                ) * s.pathogenLoad

            val dPathogen = growth - killing

            val dInnate = 0.90 * act.macrophageNeutrophilLevel * (sensedPathogen / (0.45 + sensedPathogen)) -
                0.38 * (s.innateCellActivation - 0.12 * act.macrophageNeutrophilLevel)

            val apcSignal = ((s.innateCellActivation * sensedPathogen) / (0.55 + s.innateCellActivation * sensedPathogen)) *
                act.dendriticApcLevel
            val dApc = 1.25 * (apcSignal - s.apcActivation)

            val dCd4 = 0.65 * act.cd4HelperActivation * s.apcActivation +
                0.70 * act.cd4HelperActivation * s.memoryPool * sensedPathogen -
                0.24 * s.cd4HelperActivation

            val dCd8 = 0.72 * act.cd8CytotoxicActivation * s.apcActivation * (0.4 + 0.6 * s.cd4HelperActivation) +
                0.85 * act.cd8CytotoxicActivation * s.memoryPool * sensedPathogen -
                0.26 * s.cd8CytotoxicActivation

            val dBCell = 0.60 * act.bCellPlasmaActivation * sensedPathogen * (0.3 + s.cd4HelperActivation) +
                0.92 * act.bCellPlasmaActivation * s.memoryPool * sensedPathogen -
                0.22 * s.bCellActivation

            val dAb = 0.88 * s.bCellActivation - 0.11 * s.antibodyTiter

            val tregBrake = 1.0 / (0.45 + 0.55 * act.tregSuppressionLevel)
            val dCytokine = (0.68 * s.innateCellActivation * sensedPathogen + 0.45 * s.cd4HelperActivation * s.pathogenLoad) *
                tregBrake - 0.58 * s.cytokineLoad

            val dMemory = 0.085 * (s.cd4HelperActivation + s.cd8CytotoxicActivation + s.bCellActivation) -
                0.012 * s.memoryPool

            val excessCyt = max(0.0, s.cytokineLoad - act.tregSuppressionLevel * 1.45)
            val dStress = (0.14 * s.pathogenLoad + 0.30 * excessCyt) * (1.0 + max(0.0, 1.0 - act.tregSuppressionLevel)) -
                0.18 * s.tissueStress

            val nextDay = s.day + h
            val nextLoad = (s.pathogenLoad + dPathogen * h).coerceIn(0.0, 12.0)
            peakLoadObserved = max(peakLoadObserved, nextLoad)
            val nextInnate = (s.innateCellActivation + dInnate * h).coerceIn(0.05, 8.0)
            val nextApc = (s.apcActivation + dApc * h).coerceIn(0.0, 4.0)
            val nextCd4 = (s.cd4HelperActivation + dCd4 * h).coerceIn(0.0, 8.0)
            val nextCd8 = (s.cd8CytotoxicActivation + dCd8 * h).coerceIn(0.0, 8.0)
            val nextB = (s.bCellActivation + dBCell * h).coerceIn(0.0, 8.0)
            val nextAb = (s.antibodyTiter + dAb * h).coerceIn(0.0, 12.0)
            val nextCyt = (s.cytokineLoad + dCytokine * h).coerceIn(0.0, 10.0)
            val nextMem = (s.memoryPool + dMemory * h).coerceIn(0.0, 6.0)
            val nextStress = (s.tissueStress + dStress * h).coerceIn(0.0, 10.0)

            val clearanceFrac = if (peakLoadObserved > 0.05) {
                (1.0 - nextLoad / peakLoadObserved).coerceIn(0.0, 1.0)
            } else 1.0

            val phaseLabel = when {
                nextCyt > 2.4 && act.tregSuppressionLevel < 0.6 -> "Hyper-Inflammatory Cytokine Surge"
                nextLoad < 0.05 && nextAb > 0.4 -> "Phase V: Sterile Clearance & Memory"
                nextAb > 0.7 && nextLoad < peakLoadObserved * 0.65 -> "Phase IV: Humoral Neutralization"
                nextCd8 > 0.4 || nextB > 0.35 -> "Phase III: Effector Clonal Expansion"
                nextApc > 0.25 -> "Phase II: Dendritic APC Priming"
                else -> "Phase I: Innate Recognition"
            }

            s = ImmuneSimulationState(
                day = nextDay,
                pathogenLoad = nextLoad,
                innateCellActivation = nextInnate,
                apcActivation = nextApc,
                cd4HelperActivation = nextCd4,
                cd8CytotoxicActivation = nextCd8,
                bCellActivation = nextB,
                antibodyTiter = nextAb,
                cytokineLoad = nextCyt,
                memoryPool = nextMem,
                tissueStress = nextStress,
                clearanceFraction = clearanceFrac,
                phase = phaseLabel
            )
        }

        currentState = s
        internalHistory += s
        return s
    }

    /**
     * Runs the simulation forward until [targetDurationDays] is reached.
     */
    fun runForDuration(targetDurationDays: Double = 28.0, stepSizeDays: Double = 0.25): ImmuneSimulationReport {
        val clampedTarget = targetDurationDays.coerceIn(1.0, 90.0)
        val dt = stepSizeDays.coerceIn(0.05, 1.0)
        while (currentState.day + 1e-6 < clampedTarget) {
            val rem = min(dt, clampedTarget - currentState.day)
            step(rem)
        }
        return generateReport()
    }

    fun generateReport(): ImmuneSimulationReport {
        val peakPath = internalHistory.maxOfOrNull { it.pathogenLoad } ?: currentState.pathogenLoad
        val peakInnate = internalHistory.maxOfOrNull { it.innateCellActivation } ?: currentState.innateCellActivation
        val peakCd8 = internalHistory.maxOfOrNull { it.cd8CytotoxicActivation } ?: currentState.cd8CytotoxicActivation
        val peakB = internalHistory.maxOfOrNull { it.bCellActivation } ?: currentState.bCellActivation
        val peakAb = internalHistory.maxOfOrNull { it.antibodyTiter } ?: currentState.antibodyTiter
        val peakCyt = internalHistory.maxOfOrNull { it.cytokineLoad } ?: currentState.cytokineLoad
        val peakStress = internalHistory.maxOfOrNull { it.tissueStress } ?: currentState.tissueStress

        val clearTime = internalHistory.firstOrNull {
            it.day > 1.0 && peakPath > 0.25 && it.pathogenLoad < 0.05
        }?.day

        val stormRisk = when {
            peakCyt >= 2.8 || peakStress >= 2.2 -> "High CRS Risk"
            peakCyt >= 1.6 -> "Moderate Inflammation"
            else -> "Controlled"
        }

        val dominant = when {
            peakCd8 >= peakB && peakCd8 >= peakInnate -> "Cell-Mediated CD8+ Cytotoxic T-Cell Axis"
            peakAb >= peakCd8 -> "Humoral B-Cell & Neutralizing Antibody Axis"
            else -> "Innate Macrophage / Neutrophil Containment Axis"
        }

        val outcome = when {
            clearTime != null && stormRisk == "Controlled" ->
                "Sterile pathogen resolution by Day ${"%.1f".format(clearTime)} with controlled inflammation."
            clearTime != null ->
                "Pathogen cleared by Day ${"%.1f".format(clearTime)}, accompanied by $stormRisk cytokine burden."
            currentState.pathogenLoad > peakPath * 0.5 ->
                "Persistent pathogen load (${"%.2f".format(currentState.pathogenLoad)}); insufficient adaptive activation."
            else ->
                "Partial containment underway (${"%.0f".format(currentState.clearanceFraction * 100)}% cleared)."
        }

        return ImmuneSimulationReport(
            outcomeLabel = outcome,
            dominantAxis = dominant,
            peakPathogenLoad = peakPath,
            finalPathogenLoad = currentState.pathogenLoad,
            peakInnateActivation = peakInnate,
            peakCd8Activation = peakCd8,
            peakBCellActivation = peakB,
            peakAntibodyTiter = peakAb,
            peakCytokineLoad = peakCyt,
            timeToClearanceDays = clearTime,
            cytokineStormRisk = stormRisk
        )
    }

    /**
     * Converts current parameters into [SimParams] for compatibility with [ImmuneSimulatorEngine].
     */
    fun toSimParams(durationDays: Double = 30.0): SimParams = SimParams(
        presetKey = "custom_controller",
        antigenLoad = pathogenParams.initialPathogenLoad,
        innateGain = cellActivationProfile.macrophageNeutrophilLevel,
        tCellGain = (cellActivationProfile.cd4HelperActivation + cellActivationProfile.cd8CytotoxicActivation) * 0.5,
        bCellGain = cellActivationProfile.bCellPlasmaActivation,
        abClear = (1.0 - pathogenParams.immuneEvasionFactor * 0.4).coerceIn(0.3, 1.8),
        autoTolerance = cellActivationProfile.tregSuppressionLevel,
        durationDays = durationDays,
        initMemory = cellActivationProfile.baselineMemoryPool,
        antigenGrowthRate = pathogenParams.replicationRate
    )
}
