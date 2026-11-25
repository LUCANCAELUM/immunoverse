package com.example.domain

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

enum class PathogenClass(val label: String, val standardDrugClass: String) {
    ENVELOPED_RNA_VIRUS("Enveloped +ssRNA Virus", "Polymerase / Protease Antiviral"),
    SEGMENTED_RNA_VIRUS("Segmented -ssRNA Virus", "Neuraminidase / Endonuclease Inhibitor"),
    RETROVIRUS("Retrovirus (ssRNA-RT)", "cART (INSTI + NRTI)"),
    INTRACELLULAR_BACTERIUM("Intracellular Bacterium", "Multi-Drug Antimycobacterial"),
    EXTRACELLULAR_BACTERIUM("Extracellular Bacterium", "Targeted Beta-Lactam / Glycopeptide")
}

enum class TropismTarget(val label: String, val organSite: String, val clearanceDifficulty: Double) {
    UPPER_RESPIRATORY("ACE2 / Upper Airway", "Nasopharynx & Bronchi", 0.85),
    LOWER_ALVEOLAR("Alveolar Type II / Lung", "Deep Lung Parenchyma", 1.25),
    CD4_LYMPHOID("CD4+ CCR5/CXCR4", "Lymphoid T-Cell Pool", 1.45),
    MACROPHAGE_NICHE("Alveolar Macrophage Phagosome", "Granuloma / Lung", 1.35),
    SYSTEMIC_ENDOTHELIUM("Systemic Vascular / Skin", "Bloodstream & Endothelium", 1.20)
}

data class PathogenDesign(
    val id: String = "custom",
    val name: String = "Custom Variant Alpha",
    val pathogenClass: PathogenClass = PathogenClass.ENVELOPED_RNA_VIRUS,
    val tropism: TropismTarget = TropismTarget.UPPER_RESPIRATORY,
    val replicationRate: Double = 1.15,     // 0.4..2.2
    val receptorAffinity: Double = 1.10,    // 0.4..2.2 (drives R_eff & entry)
    val immuneEvasion: Double = 0.55,       // 0.0..1.5 (suppresses early IFN-I)
    val antigenicDrift: Double = 0.45,      // 0.0..1.5 (escapes prior memory)
    val cytokineTrigger: Double = 0.60,     // 0.1..1.8 (drives IL-6 / fever / immunopathology)
    val tissueInvasiveness: Double = 0.50,  // 0.1..1.6 (epithelial barrier disruption)
    val referenceNotes: String = ""
)

data class VirtualHostProfile(
    val key: String,
    val label: String,
    val summary: String,
    val ageYears: Int,
    val mucosalBarrier: Double,       // 0.4..1.5
    val innateIfnSpeed: Double,       // 0.3..1.6
    val priorMemoryIgG: Double,       // 0.0..1.5
    val adaptiveCompetence: Double,   // 0.2..1.5
    val tregAntiInflammatory: Double  // 0.3..1.5
)

data class TreatmentStrategy(
    val useDirectAntiviralOrAntibiotic: Boolean = false,
    val useMonoclonalAntibody: Boolean = false,
    val useHostImmunomodulator: Boolean = false,
    val useSupportiveCare: Boolean = true,
    val interventionDay: Double = 2.0 // 0.0..10.0
)

data class HostSimStep(
    val day: Double,
    val pathogenLoadNorm: Double,
    val log10ViralLoad: Double,
    val sheddingReff: Double,
    val innateIfn: Double,
    val neutralizingAb: Double,
    val effectorCtl: Double,
    val il6Cytokine: Double,
    val organStressPct: Double,
    val feverCelsius: Double
)

data class HostInfectionResult(
    val series: List<HostSimStep>,
    val untreatedSeries: List<HostSimStep>,
    val peakLog10Load: Double,
    val peakDay: Double,
    val incubationDays: Double,
    val infectiousWindowDays: Double,
    val effectiveR0: Double,
    val peakFeverC: Double,
    val maxOrganStressPct: Double,
    val untreatedMaxStressPct: Double,
    val burdenReductionPct: Double,
    val severityTier: String,
    val clinicalOutcome: String,
    val closestRealStrain: String,
    val strategyAnalysis: List<String>
)

