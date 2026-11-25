package com.example.domain

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

data class MolAtom3D(
    val id: String,
    val symbol: String, // C, N, O, S, F, Cl, H
    val label: String,  // e.g., "C1 (Benzene)", "O1 (Phenolic -OH)"
    val functionalGroup: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val hybridization: String = "sp2"
)

data class MolBond3D(
    val fromIdx: Int,
    val toIdx: Int,
    val order: Int = 1, // 1=single, 2=double, 3=triple
    val isAromatic: Boolean = false
)

data class IrPeak(
    val wavenumberCm1: Double,
    val transmittancePct: Double,
    val intensity: String,
    val assignment: String
)

data class HNmrPeak(
    val ppm: Double,
    val multiplicity: String,
    val protons: Int,
    val jHz: Double,
    val assignment: String
)

data class CNmrPeak(
    val ppm: Double,
    val environment: String
)

data class MsFragment(
    val mz: Double,
    val relIntensity: Double,
    val ionFormula: String,
    val note: String
)

data class WetBenchTest(
    val testName: String,
    val reagent: String,
    val observation: String,
    val positive: Boolean,
    val inference: String
)

data class LipinskiMetrics(
    val mw: Double,
    val logP: Double,
    val hbd: Int,
    val hba: Int,
    val tpsaA2: Double,
    val rotBonds: Int,
    val ruleViolations: Int,
    val bbbPermeable: Boolean
)

data class MolecularStructureProfile(
    val id: String,
    val unknownCode: String,
    val name: String,
    val formula: String,
    val monoisotopicMass: Double,
    val ihd: Int,
    val iupacName: String,
    val therapeuticClass: String,
    val targetReceptor: String,
    val mechanism: String,
    val pkaValues: List<Double>,
    val functionalGroupTags: List<String>,
    val lipinski: LipinskiMetrics,
    val atoms: List<MolAtom3D>,
    val bonds: List<MolBond3D>,
    val irPeaks: List<IrPeak>,
    val hNmrPeaks: List<HNmrPeak>,
    val cNmrPeaks: List<CNmrPeak>,
    val msFragments: List<MsFragment>,
    val wetBenchTests: List<WetBenchTest>,
    val ec50Nm: Double,
    val emaxPct: Double,
    val hillSlope: Double,
    val ed50MgKg: Double,
    val ld50MgKg: Double,
    val bioavailabilityPct: Double,
    val halfLifeHr: Double,
    val vdL: Double,
    val kaPerHr: Double,
    val mecMgL: Double,
    val mtcMgL: Double
)

enum class AntagonistMechanism(val label: String) {
    NONE("No Antagonist"),
    COMPETITIVE("Competitive (Schild Shift)"),
    NON_COMPETITIVE("Non-Competitive (Irreversible)")
}

enum class DosingRoute(val label: String) {
    ORAL("Oral Tablet (1st-order Abs)"),
    IV_BOLUS("IV Bolus (100% Bioavail)")
}

data class ElucidationEvaluation(
    val scorePct: Int,
    val formulaMatched: Boolean,
    val ihdMatched: Boolean,
    val matchedGroups: List<String>,
    val missedGroups: List<String>,
    val extraGroups: List<String>,
    val summaryFeedback: String
)

data class PdPkSimulationResult(
    val drugName: String,
    val doseMg: Double,
    val route: DosingRoute,
    val antagonistMechanism: AntagonistMechanism,
    val baselineCurve: List<Pair<Double, Double>>,     // log10(nM) -> % Effect
    val modulatedCurve: List<Pair<Double, Double>>,    // log10(nM) -> % Effect
    val apparentEc50Nm: Double,
    val apparentEmaxPct: Double,
    val schildRatio: Double,
    val therapeuticIndex: Double,
    val pkCurve: List<Pair<Double, Double>>,           // hour -> mg/L
    val mecLine: List<Pair<Double, Double>>,
    val mtcLine: List<Pair<Double, Double>>,
    val cMaxMgL: Double,
    val tMaxHr: Double,
    val aucMgHrL: Double,
    val halfLifeHr: Double,
    val clearanceLhr: Double,
    val windowStatus: String,
    val clinicalInterpretation: String
)

object PharmacologyEngine {

    val ALL_FUNCTIONAL_GROUPS = listOf(
        "Phenolic -OH",
        "Carboxylic Acid (-COOH)",
        "Ester (-COOR)",
        "Amide (-CONH-)",
        "Aromatic Benzene Ring",
        "Primary Amine (-NH2)",
        "Secondary/Tertiary Amine",
        "β-Lactam Ring",
        "Thiazolidine / Thioether (S)",
        "Purine / Xanthine Heterocycle",
        "Quinoline Heterocycle",
        "Organohalide (-Cl / -F)",
        "Ketone / Enone (C=O)",
        "Acyclic Ether / Alcohol"
    )

    val MOLECULES: List<MolecularStructureProfile> = listOf(
        // 1. Acetaminophen (Paracetamol)
        MolecularStructureProfile(
            id = "acetaminophen",
            unknownCode = "Unknown Sample A-101",
            name = "Acetaminophen (Paracetamol)",
            formula = "C8H9NO2",
            monoisotopicMass = 151.0633,
            ihd = 5,
            iupacName = "N-(4-hydroxyphenyl)acetamide",
            therapeuticClass = "Analgesic / Antipyretic",
            targetReceptor = "CNS COX-3 / Peroxidase Site",
            mechanism = "Reduces oxidized active site of COX peroxidase and activates descending serotonergic pain pathways.",
            pkaValues = listOf(9.38),
            functionalGroupTags = listOf("Phenolic -OH", "Amide (-CONH-)", "Aromatic Benzene Ring"),
            lipinski = LipinskiMetrics(151.16, 0.46, 2, 2, 49.3, 1, 0, true),
            atoms = listOf(
                MolAtom3D("c1", "C", "C1 (Para-Amide C)", "Aromatic Benzene Ring", -0.18f, 0.25f, 0.02f),
                MolAtom3D("c2", "C", "C2 (Ortho-CH)", "Aromatic Benzene Ring", -0.44f, 0.12f, 0.12f),
                MolAtom3D("c3", "C", "C3 (Meta-CH)", "Aromatic Benzene Ring", -0.44f, -0.18f, 0.10f),
                MolAtom3D("c4", "C", "C4 (Phenolic C-OH)", "Aromatic Benzene Ring", -0.18f, -0.34f, -0.02f),
                MolAtom3D("c5", "C", "C5 (Meta-CH)", "Aromatic Benzene Ring", 0.08f, -0.18f, -0.10f),
                MolAtom3D("c6", "C", "C6 (Ortho-CH)", "Aromatic Benzene Ring", 0.08f, 0.12f, -0.08f),
                MolAtom3D("oh", "O", "Phenolic -OH (pKa 9.4)", "Phenolic -OH", -0.18f, -0.66f, -0.04f, "sp3"),
                MolAtom3D("n1", "N", "Amide -NH-", "Amide (-CONH-)", -0.18f, 0.54f, 0.06f, "sp2"),
                MolAtom3D("co", "C", "Carbonyl C=O", "Amide (-CONH-)", 0.14f, 0.68f, -0.02f, "sp2"),
                MolAtom3D("o2", "O", "Amide =O (HBA)", "Amide (-CONH-)", 0.34f, 0.52f, -0.14f, "sp2"),
                MolAtom3D("me", "C", "Acetyl -CH3", "Amide (-CONH-)", 0.28f, 0.90f, 0.08f, "sp3")
            ),
            bonds = listOf(
                MolBond3D(0, 1, 1, true), MolBond3D(1, 2, 2, true), MolBond3D(2, 3, 1, true),
                MolBond3D(3, 4, 2, true), MolBond3D(4, 5, 1, true), MolBond3D(5, 0, 2, true),
                MolBond3D(3, 6, 1), MolBond3D(0, 7, 1), MolBond3D(7, 8, 1),
                MolBond3D(8, 9, 2), MolBond3D(8, 10, 1)
            ),
            irPeaks = listOf(
                IrPeak(3325.0, 18.0, "Strong/Broad", "Phenolic O-H & Amide N-H stretch"),
                IrPeak(1654.0, 12.0, "Strong", "Amide I C=O stretch"),
                IrPeak(1564.0, 26.0, "Medium", "Amide II N-H bend + C-N"),
                IrPeak(1506.0, 22.0, "Strong", "Para-disubstituted aromatic C=C"),
                IrPeak(1260.0, 30.0, "Medium", "Phenolic C-O stretch"),
                IrPeak(836.0, 24.0, "Strong", "1,4-Para aromatic C-H out-of-plane")
            ),
            hNmrPeaks = listOf(
                HNmrPeak(9.65, "s", 1, 0.0, "Amide -NH- (D2O exchangeable)"),
                HNmrPeak(9.15, "s", 1, 0.0, "Phenolic -OH (D2O exchangeable)"),
                HNmrPeak(7.34, "d", 2, 8.8, "H-2, H-6 ortho to amide"),
                HNmrPeak(6.68, "d", 2, 8.8, "H-3, H-5 ortho to -OH"),
                HNmrPeak(1.98, "s", 3, 0.0, "Acetamide -CH3")
            ),
            cNmrPeaks = listOf(
                CNmrPeak(168.4, "Amide C=O"),
                CNmrPeak(153.6, "C4 Aromatic C-OH"),
                CNmrPeak(131.5, "C1 Aromatic C-NH"),
                CNmrPeak(121.3, "C2/C6 Aromatic CH"),
                CNmrPeak(115.4, "C3/C5 Aromatic CH"),
                CNmrPeak(24.1, "Acetyl CH3")
            ),
            msFragments = listOf(
                MsFragment(152.07, 100.0, "C8H10NO2+", "[M+H]+ Molecular Ion"),
                MsFragment(110.06, 68.0, "C6H8NO+", "Ketene loss (-42 Da, 4-aminophenol)"),
                MsFragment(93.03, 24.0, "C6H5O+", "NH3 + ketene elimination"),
                MsFragment(43.02, 41.0, "CH3CO+", "Acylium ion fragment")
            ),
            wetBenchTests = listOf(
                WetBenchTest("FeCl3 Complexation", "1% FeCl3 aq", "Deep violet-blue complex forms immediately", true, "Confirms Phenolic -OH group"),
                WetBenchTest("NaHCO3 Effervescence", "5% NaHCO3", "No CO2 gas bubbles evolved", false, "Rules out strong Carboxylic Acid (-COOH)"),
                WetBenchTest("2,4-DNP Reagent", "2,4-Dinitrophenylhydrazine", "No orange-red precipitate", false, "Carbonyl is an Amide, not a Ketone/Aldehyde"),
                WetBenchTest("NaOH Solubility", "1M NaOH", "Dissolves reversibly; pKa ≈ 9.4", true, "Weakly acidic phenol proton")
            ),
            ec50Nm = 4200.0, emaxPct = 82.0, hillSlope = 1.15,
            ed50MgKg = 18.0, ld50MgKg = 338.0,
            bioavailabilityPct = 79.0, halfLifeHr = 2.4, vdL = 63.0, kaPerHr = 1.8, mecMgL = 5.0, mtcMgL = 150.0
        ),

        // 2. Aspirin (Acetylsalicylic Acid)
        MolecularStructureProfile(
            id = "aspirin",
            unknownCode = "Unknown Sample B-204",
            name = "Aspirin (Acetylsalicylic Acid)",
            formula = "C9H8O4",
            monoisotopicMass = 180.0423,
            ihd = 6,
            iupacName = "2-acetoxybenzoic acid",
            therapeuticClass = "NSAID / Antiplatelet COX Inhibitor",
            targetReceptor = "COX-1 (Ser530 Acetylation) & COX-2",
            mechanism = "Irreversibly acetylates Ser530 in the COX active site channel, blocking prostaglandin H2 and TXA2 synthesis.",
            pkaValues = listOf(3.49),
            functionalGroupTags = listOf("Carboxylic Acid (-COOH)", "Ester (-COOR)", "Aromatic Benzene Ring"),
            lipinski = LipinskiMetrics(180.16, 1.19, 1, 4, 63.6, 3, 0, true),
            atoms = listOf(
                MolAtom3D("c1", "C", "C1 (Carboxyl ipso)", "Aromatic Benzene Ring", -0.12f, 0.16f, 0f),
                MolAtom3D("c2", "C", "C2 (Acetoxy ipso)", "Aromatic Benzene Ring", 0.16f, 0.02f, 0.08f),
                MolAtom3D("c3", "C", "C3 (Aromatic CH)", "Aromatic Benzene Ring", 0.16f, -0.28f, 0.06f),
                MolAtom3D("c4", "C", "C4 (Aromatic CH)", "Aromatic Benzene Ring", -0.12f, -0.44f, -0.02f),
                MolAtom3D("c5", "C", "C5 (Aromatic CH)", "Aromatic Benzene Ring", -0.38f, -0.28f, -0.08f),
                MolAtom3D("c6", "C", "C6 (Aromatic CH)", "Aromatic Benzene Ring", -0.38f, 0.02f, -0.06f),
                MolAtom3D("ca_c", "C", "Carboxyl C=O", "Carboxylic Acid (-COOH)", -0.14f, 0.48f, -0.04f, "sp2"),
                MolAtom3D("ca_o1", "O", "Carboxyl =O", "Carboxylic Acid (-COOH)", -0.38f, 0.62f, -0.14f, "sp2"),
                MolAtom3D("ca_o2", "O", "Acidic -OH (pKa 3.5)", "Carboxylic Acid (-COOH)", 0.10f, 0.66f, 0.06f, "sp3"),
                MolAtom3D("es_o1", "O", "Phenolic Ester -O-", "Ester (-COOR)", 0.42f, 0.16f, 0.18f, "sp3"),
                MolAtom3D("es_c", "C", "Acetyl Ester C=O", "Ester (-COOR)", 0.62f, -0.04f, 0.24f, "sp2"),
                MolAtom3D("es_o2", "O", "Ester Carbonyl =O", "Ester (-COOR)", 0.56f, -0.30f, 0.34f, "sp2"),
                MolAtom3D("es_me", "C", "Transfer Acetyl -CH3", "Ester (-COOR)", 0.86f, 0.08f, 0.14f, "sp3")
            ),
            bonds = listOf(
                MolBond3D(0, 1, 1, true), MolBond3D(1, 2, 2, true), MolBond3D(2, 3, 1, true),
                MolBond3D(3, 4, 2, true), MolBond3D(4, 5, 1, true), MolBond3D(5, 0, 2, true),
                MolBond3D(0, 6, 1), MolBond3D(6, 7, 2), MolBond3D(6, 8, 1),
                MolBond3D(1, 9, 1), MolBond3D(9, 10, 1), MolBond3D(10, 11, 2), MolBond3D(10, 12, 1)
            ),
            irPeaks = listOf(
                IrPeak(2980.0, 22.0, "Very Broad", "Carboxylic acid O-H dimer stretch (3300-2500 cm-1)"),
                IrPeak(1754.0, 10.0, "Strong", "Phenolic Ester C=O stretch"),
                IrPeak(1692.0, 14.0, "Strong", "Aryl Carboxylic Acid C=O stretch"),
                IrPeak(1605.0, 28.0, "Medium", "Ortho-substituted aromatic ring C=C"),
                IrPeak(1188.0, 18.0, "Strong", "Acetate C-O stretch")
            ),
            hNmrPeaks = listOf(
                HNmrPeak(11.42, "s", 1, 0.0, "Carboxylic acid -COOH"),
                HNmrPeak(8.02, "dd", 1, 7.8, "H-6 ortho to -COOH"),
                HNmrPeak(7.61, "td", 1, 7.6, "H-4 para to -COOH"),
                HNmrPeak(7.35, "td", 1, 7.6, "H-5 meta to -COOH"),
                HNmrPeak(7.14, "dd", 1, 8.0, "H-3 ortho to -OAc"),
                HNmrPeak(2.32, "s", 3, 0.0, "Acetoxy -CH3")
            ),
            cNmrPeaks = listOf(
                CNmrPeak(169.8, "Ester Acetyl C=O"),
                CNmrPeak(165.4, "Carboxylic Acid -COOH"),
                CNmrPeak(150.6, "C2 Aromatic C-OAc"),
                CNmrPeak(134.1, "C4 Aromatic CH"),
                CNmrPeak(131.6, "C6 Aromatic CH"),
                CNmrPeak(21.0, "Acetyl CH3")
            ),
            msFragments = listOf(
                MsFragment(181.05, 35.0, "C9H9O4+", "[M+H]+ intact"),
                MsFragment(139.04, 100.0, "C7H7O3+", "Ketene loss (-42 Da -> Salicylic acid)"),
                MsFragment(121.03, 76.0, "C7H5O2+", "Ortho effect H2O loss (-18 Da)"),
                MsFragment(43.02, 52.0, "CH3CO+", "Acetyl cation")
            ),
            wetBenchTests = listOf(
                WetBenchTest("NaHCO3 Effervescence", "5% NaHCO3", "Vigorous CO2 gas evolution", true, "Confirms Carboxylic Acid (-COOH, pKa ~3.5)"),
                WetBenchTest("FeCl3 (Unheated)", "1% FeCl3", "Pale yellow (negative initially; turns violet after hydrolysis)", false, "Phenol OH is masked as an Ester (-OAc)"),
                WetBenchTest("Hydroxamate Test", "NH2OH / HCl / FeCl3", "Burgundy-red ferric hydroxamate", true, "Confirms Ester functional group")
            ),
            ec50Nm = 950.0, emaxPct = 95.0, hillSlope = 1.25,
            ed50MgKg = 14.0, ld50MgKg = 200.0,
            bioavailabilityPct = 68.0, halfLifeHr = 3.2, vdL = 11.0, kaPerHr = 2.4, mecMgL = 20.0, mtcMgL = 300.0
        ),

        // 3. Caffeine
        MolecularStructureProfile(
            id = "caffeine",
            unknownCode = "Unknown Sample C-307",
            name = "Caffeine (1,3,7-Trimethylxanthine)",
            formula = "C8H10N4O2",
            monoisotopicMass = 194.0804,
            ihd = 6,
            iupacName = "1,3,7-trimethyl-3,7-dihydro-1H-purine-2,6-dione",
            therapeuticClass = "CNS Stimulant / Adenosine Receptor Antagonist",
            targetReceptor = "Adenosine A1 & A2A GPCRs / PDE4",
            mechanism = "Competitive antagonist at central adenosine receptors and weak phosphodiesterase inhibitor, elevating cAMP.",
            pkaValues = listOf(0.6),
            functionalGroupTags = listOf("Purine / Xanthine Heterocycle", "Amide (-CONH-)", "Secondary/Tertiary Amine"),
            lipinski = LipinskiMetrics(194.19, -0.07, 0, 6, 58.4, 0, 0, true),
            atoms = listOf(
                MolAtom3D("n1", "N", "N1 (Imide N-CH3)", "Purine / Xanthine Heterocycle", -0.45f, 0.25f, 0.04f),
                MolAtom3D("c2", "C", "C2 (Urea C=O)", "Purine / Xanthine Heterocycle", -0.45f, -0.10f, 0.06f),
                MolAtom3D("n3", "N", "N3 (Methyl-N)", "Purine / Xanthine Heterocycle", -0.18f, -0.28f, 0.02f),
                MolAtom3D("c4", "C", "C4 (Bridgehead)", "Purine / Xanthine Heterocycle", 0.08f, -0.10f, -0.04f),
                MolAtom3D("c5", "C", "C5 (Bridgehead)", "Purine / Xanthine Heterocycle", 0.08f, 0.22f, -0.04f),
                MolAtom3D("c6", "C", "C6 (Lactam C=O)", "Purine / Xanthine Heterocycle", -0.18f, 0.40f, 0.00f),
                MolAtom3D("n7", "N", "N7 (Imidazole N-Me)", "Purine / Xanthine Heterocycle", 0.36f, 0.32f, -0.08f),
                MolAtom3D("c8", "C", "C8 (Imidazole C-H)", "Purine / Xanthine Heterocycle", 0.52f, 0.06f, -0.10f),
                MolAtom3D("n9", "N", "N9 (Imidazole =N-)", "Purine / Xanthine Heterocycle", 0.36f, -0.20f, -0.06f),
                MolAtom3D("o2", "O", "C2 Carbonyl =O", "Amide (-CONH-)", -0.70f, -0.22f, 0.12f),
                MolAtom3D("o6", "O", "C6 Carbonyl =O", "Amide (-CONH-)", -0.18f, 0.68f, 0.02f),
                MolAtom3D("me1", "C", "N1-CH3", "Secondary/Tertiary Amine", -0.72f, 0.42f, 0.08f, "sp3"),
                MolAtom3D("me3", "C", "N3-CH3", "Secondary/Tertiary Amine", -0.18f, -0.58f, 0.05f, "sp3"),
                MolAtom3D("me7", "C", "N7-CH3", "Secondary/Tertiary Amine", 0.50f, 0.58f, -0.14f, "sp3")
            ),
            bonds = listOf(
                MolBond3D(0, 1, 1, true), MolBond3D(1, 2, 1, true), MolBond3D(2, 3, 1, true),
                MolBond3D(3, 4, 2, true), MolBond3D(4, 5, 1, true), MolBond3D(5, 0, 1, true),
                MolBond3D(4, 6, 1, true), MolBond3D(6, 7, 1, true), MolBond3D(7, 8, 2, true), MolBond3D(8, 3, 1, true),
                MolBond3D(1, 9, 2), MolBond3D(5, 10, 2),
                MolBond3D(0, 11, 1), MolBond3D(2, 12, 1), MolBond3D(6, 13, 1)
            ),
            irPeaks = listOf(
                IrPeak(1701.0, 12.0, "Strong", "Asymmetric C2=O xanthine stretch"),
                IrPeak(1658.0, 8.0, "Strong", "C6=O + C=N conjugated stretch"),
                IrPeak(1550.0, 24.0, "Medium", "Purine ring C=C / C=N skeletal"),
                IrPeak(2954.0, 35.0, "Medium", "N-CH3 aliphatic C-H (no O-H / N-H >3200)")
            ),
            hNmrPeaks = listOf(
                HNmrPeak(7.55, "s", 1, 0.0, "H-8 Imidazole aromatic proton"),
                HNmrPeak(3.98, "s", 3, 0.0, "N7-CH3"),
                HNmrPeak(3.56, "s", 3, 0.0, "N3-CH3"),
                HNmrPeak(3.38, "s", 3, 0.0, "N1-CH3")
            ),
            cNmrPeaks = listOf(
                CNmrPeak(155.4, "C6 Carbonyl"),
                CNmrPeak(151.7, "C2 Carbonyl"),
                CNmrPeak(148.7, "C4 Bridgehead"),
                CNmrPeak(141.5, "C8 Imidazole CH"),
                CNmrPeak(107.6, "C5 Bridgehead"),
                CNmrPeak(33.6, "N7-CH3"),
                CNmrPeak(29.8, "N3-CH3"),
                CNmrPeak(27.9, "N1-CH3")
            ),
            msFragments = listOf(
                MsFragment(195.09, 100.0, "C8H11N4O2+", "[M+H]+ Even-mass 4-N Nitrogen Rule!"),
                MsFragment(138.07, 45.0, "C6H8N3O+", "Retro-Diels-Alder loss of CH3NCO (-57 Da)"),
                MsFragment(110.07, 28.0, "C5H8N3+", "Subsequent CO loss (-28 Da)")
            ),
            wetBenchTests = listOf(
                WetBenchTest("Murexide Test", "HClO3 + NH4OH", "Rich purple ammonium purpurate", true, "Confirms Xanthine / Purine heterocycle"),
                WetBenchTest("Dragendorff Reagent", "KBiI4", "Orange-brown alkaloid precipitate", true, "Nitrogenous alkaloid ring"),
                WetBenchTest("Acetic Anhydride / H-shift", "D2O Shake", "Zero exchangeable O-H or N-H protons", false, "All nitrogens are tertiary/alkylated")
            ),
            ec50Nm = 12000.0, emaxPct = 88.0, hillSlope = 1.0,
            ed50MgKg = 4.5, ld50MgKg = 192.0,
            bioavailabilityPct = 99.0, halfLifeHr = 4.9, vdL = 42.0, kaPerHr = 3.2, mecMgL = 2.0, mtcMgL = 40.0
        ),

        // 4. Ibuprofen
        MolecularStructureProfile(
            id = "ibuprofen",
            unknownCode = "Unknown Sample D-412",
            name = "(S)-Ibuprofen",
            formula = "C13H18O2",
            monoisotopicMass = 206.1307,
            ihd = 5,
            iupacName = "(2S)-2-[4-(2-methylpropyl)phenyl]propanoic acid",
            therapeuticClass = "NSAID / Non-Selective COX Inhibitor",
            targetReceptor = "COX-1 & COX-2 Arg120 Salt-Bridge Pocket",
            mechanism = "Reversible competitive inhibitor of arachidonic acid binding inside the COX cyclooxygenase channel.",
            pkaValues = listOf(4.4),
            functionalGroupTags = listOf("Carboxylic Acid (-COOH)", "Aromatic Benzene Ring"),
            lipinski = LipinskiMetrics(206.28, 3.97, 1, 2, 37.3, 4, 0, true),
            atoms = listOf(
                MolAtom3D("c1", "C", "C1 (Propionic ipso)", "Aromatic Benzene Ring", 0.14f, 0.04f, 0.0f),
                MolAtom3D("c2", "C", "C2 (Ar CH)", "Aromatic Benzene Ring", 0.0f, 0.28f, 0.08f),
                MolAtom3D("c3", "C", "C3 (Ar CH)", "Aromatic Benzene Ring", -0.28f, 0.28f, 0.06f),
                MolAtom3D("c4", "C", "C4 (Isobutyl ipso)", "Aromatic Benzene Ring", -0.42f, 0.04f, -0.02f),
                MolAtom3D("c5", "C", "C5 (Ar CH)", "Aromatic Benzene Ring", -0.28f, -0.22f, -0.08f),
                MolAtom3D("c6", "C", "C6 (Ar CH)", "Aromatic Benzene Ring", 0.0f, -0.22f, -0.06f),
                MolAtom3D("ch", "C", "Chiral α-C* (S)", "Carboxylic Acid (-COOH)", 0.44f, 0.04f, 0.04f, "sp3"),
                MolAtom3D("am", "C", "α-CH3", "Carboxylic Acid (-COOH)", 0.56f, -0.20f, 0.22f, "sp3"),
                MolAtom3D("co", "C", "Carboxyl C=O", "Carboxylic Acid (-COOH)", 0.66f, 0.26f, -0.08f, "sp2"),
                MolAtom3D("o1", "O", "Carboxyl =O", "Carboxylic Acid (-COOH)", 0.62f, 0.52f, -0.14f, "sp2"),
                MolAtom3D("o2", "O", "Carboxyl -OH", "Carboxylic Acid (-COOH)", 0.90f, 0.16f, -0.12f, "sp3"),
                MolAtom3D("ib1", "C", "Benzyl -CH2-", "Aromatic Benzene Ring", -0.70f, 0.04f, -0.04f, "sp3"),
                MolAtom3D("ib2", "C", "Isobutyl -CH-", "Aromatic Benzene Ring", -0.84f, -0.22f, 0.10f, "sp3")
            ),
            bonds = listOf(
                MolBond3D(0, 1, 1, true), MolBond3D(1, 2, 2, true), MolBond3D(2, 3, 1, true),
                MolBond3D(3, 4, 2, true), MolBond3D(4, 5, 1, true), MolBond3D(5, 0, 2, true),
                MolBond3D(0, 6, 1), MolBond3D(6, 7, 1), MolBond3D(6, 8, 1),
                MolBond3D(8, 9, 2), MolBond3D(8, 10, 1),
                MolBond3D(3, 11, 1), MolBond3D(11, 12, 1)
            ),
            irPeaks = listOf(
                IrPeak(2955.0, 14.0, "Strong/Broad", "Aliphatic C-H + Carboxylic O-H dimer"),
                IrPeak(1721.0, 8.0, "Very Strong", "Aliphatic Carboxylic Acid C=O stretch"),
                IrPeak(1512.0, 34.0, "Medium", "1,4-Para benzene ring stretch"),
                IrPeak(1231.0, 26.0, "Medium", "Acid C-O stretch")
            ),
            hNmrPeaks = listOf(
                HNmrPeak(11.1, "s", 1, 0.0, "-COOH proton"),
                HNmrPeak(7.22, "d", 2, 8.0, "Aromatic CH ortho to chiral center"),
                HNmrPeak(7.10, "d", 2, 8.0, "Aromatic CH ortho to isobutyl"),
                HNmrPeak(3.71, "q", 1, 7.2, "Chiral α-CH quartet"),
                HNmrPeak(2.45, "d", 2, 7.2, "Benzylic -CH2-"),
                HNmrPeak(1.85, "m", 1, 6.7, "Isobutyl methine -CH-"),
                HNmrPeak(1.50, "d", 3, 7.2, "Chiral α-CH3 doublet"),
                HNmrPeak(0.90, "d", 6, 6.6, "Terminal gem-dimethyl (CH3)2")
            ),
            cNmrPeaks = listOf(
                CNmrPeak(181.2, "Carboxylic -COOH"),
                CNmrPeak(140.8, "C4 Aromatic"),
                CNmrPeak(137.7, "C1 Aromatic"),
                CNmrPeak(129.4, "Aromatic CH"),
                CNmrPeak(45.1, "Chiral α-CH"),
                CNmrPeak(22.4, "Gem-dimethyls")
            ),
            msFragments = listOf(
                MsFragment(206.13, 38.0, "C13H18O2+", "M+. Molecular ion"),
                MsFragment(163.08, 100.0, "C10H11O2+", "Loss of isopropyl radical (-43 Da)"),
                MsFragment(161.13, 72.0, "C12H17+", "Loss of -COOH radical (-45 Da)"),
                MsFragment(119.09, 44.0, "C9H11+", "Dual side-chain cleavage")
            ),
            wetBenchTests = listOf(
                WetBenchTest("NaHCO3 Effervescence", "5% NaHCO3", "CO2 bubbles formed", true, "Confirms Carboxylic Acid (-COOH)"),
                WetBenchTest("Polarimetry [α]D", "Ethanol solution", "+54° optical rotation", true, "Confirms chiral α-carbon enantiomer"),
                WetBenchTest("FeCl3 Test", "1% FeCl3", "Negative (no violet color)", false, "No phenolic -OH present")
            ),
            ec50Nm = 1800.0, emaxPct = 92.0, hillSlope = 1.1,
            ed50MgKg = 10.0, ld50MgKg = 636.0,
            bioavailabilityPct = 85.0, halfLifeHr = 2.1, vdL = 10.5, kaPerHr = 2.0, mecMgL = 10.0, mtcMgL = 100.0
        ),

        // 5. Penicillin G
        MolecularStructureProfile(
            id = "penicillin_g",
            unknownCode = "Unknown Sample E-519",
            name = "Penicillin G (Benzylpenicillin)",
            formula = "C16H18N2O4S",
            monoisotopicMass = 334.0987,
            ihd = 9,
            iupacName = "(2S,5R,6R)-3,3-dimethyl-7-oxo-6-(2-phenylacetamido)-4-thia-1-azabicyclo[3.2.0]heptane-2-carboxylic acid",
            therapeuticClass = "β-Lactam Bactericidal Antibiotic",
            targetReceptor = "Bacterial Penicillin-Binding Proteins (PBPs / Transpeptidase)",
            mechanism = "Strained 4-membered β-lactam ring acylates active-site Serine of D-Ala-D-Ala transpeptidase, blocking peptidoglycan cross-linking.",
            pkaValues = listOf(2.74),
            functionalGroupTags = listOf("β-Lactam Ring", "Thiazolidine / Thioether (S)", "Amide (-CONH-)", "Carboxylic Acid (-COOH)", "Aromatic Benzene Ring"),
            lipinski = LipinskiMetrics(334.39, 1.83, 2, 5, 112.0, 4, 0, false),
            atoms = listOf(
                MolAtom3D("n4", "N", "β-Lactam Bridgehead N4", "β-Lactam Ring", 0.0f, 0.0f, 0.0f, "sp3"),
                MolAtom3D("c7", "C", "Strained β-Lactam C7=O", "β-Lactam Ring", -0.26f, 0.22f, 0.08f, "sp2"),
                MolAtom3D("c6", "C", "C6 Chiral Amide Junction", "β-Lactam Ring", -0.28f, -0.06f, 0.14f, "sp3"),
                MolAtom3D("c5", "C", "Bridgehead C5", "β-Lactam Ring", -0.02f, -0.24f, 0.08f, "sp3"),
                MolAtom3D("o8", "O", "β-Lactam =O (1775 cm-1)", "β-Lactam Ring", -0.44f, 0.44f, 0.10f, "sp2"),
                MolAtom3D("s1", "S", "Thiazolidine Sulfur S1", "Thiazolidine / Thioether (S)", 0.24f, -0.34f, -0.10f, "sp3"),
                MolAtom3D("c2", "C", "gem-Dimethyl C2", "Thiazolidine / Thioether (S)", 0.42f, -0.06f, -0.18f, "sp3"),
                MolAtom3D("c3", "C", "C3 Carboxyl Junction", "Thiazolidine / Thioether (S)", 0.24f, 0.16f, -0.10f, "sp3"),
                MolAtom3D("cooh", "C", "C3 -COOH", "Carboxylic Acid (-COOH)", 0.42f, 0.44f, -0.12f, "sp2"),
                MolAtom3D("am_n", "N", "Exocyclic Amide -NH-", "Amide (-CONH-)", -0.52f, -0.12f, 0.26f, "sp2"),
                MolAtom3D("am_c", "C", "Acyl Side-Chain C=O", "Amide (-CONH-)", -0.74f, 0.04f, 0.12f, "sp2"),
                MolAtom3D("ph", "C", "Benzyl Phenyl Core", "Aromatic Benzene Ring", -0.92f, -0.22f, -0.04f, "sp2")
            ),
            bonds = listOf(
                MolBond3D(0, 1, 1), MolBond3D(1, 2, 1), MolBond3D(2, 3, 1), MolBond3D(3, 0, 1),
                MolBond3D(1, 4, 2),
                MolBond3D(3, 5, 1), MolBond3D(5, 6, 1), MolBond3D(6, 7, 1), MolBond3D(7, 0, 1),
                MolBond3D(7, 8, 1), MolBond3D(2, 9, 1), MolBond3D(9, 10, 1), MolBond3D(10, 11, 1)
            ),
            irPeaks = listOf(
                IrPeak(1778.0, 6.0, "Diagnostic Very High", "Strained 4-membered β-Lactam C=O stretch!"),
                IrPeak(1690.0, 16.0, "Strong", "Carboxylic Acid C=O"),
                IrPeak(1655.0, 14.0, "Strong", "Secondary Amide C=O"),
                IrPeak(3350.0, 24.0, "Medium", "Amide N-H stretch")
            ),
            hNmrPeaks = listOf(
                HNmrPeak(7.28, "m", 5, 7.2, "Monosubstituted benzyl aromatic ring"),
                HNmrPeak(5.52, "dd", 1, 4.1, "β-Lactam cis H-6 (J = 4.1 Hz diagnostic cis!)"),
                HNmrPeak(5.44, "d", 1, 4.1, "β-Lactam H-5"),
                HNmrPeak(4.36, "s", 1, 0.0, "Thiazolidine H-3"),
                HNmrPeak(3.62, "s", 2, 0.0, "Benzylic -CH2-"),
                HNmrPeak(1.62, "s", 3, 0.0, "2β-CH3"),
                HNmrPeak(1.50, "s", 3, 0.0, "2α-CH3")
            ),
            cNmrPeaks = listOf(
                CNmrPeak(174.5, "β-Lactam C7=O"),
                CNmrPeak(171.2, "Amide C=O"),
                CNmrPeak(169.8, "-COOH"),
                CNmrPeak(70.4, "C3"),
                CNmrPeak(64.8, "C2-S")
            ),
            msFragments = listOf(
                MsFragment(335.11, 100.0, "C16H19N2O4S+", "[M+H]+ with M+2 (4.4% -> 1 Sulfur atom)"),
                MsFragment(176.07, 65.0, "C10H10NO2+", "β-Lactam retro-[2+2] ketene fragment"),
                MsFragment(160.04, 82.0, "C6H10NO2S+", "Thiazolidine fragment containing S"),
                MsFragment(91.05, 55.0, "C7H7+", "Tropylium (benzyl) ion")
            ),
            wetBenchTests = listOf(
                WetBenchTest("β-Lactamase Hydrolysis", "Purified TEM-1 β-lactamase", "Rapid pH drop + 1778 cm-1 IR peak disappears", true, "Confirms strained β-lactam ring"),
                WetBenchTest("Sodium Fusion + Pb(OAc)2", "Lassaigne Sulfur Test", "Black PbS precipitate", true, "Confirms Sulfur heteroatom (S)"),
                WetBenchTest("NaHCO3 Effervescence", "5% NaHCO3", "CO2 bubbles", true, "Confirms Carboxylic Acid (-COOH)")
            ),
            ec50Nm = 35.0, emaxPct = 98.0, hillSlope = 1.6,
            ed50MgKg = 4.0, ld50MgKg = 2000.0,
            bioavailabilityPct = 20.0, halfLifeHr = 0.7, vdL = 22.0, kaPerHr = 1.1, mecMgL = 0.5, mtcMgL = 120.0
        ),

        // 6. Dopamine
        MolecularStructureProfile(
            id = "dopamine",
            unknownCode = "Unknown Sample F-621",
            name = "Dopamine (3,4-Dihydroxyphenethylamine)",
            formula = "C8H11NO2",
            monoisotopicMass = 153.0790,
            ihd = 4,
            iupacName = "4-(2-aminoethyl)benzene-1,2-diol",
            therapeuticClass = "Catecholamine Neurotransmitter / D1-D5 Agonist",
            targetReceptor = "Dopamine D1–D5 GPCRs & α1/β1 at high dose",
            mechanism = "Activates Gs-coupled D1/D5 and Gi-coupled D2/D3/D4 receptors; renal vasodilation at low dose, inotrope at mid dose.",
            pkaValues = listOf(8.9, 10.6),
            functionalGroupTags = listOf("Phenolic -OH", "Primary Amine (-NH2)", "Aromatic Benzene Ring"),
            lipinski = LipinskiMetrics(153.18, -0.98, 3, 3, 66.5, 2, 0, false),
            atoms = listOf(
                MolAtom3D("c1", "C", "C1 (Side-chain ipso)", "Aromatic Benzene Ring", -0.10f, 0.0f, 0.0f),
                MolAtom3D("c2", "C", "C2 (Ar CH)", "Aromatic Benzene Ring", -0.26f, 0.26f, 0.06f),
                MolAtom3D("c3", "C", "C3 (Catechol C-OH)", "Aromatic Benzene Ring", -0.54f, 0.26f, 0.04f),
                MolAtom3D("c4", "C", "C4 (Catechol C-OH)", "Aromatic Benzene Ring", -0.68f, 0.0f, -0.02f),
                MolAtom3D("c5", "C", "C5 (Ar CH)", "Aromatic Benzene Ring", -0.54f, -0.26f, -0.06f),
                MolAtom3D("c6", "C", "C6 (Ar CH)", "Aromatic Benzene Ring", -0.26f, -0.26f, -0.04f),
                MolAtom3D("oh3", "O", "C3 Catechol -OH", "Phenolic -OH", -0.68f, 0.54f, 0.08f, "sp3"),
                MolAtom3D("oh4", "O", "C4 Catechol -OH", "Phenolic -OH", -0.94f, 0.0f, -0.04f, "sp3"),
                MolAtom3D("cb", "C", "β-CH2", "Primary Amine (-NH2)", 0.20f, 0.0f, 0.02f, "sp3"),
                MolAtom3D("ca", "C", "α-CH2", "Primary Amine (-NH2)", 0.46f, 0.22f, -0.08f, "sp3"),
                MolAtom3D("nh2", "N", "Primary -NH2 (Protonated pH 7.4)", "Primary Amine (-NH2)", 0.74f, 0.16f, -0.02f, "sp3")
            ),
            bonds = listOf(
                MolBond3D(0, 1, 1, true), MolBond3D(1, 2, 2, true), MolBond3D(2, 3, 1, true),
                MolBond3D(3, 4, 2, true), MolBond3D(4, 5, 1, true), MolBond3D(5, 0, 2, true),
                MolBond3D(2, 6, 1), MolBond3D(3, 7, 1),
                MolBond3D(0, 8, 1), MolBond3D(8, 9, 1), MolBond3D(9, 10, 1)
            ),
            irPeaks = listOf(
                IrPeak(3340.0, 16.0, "Broad", "Catechol O-H & Primary N-H2 stretches"),
                IrPeak(1615.0, 22.0, "Medium", "N-H scissoring + Aromatic C=C"),
                IrPeak(1285.0, 18.0, "Strong", "Catechol C-O phenolic stretches")
            ),
            hNmrPeaks = listOf(
                HNmrPeak(6.72, "d", 1, 8.0, "H-5 Aromatic"),
                HNmrPeak(6.68, "d", 1, 2.0, "H-2 Aromatic"),
                HNmrPeak(6.55, "dd", 1, 8.0, "H-6 Aromatic"),
                HNmrPeak(3.02, "t", 2, 7.4, "α-CH2 adjacent to -NH3+"),
                HNmrPeak(2.74, "t", 2, 7.4, "Benzylic β-CH2")
            ),
            cNmrPeaks = listOf(
                CNmrPeak(144.8, "C3/C4 Catechol C-OH"),
                CNmrPeak(129.4, "C1"),
                CNmrPeak(120.8, "C6"),
                CNmrPeak(41.2, "α-CH2-N"),
                CNmrPeak(33.1, "β-CH2")
            ),
            msFragments = listOf(
                MsFragment(154.09, 100.0, "C8H12NO2+", "[M+H]+"),
                MsFragment(137.06, 74.0, "C8H9O2+", "Neutral NH3 loss (-17 Da -> ethylcatechol)"),
                MsFragment(123.04, 30.0, "C7H7O2+", "Dihydroxybenzyl cation")
            ),
            wetBenchTests = listOf(
                WetBenchTest("FeCl3 Catechol Test", "FeCl3 neutral", "Emerald-green turning red with NaHCO3", true, "Diagnostic ortho-dihydroxy catechol motif!"),
                WetBenchTest("Ninhydrin Reagent", "0.2% Ninhydrin + heat", "Ruhemann's purple develops", true, "Confirms Primary Amine (-NH2)"),
                WetBenchTest("2,4-DNP Test", "2,4-DNP", "No precipitate", false, "No aldehyde or ketone C=O")
            ),
            ec50Nm = 180.0, emaxPct = 100.0, hillSlope = 1.0,
            ed50MgKg = 0.8, ld50MgKg = 59.0,
            bioavailabilityPct = 2.0, halfLifeHr = 0.15, vdL = 35.0, kaPerHr = 0.5, mecMgL = 0.02, mtcMgL = 0.5
        ),

        // 7. Chloroquine
        MolecularStructureProfile(
            id = "chloroquine",
            unknownCode = "Unknown Sample G-715",
            name = "Chloroquine",
            formula = "C18H26ClN3",
            monoisotopicMass = 319.1815,
            ihd = 7,
            iupacName = "4-N-(7-chloroquinolin-4-yl)-1-N,1-N-diethylpentane-1,4-diamine",
            therapeuticClass = "4-Aminoquinoline Antimalarial / Lysosomotropic Agent",
            targetReceptor = "Heme Polymerase (Hemozoin) & Endosomal TLR9",
            mechanism = "Accumulates in acidic food vacuoles/lysosomes via ion trapping, inhibiting toxic heme detoxification and TLR7/9 acidification.",
            pkaValues = listOf(8.1, 10.2),
            functionalGroupTags = listOf("Quinoline Heterocycle", "Organohalide (-Cl / -F)", "Secondary/Tertiary Amine"),
            lipinski = LipinskiMetrics(319.87, 4.63, 1, 3, 28.2, 8, 0, true),
            atoms = listOf(
                MolAtom3D("n1", "N", "Quinoline N1", "Quinoline Heterocycle", -0.50f, -0.30f, 0.0f),
                MolAtom3D("c2", "C", "C2", "Quinoline Heterocycle", -0.24f, -0.42f, 0.02f),
                MolAtom3D("c3", "C", "C3", "Quinoline Heterocycle", 0.02f, -0.26f, 0.04f),
                MolAtom3D("c4", "C", "C4 (4-Amino ipso)", "Quinoline Heterocycle", 0.02f, 0.04f, 0.02f),
                MolAtom3D("c4a", "C", "Bridge C4a", "Quinoline Heterocycle", -0.24f, 0.20f, 0.0f),
                MolAtom3D("c8a", "C", "Bridge C8a", "Quinoline Heterocycle", -0.50f, 0.02f, -0.02f),
                MolAtom3D("c7", "C", "C7-Cl", "Quinoline Heterocycle", -0.78f, 0.36f, -0.06f),
                MolAtom3D("cl", "Cl", "7-Chloro (-Cl)", "Organohalide (-Cl / -F)", -1.04f, 0.50f, -0.10f, "sp3"),
                MolAtom3D("n4", "N", "Secondary 4-NH-", "Secondary/Tertiary Amine", 0.28f, 0.18f, 0.04f, "sp2"),
                MolAtom3D("ch", "C", "Side-chain CH(CH3)", "Secondary/Tertiary Amine", 0.52f, 0.02f, 0.12f, "sp3"),
                MolAtom3D("t_n", "N", "Terminal NEt2 (Basic pKa 10.2)", "Secondary/Tertiary Amine", 0.88f, -0.14f, -0.08f, "sp3")
            ),
            bonds = listOf(
                MolBond3D(0, 1, 2, true), MolBond3D(1, 2, 1, true), MolBond3D(2, 3, 2, true),
                MolBond3D(3, 4, 1, true), MolBond3D(4, 5, 2, true), MolBond3D(5, 0, 1, true),
                MolBond3D(4, 6, 1, true), MolBond3D(6, 7, 1),
                MolBond3D(3, 8, 1), MolBond3D(8, 9, 1), MolBond3D(9, 10, 1)
            ),
            irPeaks = listOf(
                IrPeak(3265.0, 24.0, "Medium", "Secondary amine N-H stretch"),
                IrPeak(1580.0, 10.0, "Strong", "Quinoline C=N & C=C conjugated system"),
                IrPeak(1080.0, 22.0, "Medium", "Aryl C-Cl stretch")
            ),
            hNmrPeaks = listOf(
                HNmrPeak(8.51, "d", 1, 5.4, "Quinoline H-2"),
                HNmrPeak(8.02, "d", 1, 9.0, "Quinoline H-5"),
                HNmrPeak(7.92, "d", 1, 2.0, "Quinoline H-8"),
                HNmrPeak(6.42, "d", 1, 5.4, "Quinoline H-3"),
                HNmrPeak(3.72, "m", 1, 6.5, "Chiral CH-NH"),
                HNmrPeak(2.54, "q", 4, 7.1, "Diethylamino -N(CH2)2"),
                HNmrPeak(1.03, "t", 6, 7.1, "Diethylamino methyls")
            ),
            cNmrPeaks = listOf(
                CNmrPeak(151.9, "C2 Quinoline"),
                CNmrPeak(149.4, "C4-NH"),
                CNmrPeak(134.9, "C7-Cl"),
                CNmrPeak(52.4, "CH2-NEt2")
            ),
            msFragments = listOf(
                MsFragment(320.19, 100.0, "C18H27ClN3+", "[M+H]+ with classic 3:1 M+2 (322.19 -> 1 Cl)"),
                MsFragment(247.09, 64.0, "C14H16ClN2+", "Loss of NHEt2 (-73 Da)"),
                MsFragment(86.10, 88.0, "C5H12N+", "Diethyliminium CH2=NEt2+ base cleavage")
            ),
            wetBenchTests = listOf(
                WetBenchTest("Beilstein Flame / AgNO3", "Cu wire / Na fusion", "Green flame & 3:1 HRMS M+2 isotope ratio", true, "Confirms Chlorine (-Cl) halogen"),
                WetBenchTest("UV 330nm Fluorescence", "pH 5 vs pH 9 buffer", "Strong pH-dependent blue fluorescence", true, "4-Aminoquinoline chromophore")
            ),
            ec50Nm = 42.0, emaxPct = 96.0, hillSlope = 1.4,
            ed50MgKg = 6.0, ld50MgKg = 88.0,
            bioavailabilityPct = 89.0, halfLifeHr = 18.0, vdL = 800.0, kaPerHr = 1.6, mecMgL = 0.05, mtcMgL = 0.8
        ),

        // 8. Acyclovir
        MolecularStructureProfile(
            id = "acyclovir",
            unknownCode = "Unknown Sample H-830",
            name = "Acyclovir",
            formula = "C8H11N5O3",
            monoisotopicMass = 225.0862,
            ihd = 6,
            iupacName = "2-amino-9-(2-hydroxyethoxymethyl)-1,9-dihydro-6H-purin-6-one",
            therapeuticClass = "Antiviral Nucleoside Analog (HSV/VZV)",
            targetReceptor = "Viral Thymidine Kinase -> Viral DNA Polymerase",
            mechanism = "Activated selectively by viral thymidine kinase; converted to triphosphate that acts as obligate DNA chain terminator (lacks 3'-OH).",
            pkaValues = listOf(2.27, 9.25),
            functionalGroupTags = listOf("Purine / Xanthine Heterocycle", "Primary Amine (-NH2)", "Amide (-CONH-)", "Acyclic Ether / Alcohol"),
            lipinski = LipinskiMetrics(225.20, -1.56, 3, 7, 114.8, 4, 0, false),
            atoms = listOf(
                MolAtom3D("n1", "N", "Guanine N1-H", "Purine / Xanthine Heterocycle", -0.50f, 0.25f, 0.0f),
                MolAtom3D("c2", "C", "C2 (2-NH2)", "Purine / Xanthine Heterocycle", -0.50f, -0.08f, 0.04f),
                MolAtom3D("n3", "N", "N3", "Purine / Xanthine Heterocycle", -0.24f, -0.26f, 0.04f),
                MolAtom3D("c4", "C", "C4 Bridgehead", "Purine / Xanthine Heterocycle", 0.02f, -0.08f, 0.0f),
                MolAtom3D("c5", "C", "C5 Bridgehead", "Purine / Xanthine Heterocycle", 0.02f, 0.22f, -0.02f),
                MolAtom3D("c6", "C", "C6=O Lactam", "Purine / Xanthine Heterocycle", -0.24f, 0.40f, -0.02f),
                MolAtom3D("n9", "N", "N9 Glycosidic Site", "Purine / Xanthine Heterocycle", 0.28f, -0.18f, -0.02f),
                MolAtom3D("nh2", "N", "C2-NH2 Donor", "Primary Amine (-NH2)", -0.76f, -0.22f, 0.08f),
                MolAtom3D("o6", "O", "C6=O Acceptor", "Amide (-CONH-)", -0.24f, 0.68f, -0.04f),
                MolAtom3D("et1", "C", "N9-CH2-O- Mimic", "Acyclic Ether / Alcohol", 0.54f, -0.34f, 0.08f, "sp3"),
                MolAtom3D("eo", "O", "Ether -O-", "Acyclic Ether / Alcohol", 0.72f, -0.14f, 0.16f, "sp3"),
                MolAtom3D("oh", "O", "5'-OH Phosphorylation Site", "Acyclic Ether / Alcohol", 0.94f, -0.38f, -0.02f, "sp3")
            ),
            bonds = listOf(
                MolBond3D(0, 1, 1, true), MolBond3D(1, 2, 2, true), MolBond3D(2, 3, 1, true),
                MolBond3D(3, 4, 2, true), MolBond3D(4, 5, 1, true), MolBond3D(5, 0, 1, true),
                MolBond3D(3, 6, 1, true), MolBond3D(1, 7, 1), MolBond3D(5, 8, 2),
                MolBond3D(6, 9, 1), MolBond3D(9, 10, 1), MolBond3D(10, 11, 1)
            ),
            irPeaks = listOf(
                IrPeak(3440.0, 16.0, "Strong", "O-H + 2-NH2 stretches"),
                IrPeak(1712.0, 12.0, "Strong", "Guanine C6=O stretch"),
                IrPeak(1105.0, 22.0, "Strong", "Acyclic C-O-C ether stretch")
            ),
            hNmrPeaks = listOf(
                HNmrPeak(10.65, "s", 1, 0.0, "N1-H lactam"),
                HNmrPeak(7.82, "s", 1, 0.0, "Guanine H-8"),
                HNmrPeak(6.51, "s", 2, 0.0, "C2-NH2"),
                HNmrPeak(5.36, "s", 2, 0.0, "N9-CH2-O (diagnostic singlet!)"),
                HNmrPeak(4.68, "t", 1, 5.2, "Terminal -OH")
            ),
            cNmrPeaks = listOf(
                CNmrPeak(157.0, "C6=O"),
                CNmrPeak(153.9, "C2-NH2"),
                CNmrPeak(137.9, "C8"),
                CNmrPeak(72.1, "N9-CH2-O"),
                CNmrPeak(60.1, "CH2-OH")
            ),
            msFragments = listOf(
                MsFragment(226.09, 100.0, "C8H12N5O3+", "[M+H]+ (Odd mass + 5 Nitrogens)"),
                MsFragment(152.06, 86.0, "C5H6N5O+", "Protonated Guanine base after side-chain cleavage")
            ),
            wetBenchTests = listOf(
                WetBenchTest("Ninhydrin / Diazotization", "HNO2", "N2 bubbles evolved", true, "Exocyclic primary aromatic -NH2 present"),
                WetBenchTest("Periodate Ribose Test", "NaIO4", "No cis-diol cleavage", false, "Acyclic ribose mimic (no intact sugar ring)")
            ),
            ec50Nm = 320.0, emaxPct = 97.0, hillSlope = 1.35,
            ed50MgKg = 12.0, ld50MgKg = 1500.0,
            bioavailabilityPct = 22.0, halfLifeHr = 2.9, vdL = 48.0, kaPerHr = 1.2, mecMgL = 0.4, mtcMgL = 25.0
        )
    )