object PathogenSimulatorEngine {
    val REFERENCE_STRAINS: List<PathogenDesign> = listOf(
        PathogenDesign(
            id = "scov2_wuhan",
            name = "SARS-CoV-2 (Ancestral Wuhan-Hu-1)",
            pathogenClass = PathogenClass.ENVELOPED_RNA_VIRUS,
            tropism = TropismTarget.LOWER_ALVEOLAR,
            replicationRate = 1.05,
            receptorAffinity = 0.95,
            immuneEvasion = 0.65,
            antigenicDrift = 0.15,
            cytokineTrigger = 0.90,
            tissueInvasiveness = 0.78,
            referenceNotes = "Ancestral D614 strain: ~5 day incubation, moderate ACE2 affinity, significant lower-lung IL-6/macrophage activation in naive hosts."
        ),
        PathogenDesign(
            id = "scov2_delta",
            name = "SARS-CoV-2 (Delta B.1.617.2)",
            pathogenClass = PathogenClass.ENVELOPED_RNA_VIRUS,
            tropism = TropismTarget.LOWER_ALVEOLAR,
            replicationRate = 1.48,
            receptorAffinity = 1.40,
            immuneEvasion = 0.80,
            antigenicDrift = 0.55,
            cytokineTrigger = 1.15,
            tissueInvasiveness = 1.05,
            referenceNotes = "Furin cleavage P681R & L452R: rapid peak viral load (~day 3.5-4), higher syncytia formation & alveolar severity."
        ),
        PathogenDesign(
            id = "scov2_omicron",
            name = "SARS-CoV-2 (Omicron JN.1)",
            pathogenClass = PathogenClass.ENVELOPED_RNA_VIRUS,
            tropism = TropismTarget.UPPER_RESPIRATORY,
            replicationRate = 1.55,
            receptorAffinity = 1.75,
            immuneEvasion = 0.92,
            antigenicDrift = 1.32,
            cytokineTrigger = 0.52,
            tissueInvasiveness = 0.42,
            referenceNotes = "High upper-airway tropism & marked spike RBD antigenic escape (>30 mutations), faster transmission with lower deep-alveolar fusogenicity."
        ),
        PathogenDesign(
            id = "flu_h1n1",
            name = "Influenza A (H1N1 pdm09)",
            pathogenClass = PathogenClass.SEGMENTED_RNA_VIRUS,
            tropism = TropismTarget.UPPER_RESPIRATORY,
            replicationRate = 1.32,
            receptorAffinity = 1.15,
            immuneEvasion = 0.50,
            antigenicDrift = 0.40,
            cytokineTrigger = 0.78,
            tissueInvasiveness = 0.65,
            referenceNotes = "Rapid 1.5-2 day incubation; NS1 antagonizes RIG-I/IFN-I; responsive to neuraminidase/cap-dependent endonuclease inhibitors within 48h."
        ),
        PathogenDesign(
            id = "flu_h3n2",
            name = "Influenza A (H3N2 High-Drift)",
            pathogenClass = PathogenClass.SEGMENTED_RNA_VIRUS,
            tropism = TropismTarget.LOWER_ALVEOLAR,
            replicationRate = 1.28,
            receptorAffinity = 1.25,
            immuneEvasion = 0.68,
            antigenicDrift = 1.05,
            cytokineTrigger = 0.92,
            tissueInvasiveness = 0.80,
            referenceNotes = "Faster HA antigenic drift causing seasonal vaccine mismatch and steeper morbidity in older adults."
        ),
        PathogenDesign(
            id = "hiv1_cladeb",
            name = "HIV-1 (Clade B R5-Tropic)",
            pathogenClass = PathogenClass.RETROVIRUS,
            tropism = TropismTarget.CD4_LYMPHOID,
            replicationRate = 0.85,
            receptorAffinity = 1.00,
            immuneEvasion = 1.38,
            antigenicDrift = 1.42,
            cytokineTrigger = 0.45,
            tissueInvasiveness = 0.95,
            referenceNotes = "Integrates proviral DNA in CD4+ T cells; extreme Env glycan shield & hypervariability defeat natural neutralizing clearance without cART."
        ),
        PathogenDesign(
            id = "mtb_wt",
            name = "Mycobacterium tuberculosis",
            pathogenClass = PathogenClass.INTRACELLULAR_BACTERIUM,
            tropism = TropismTarget.MACROPHAGE_NICHE,
            replicationRate = 0.55,
            receptorAffinity = 0.85,
            immuneEvasion = 1.20,
            antigenicDrift = 0.15,
            cytokineTrigger = 0.72,
            tissueInvasiveness = 0.90,
            referenceNotes = "Slow doubling time; blocks phagolysosome fusion inside alveolar macrophages, requiring Th1 IFN-γ + multi-drug therapy."
        ),
        PathogenDesign(
            id = "mrsa_usa300",
            name = "MRSA (S. aureus USA300)",
            pathogenClass = PathogenClass.EXTRACELLULAR_BACTERIUM,
            tropism = TropismTarget.SYSTEMIC_ENDOTHELIUM,
            replicationRate = 1.35,
            receptorAffinity = 1.05,
            immuneEvasion = 0.95,
            antigenicDrift = 0.25,
            cytokineTrigger = 1.25,
            tissueInvasiveness = 1.15,
            referenceNotes = "Protein A & superantigens trigger intense cytokine cascade and endothelial invasion; requires targeted non-beta-lactam antimicrobials."
        )
    )

    val HOST_PROFILES: List<VirtualHostProfile> = listOf(
        VirtualHostProfile(
            key = "healthy_adult",
            label = "Healthy Adult (30y)",
            summary = "Intact mucosal IgA barrier, brisk Type-I IFN sensing, balanced Treg control.",
            ageYears = 30,
            mucosalBarrier = 1.05,
            innateIfnSpeed = 1.10,
            priorMemoryIgG = 0.15,
            adaptiveCompetence = 1.10,
            tregAntiInflammatory = 1.05
        ),
        VirtualHostProfile(
            key = "vaccinated_hybrid",
            label = "Vaccinated / Hybrid Memory",
            summary = "Pre-formed cross-reactive memory B/T cells accelerate neutralizing IgG & CTL recall by 3–4 days.",
            ageYears = 35,
            mucosalBarrier = 1.15,
            innateIfnSpeed = 1.15,
            priorMemoryIgG = 1.18,
            adaptiveCompetence = 1.28,
            tregAntiInflammatory = 1.10
        ),
        VirtualHostProfile(
            key = "older_inflammaging",
            label = "Older Adult (72y)",
            summary = "Delayed naive T-cell priming, blunted early IFN-I, and higher baseline IL-6 inflammaging.",
            ageYears = 72,
            mucosalBarrier = 0.78,
            innateIfnSpeed = 0.65,
            priorMemoryIgG = 0.20,
            adaptiveCompetence = 0.68,
            tregAntiInflammatory = 0.65
        ),
        VirtualHostProfile(
            key = "immunocompromised",
            label = "Immunocompromised Host",
            summary = "Depleted CD4/B-cell compartment; prolonged pathogen shedding & high risk of persistent infection.",
            ageYears = 54,
            mucosalBarrier = 0.72,
            innateIfnSpeed = 0.58,
            priorMemoryIgG = 0.05,
            adaptiveCompetence = 0.30,
            tregAntiInflammatory = 0.80
        ),
        VirtualHostProfile(
            key = "hyperinflammatory",
            label = "Metabolic / Hyper-Inflammatory",
            summary = "Endothelial vulnerability and low Treg brake increase cytokine storm risk upon high viral load.",
            ageYears = 58,
            mucosalBarrier = 0.82,
            innateIfnSpeed = 0.90,
            priorMemoryIgG = 0.10,
            adaptiveCompetence = 0.88,
            tregAntiInflammatory = 0.45
        )
    )