    fun generateFtirCurve(mol: MolecularStructureProfile): List<Pair<Double, Double>> {
        // 4000 down to 500 cm-1 sampled every 50 cm-1
        val pts = mutableListOf<Pair<Double, Double>>()
        var w = 500.0
        while (w <= 4000.0) {
            var absorbance = 0.04
            mol.irPeaks.forEach { pk ->
                val width = if (pk.intensity.contains("Broad", true)) 170.0 else 42.0
                val depth = (100.0 - pk.transmittancePct) / 100.0
                val dist = (w - pk.wavenumberCm1) / width
                absorbance += depth * exp(-0.5 * dist * dist)
            }
            val transmittance = (100.0 * (1.0 - absorbance.coerceIn(0.0, 0.96))).coerceIn(4.0, 99.0)
            pts += w to transmittance
            w += 50.0
        }
        return pts
    }

    fun generateHNmrCurve(mol: MolecularStructureProfile): List<Pair<Double, Double>> {
        val pts = mutableListOf<Pair<Double, Double>>()
        var ppm = 0.0
        while (ppm <= 12.0) {
            var sig = 0.02
            mol.hNmrPeaks.forEach { pk ->
                val dist = (ppm - pk.ppm) / 0.14
                sig += (pk.protons.toDouble() * 18.0) * exp(-0.5 * dist * dist)
            }
            pts += ppm to sig.coerceAtMost(100.0)
            ppm += 0.15
        }
        return pts
    }