    fun runHostInfectionSim(
        pathogen: PathogenDesign,
        host: VirtualHostProfile,
        treatment: TreatmentStrategy
    ): HostInfectionResult {
        val treated = simulateSeries(pathogen, host, treatment)
        val untreated = simulateSeries(pathogen, host, TreatmentStrategy(useSupportiveCare = false))

        val peakStep = treated.maxByOrNull { it.log10ViralLoad } ?: treated.first()
        val peakLog10 = peakStep.log10ViralLoad
        val peakDay = peakStep.day
        val peakFever = treated.maxOf { it.feverCelsius }
        val maxStress = treated.maxOf { it.organStressPct }
        val untreatedMaxStress = untreated.maxOf { it.organStressPct }

        val incubation = treated.firstOrNull { it.log10ViralLoad >= 3.2 || it.feverCelsius >= 37.6 }?.day
            ?: (5.5 / pathogen.replicationRate.coerceAtLeast(0.4))
        val infectiousDays = treated.count { it.log10ViralLoad >= 4.0 } * 0.25

        // Effective R_eff reflects receptor affinity, replication, evasion, minus host immunity & treatment
        val escapeFactor = (1.0 + 0.45 * pathogen.antigenicDrift) / (1.0 + host.priorMemoryIgG * 0.85)
        val drugDamp = (if (treatment.useDirectAntiviralOrAntibiotic) 0.58 else 1.0) *
            (if (treatment.useMonoclonalAntibody) 0.68 else 1.0)
        val rawReff = (1.1 + 1.85 * pathogen.receptorAffinity * pathogen.replicationRate * escapeFactor) *
            (1.15 - 0.25 * host.mucosalBarrier) * drugDamp
        val effectiveR0 = rawReff.coerceIn(0.25, 16.5)

        val untreatedAuc = untreated.sumOf { it.pathogenLoadNorm } * 0.25
        val treatedAuc = treated.sumOf { it.pathogenLoadNorm } * 0.25
        val burdenReduction = if (untreatedAuc > 0.01) {
            (((untreatedAuc - treatedAuc) / untreatedAuc) * 100.0).coerceIn(0.0, 99.0)
        } else 0.0

        val severityTier = when {
            maxStress >= 68.0 || peakFever >= 39.8 -> "Critical (ARDS / Systemic)"
            maxStress >= 42.0 || peakFever >= 39.0 -> "Severe (Hospital Care)"
            maxStress >= 20.0 || peakFever >= 38.0 -> "Moderate (Symptomatic)"
            maxStress >= 7.0 || peakLog10 >= 3.5 -> "Mild (Upper/Contained)"
            else -> "Subclinical / Cleared Early"
        }

        val endLoad = treated.last().log10ViralLoad
        val outcome = when {
            endLoad < 1.5 && maxStress < 25.0 ->
                "Sterile mucosal & adaptive clearance by Day ${"%.1f".format(treated.firstOrNull { it.day > peakDay && it.log10ViralLoad < 2.0 }?.day ?: 28.0)} with durable memory."
            endLoad < 2.2 && maxStress >= 25.0 ->
                "Pathogen cleared, but collateral epithelial/cytokine injury peaked at ${"%.0f".format(maxStress)}% organ stress."
            else ->
                "Persistent/chronic reservoir (${"%.1f".format(endLoad)} log10) due to high immune evasion or suboptimal clearance; requires combination therapy."
        }

        val closest = findClosestReferenceStrain(pathogen)
        val critique = buildStrategyCritique(pathogen, host, treatment, maxStress, untreatedMaxStress, peakDay)

        return HostInfectionResult(
            series = treated,
            untreatedSeries = untreated,
            peakLog10Load = peakLog10,
            peakDay = peakDay,
            incubationDays = incubation.coerceIn(1.0, 14.0),
            infectiousWindowDays = infectiousDays,
            effectiveR0 = effectiveR0,
            peakFeverC = peakFever,
            maxOrganStressPct = maxStress,
            untreatedMaxStressPct = untreatedMaxStress,
            burdenReductionPct = burdenReduction,
            severityTier = severityTier,
            clinicalOutcome = outcome,
            closestRealStrain = closest,
            strategyAnalysis = critique
        )
    }

    private fun simulateSeries(
        p: PathogenDesign,
        h: VirtualHostProfile,
        tx: TreatmentStrategy
    ): List<HostSimStep> {
        val dt = 0.25
        val steps = (28.0 / dt).toInt()
        val out = ArrayList<HostSimStep>(steps + 1)

        // Initial inoculum damped by mucosal barrier
        var load = (0.14 / h.mucosalBarrier.coerceIn(0.4, 1.5)).coerceIn(0.04, 0.45)
        var ifn = 0.08 * h.innateIfnSpeed
        // Memory IgG partially escapes if pathogen antigenicDrift is high
        val effectiveMemory = (h.priorMemoryIgG * (1.0 - 0.58 * p.antigenicDrift)).coerceAtLeast(0.0)
        var ab = 0.05 + 0.55 * effectiveMemory
        var ctl = 0.03 + 0.30 * h.priorMemoryIgG * 0.75
        var il6 = 0.06
        var damage = 0.0

        for (i in 0..steps) {
            val day = i * dt
            val txActive = day >= tx.interventionDay

            val drugClearBonus = if (txActive && tx.useDirectAntiviralOrAntibiotic) {
                when (p.pathogenClass) {
                    PathogenClass.RETROVIRUS -> 1.15
                    PathogenClass.INTRACELLULAR_BACTERIUM -> 0.58
                    else -> 0.92
                }
            } else 0.0

            val mabClearBonus = if (txActive && tx.useMonoclonalAntibody) {
                (0.75 * (1.0 - 0.35 * p.antigenicDrift)).coerceAtLeast(0.22)
            } else 0.0

            val steroidDamp = if (txActive && tx.useHostImmunomodulator) 0.52 else 1.0
            val supportiveProtect = if (txActive && tx.useSupportiveCare) 0.78 else 1.0

            val log10Load = if (load <= 0.0005) 0.0 else (2.0 + 2.4 * ln(1.0 + load * 12.0)).coerceIn(0.0, 9.6)
            val reffInstant = (p.receptorAffinity * (load / (load + 0.35)) * 4.2 * (1.0 - 0.35 * ab.coerceAtMost(1.5))).coerceIn(0.0, 14.0)
            val organStressPct = (damage * 100.0).coerceIn(0.0, 98.0)
            val fever = (36.8 + (il6 * 1.45) + (load * 0.35)).coerceIn(36.6, 40.9)

            out += HostSimStep(
                day = day,
                pathogenLoadNorm = load,
                log10ViralLoad = log10Load,
                sheddingReff = reffInstant,
                innateIfn = ifn,
                neutralizingAb = ab,
                effectorCtl = ctl,
                il6Cytokine = il6,
                organStressPct = organStressPct,
                feverCelsius = fever
            )

            // Coupled Host-Pathogen ODE
            val growth = (0.72 * p.replicationRate * (0.65 + 0.45 * p.receptorAffinity)) * load * (1.0 - load / 5.5)
            val innateSuppress = (0.38 * ifn * (1.0 / (1.0 + 0.75 * p.immuneEvasion))) * load
            val adaptiveKill = (0.48 * ab + 0.44 * ctl) * load
            val therapeuticKill = (drugClearBonus + mabClearBonus) * load
            val dLoad = growth - innateSuppress - adaptiveKill - therapeuticKill

            val ifnDrive = (0.85 * h.innateIfnSpeed * load) / (1.0 + 0.85 * p.immuneEvasion)
            val dIfn = ifnDrive - 0.36 * ifn

            val adaptiveDelay = if (day < 2.8 && effectiveMemory < 0.25) 0.25 else 1.0
            val dCtl = adaptiveDelay * (0.42 * h.adaptiveCompetence * load * (0.5 + 0.5 * ifn)) - 0.22 * ctl
            val dAb = adaptiveDelay * (0.38 * h.adaptiveCompetence * (load + 0.25 * effectiveMemory)) - 0.08 * ab

            val dIl6 = (0.52 * p.cytokineTrigger * load * steroidDamp / h.tregAntiInflammatory.coerceAtLeast(0.35)) -
                0.42 * il6 * (0.8 + 0.3 * h.tregAntiInflammatory)

            val tropismMult = p.tropism.clearanceDifficulty
            val dDamage = (0.048 * p.tissueInvasiveness * tropismMult * load + 0.038 * max(0.0, il6 - 0.85)) *
                supportiveProtect - 0.042 * damage

            load = max(0.0, load + dLoad * dt)
            ifn = max(0.05, ifn + dIfn * dt)
            ctl = max(0.02, ctl + dCtl * dt)
            ab = max(0.04, ab + dAb * dt)
            il6 = max(0.05, il6 + dIl6 * dt)
            damage = (damage + dDamage * dt).coerceIn(0.0, 0.98)
        }
        return out
    }