    fun generateTitrationCurve(mol: MolecularStructureProfile): List<Pair<Double, Double>> {
        // pH vs mL Strong Acid/Base Equivalent (0.0 to 2.0 equiv)
        val pka = mol.pkaValues.firstOrNull() ?: 7.0
        val pts = mutableListOf<Pair<Double, Double>>()
        for (i in 0..24) {
            val eq = i / 12.0
            val ph = when {
                eq <= 0.05 -> (pka - 1.4).coerceIn(1.2, 11.0)
                eq < 0.96 -> (pka + kotlin.math.log10(eq / (1.0 - eq))).coerceIn(1.2, 12.5)
                eq in 0.96..1.04 -> (pka + 2.4).coerceIn(5.0, 12.5)
                else -> (pka + 2.8 + (eq - 1.0) * 1.4).coerceIn(2.0, 13.0)
            }
            pts += eq to ph
        }
        return pts
    }

    fun evaluateElucidationHypothesis(
        target: MolecularStructureProfile,
        guessedFormula: String,
        guessedIhd: Int?,
        selectedGroups: Set<String>
    ): ElucidationEvaluation {
        val cleanGuess = guessedFormula.trim().uppercase().replace(" ", "")
        val formulaOk = cleanGuess == target.formula.uppercase()
        val ihdOk = guessedIhd == target.ihd
        val expected = target.functionalGroupTags.toSet()
        val matched = selectedGroups.intersect(expected).sorted()
        val missed = expected.subtract(selectedGroups).sorted()
        val extra = selectedGroups.subtract(expected).sorted()

        var score = 0
        if (formulaOk) score += 30
        if (ihdOk) score += 20
        val groupRecall = if (expected.isNotEmpty()) matched.size.toDouble() / expected.size else 1.0
        val penalty = (extra.size * 6).coerceAtMost(20)
        score += (groupRecall * 50.0).toInt() - penalty
        val clamped = score.coerceIn(0, 100)

        val feedback = when {
            clamped >= 90 -> "Structure Confirmed! Formula, unsaturation (IHD=${target.ihd}), and functional group assignments match ${target.name}."
            clamped >= 65 -> "Strong Partial Elucidation ($clamped%). Re-check missed functional groups via IR/NMR/Wet-Bench clues."
            else -> "More experiments needed ($clamped%). Run HRMS for Formula + IHD, FT-IR for carbonyls/O-H, and Wet-Bench tests."
        }
        return ElucidationEvaluation(clamped, formulaOk, ihdOk, matched, missed, extra, feedback)
    }