    private fun findClosestReferenceStrain(p: PathogenDesign): String {
        val best = REFERENCE_STRAINS.minByOrNull { ref ->
            val classPenalty = if (ref.pathogenClass == p.pathogenClass) 0.0 else 0.8
            val tropismPenalty = if (ref.tropism == p.tropism) 0.0 else 0.4
            classPenalty + tropismPenalty +
                abs(ref.replicationRate - p.replicationRate) +
                abs(ref.receptorAffinity - p.receptorAffinity) +
                abs(ref.immuneEvasion - p.immuneEvasion) +
                abs(ref.antigenicDrift - p.antigenicDrift) +
                abs(ref.cytokineTrigger - p.cytokineTrigger)
        } ?: REFERENCE_STRAINS.first()
        return best.name
    }

    private fun buildStrategyCritique(
        p: PathogenDesign,
        h: VirtualHostProfile,
        tx: TreatmentStrategy,
        maxStress: Double,
        untreatedStress: Double,
        peakDay: Double
    ): List<String> = buildList {
        add("Target Antimicrobial/Antiviral (${p.pathogenClass.standardDrugClass}): " +
            if (tx.useDirectAntiviralOrAntibiotic) {
                if (tx.interventionDay <= 3.0) "Optimal early window (Day ${"%.1f".format(tx.interventionDay)}) blunted replication before peak viral load."
                else "Started late (Day ${"%.1f".format(tx.interventionDay)}); direct pathogen reduction is partial because peak replication already occurred near Day ${"%.1f".format(peakDay)}."
            } else {
                "Not active — adding ${p.pathogenClass.standardDrugClass} early cuts peak pathogen AUC and contagious shedding."
            }
        )
        add("Passive Antibody / bNAb Neutralization: " +
            if (tx.useMonoclonalAntibody) {
                if (p.antigenicDrift > 0.9) "Partial neutralization due to high antigenic drift (${"%.2f".format(p.antigenicDrift)}); use conserved-epitope or polyclonal cocktails."
                else "Strong neutralization synergy with host IgG, accelerating clearance."
            } else {
                if (h.adaptiveCompetence < 0.5) "Strongly recommended for immunocompromised host lacking endogenous B-cell priming."
                else "Optional adjunct for high-receptor-affinity variants."
            }
        )
        add("Host-Directed Immunomodulation (Anti-IL-6 / Corticosteroid): " +
            if (tx.useHostImmunomodulator) {
                if (tx.interventionDay < 3.0 && !tx.useDirectAntiviralOrAntibiotic) "Caution: very early immunosuppression without antiviral cover can prolong pathogen replication."
                else "Effectively dampened IL-6 cytokine surge and alveolar/endothelial immunopathology."
            } else {
                if (maxStress > 35.0) "High inflammatory stress (${"%.0f".format(maxStress)}%) — pairing delayed immunomodulation after pathogen control lowers tissue injury."
                else "Not required in mild low-cytokine trajectories."
            }
        )
        if (untreatedStress > maxStress + 5.0) {
            add("Net Strategy Impact: Combined regimen lowered peak virtual organ stress from ${"%.0f".format(untreatedStress)}% (untreated) down to ${"%.0f".format(maxStress)}%.")
        }
    }
}