    fun runPharmacodynamicsAndPk(
        mol: MolecularStructureProfile,
        doseMg: Double,
        antagonistRatio: Double, // 0.0 to 10.0x Ki
        antagonistMechanism: AntagonistMechanism,
        route: DosingRoute
    ): PdPkSimulationResult {
        val ec50 = mol.ec50Nm
        val emax = mol.emaxPct
        val n = mol.hillSlope

        val schildFactor = when (antagonistMechanism) {
            AntagonistMechanism.NONE -> 1.0
            AntagonistMechanism.COMPETITIVE -> 1.0 + antagonistRatio.coerceAtLeast(0.0)
            AntagonistMechanism.NON_COMPETITIVE -> 1.0
        }
        val emaxModFactor = when (antagonistMechanism) {
            AntagonistMechanism.NON_COMPETITIVE -> 1.0 / (1.0 + 0.55 * antagonistRatio.coerceAtLeast(0.0))
            else -> 1.0
        }
        val apparentEc50 = ec50 * schildFactor
        val apparentEmax = emax * emaxModFactor

        val baselineCurve = mutableListOf<Pair<Double, Double>>()
        val modulatedCurve = mutableListOf<Pair<Double, Double>>()
        // log10(Conc nM) from -1.0 to 6.0 (0.1 nM to 1,000,000 nM)
        for (k in 0..35) {
            val logC = -1.0 + k * 0.20
            val conc = 10.0.pow(logC)
            val effBase = (emax * conc.pow(n)) / (ec50.pow(n) + conc.pow(n))
            val effMod = (apparentEmax * conc.pow(n)) / (apparentEc50.pow(n) + conc.pow(n))
            baselineCurve += logC to effBase
            modulatedCurve += logC to effMod
        }

        // 1-Compartment PK over 0..24 hr
        val f = if (route == DosingRoute.IV_BOLUS) 1.0 else (mol.bioavailabilityPct / 100.0)
        val ke = ln(2.0) / mol.halfLifeHr.coerceAtLeast(0.1)
        val vd = mol.vdL.coerceAtLeast(1.0)
        val ka = mol.kaPerHr.coerceAtLeast(ke + 0.15)
        val pkCurve = mutableListOf<Pair<Double, Double>>()
        var cMax = 0.0
        var tMax = 0.0
        var aucSum = 0.0
        var prevC = 0.0
        val dt = 0.5
        for (step in 0..48) {
            val t = step * dt
            val cp = if (route == DosingRoute.IV_BOLUS) {
                (doseMg * f / vd) * exp(-ke * t)
            } else {
                val coeff = (f * doseMg * ka) / (vd * (ka - ke))
                (coeff * (exp(-ke * t) - exp(-ka * t))).coerceAtLeast(0.0)
            }
            pkCurve += t to cp
            if (cp > cMax) {
                cMax = cp
                tMax = t
            }
            if (step > 0) {
                aucSum += 0.5 * (prevC + cp) * dt
            }
            prevC = cp
        }

        val clearance = ke * vd
        val ti = mol.ld50MgKg / mol.ed50MgKg.coerceAtLeast(0.01)
        val status = when {
            cMax < mol.mecMgL -> "Subtherapeutic (Below MEC ${"%.2f".format(mol.mecMgL)} mg/L)"
            cMax > mol.mtcMgL -> "Supra-MTC Toxicity Risk (Exceeds ${"%.1f".format(mol.mtcMgL)} mg/L)"
            else -> "Within Therapeutic Window (Safe & Effective)"
        }
        val interpretation = buildString {
            append("${mol.name} (${"%.0f".format(doseMg)} mg ${if (route == DosingRoute.IV_BOLUS) "IV" else "PO"}): ")
            append("Cmax=${"%.2f".format(cMax)} mg/L at Tmax=${"%.1f".format(tMax)}h, ")
            append("t1/2=${"%.1f".format(mol.halfLifeHr)}h, CL=${"%.1f".format(clearance)} L/h. ")
            when (antagonistMechanism) {
                AntagonistMechanism.NONE -> append("Unopposed agonist achieves Emax=${"%.0f".format(apparentEmax)}% at EC50=${"%.0f".format(apparentEc50)} nM.")
                AntagonistMechanism.COMPETITIVE -> append("Competitive antagonist produces a parallel ${"%.1f".format(schildFactor)}x rightward shift in EC50 with full Emax surmountability.")
                AntagonistMechanism.NON_COMPETITIVE -> append("Non-competitive blockade depresses Emax from ${"%.0f".format(emax)}% to ${"%.0f".format(apparentEmax)}% (insurmountable).")
            }
        }

        return PdPkSimulationResult(
            drugName = mol.name,
            doseMg = doseMg,
            route = route,
            antagonistMechanism = antagonistMechanism,
            baselineCurve = baselineCurve,
            modulatedCurve = modulatedCurve,
            apparentEc50Nm = apparentEc50,
            apparentEmaxPct = apparentEmax,
            schildRatio = schildFactor,
            therapeuticIndex = ti,
            pkCurve = pkCurve,
            mecLine = listOf(0.0 to mol.mecMgL, 24.0 to mol.mecMgL),
            mtcLine = listOf(0.0 to mol.mtcMgL, 24.0 to mol.mtcMgL),
            cMaxMgL = cMax,
            tMaxHr = tMax,
            aucMgHrL = aucSum,
            halfLifeHr = mol.halfLifeHr,
            clearanceLhr = clearance,
            windowStatus = status,
            clinicalInterpretation = interpretation
        )
    }
}
