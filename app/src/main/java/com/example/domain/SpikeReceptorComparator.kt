package com.example.domain

import androidx.compose.ui.graphics.Color
import com.example.ui.components.Bond3D
import com.example.ui.components.LegendItem3D
import com.example.ui.components.Scene3DModel
import com.example.ui.components.Sphere3D
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

data class ViralSpikeEntry(
    val id: String,
    val virusName: String,
    val spikeProteinName: String,
    val pdbId: String,
    val trimerClass: String,
    val cognateHostReceptorId: String,
    val cognateHostReceptorName: String,
    val secondaryCoFactor: String,
    val kdNanomolar: Double,
    val glycanShieldSites: Int,
    val antigenicDriftIndex: Double,
    val replicationFitness: Double,
    val immuneEvasionIndex: Double,
    val tissueTropism: String,
    val antibodyVulnerability: String,
    val pathogenClass: PathogenClass,
    val tropismTarget: TropismTarget,
    val accentHex: Long
)

data class HostSurfaceReceptorEntry(
    val id: String,
    val receptorName: String,
    val pdbId: String,
    val molecularFamily: String,
    val cellularDistribution: String,
    val nativePhysiologicalRole: String,
    val recognizedMotif: String,
    val defenseResponseRole: String,
    val accentHex: Long
)

data class SpikeReceptorComparisonResult(
    val spike: ViralSpikeEntry,
    val receptor: HostSurfaceReceptorEntry,
    val isCognateMatch: Boolean,
    val bindingAffinityScore: Double,
    val entryEfficiencyPct: Int,
    val projectedTransmissibilityReff: Double,
    val immuneEvasionRiskPct: Int,
    val hostResponseLoadScore: Double,
    val summaryInterpretation: String
)

object SpikeReceptorComparator {
    val VIRAL_SPIKES: List<ViralSpikeEntry> = listOf(
        ViralSpikeEntry(
            id = "sarscov2_jn1",
            virusName = "SARS-CoV-2 (Omicron JN.1)",
            spikeProteinName = "Trimeric Spike (S1 RBD + S2 Fusion)",
            pdbId = "8Y5J",
            trimerClass = "Class I Fusion Glycoprotein",
            cognateHostReceptorId = "ace2",
            cognateHostReceptorName = "Human ACE2",
            secondaryCoFactor = "Endosomal Cathepsin B/L & TMPRSS2",
            kdNanomolar = 6.2,
            glycanShieldSites = 22,
            antigenicDriftIndex = 1.32,
            replicationFitness = 1.55,
            immuneEvasionIndex = 0.92,
            tissueTropism = "Upper Airway Epithelium (Bronchi & Nasopharynx)",
            antibodyVulnerability = "Class 3 / S2 Stem-Helix Conserved Epitopes",
            pathogenClass = PathogenClass.ENVELOPED_RNA_VIRUS,
            tropismTarget = TropismTarget.UPPER_RESPIRATORY,
            accentHex = 0xFF22E5FF
        ),
        ViralSpikeEntry(
            id = "sarscov2_delta",
            virusName = "SARS-CoV-2 (Delta B.1.617.2)",
            spikeProteinName = "Spike (P681R Furin Cleaved S1/S2)",
            pdbId = "7W92",
            trimerClass = "Class I Fusion Glycoprotein",
            cognateHostReceptorId = "ace2",
            cognateHostReceptorName = "Human ACE2",
            secondaryCoFactor = "Cell-Surface TMPRSS2 Protease",
            kdNanomolar = 11.4,
            glycanShieldSites = 22,
            antigenicDriftIndex = 0.55,
            replicationFitness = 1.48,
            immuneEvasionIndex = 0.80,
            tissueTropism = "Lower Alveolar Type II Pneumocytes",
            antibodyVulnerability = "Class 1 & Class 2 RBD Neutralizing Antibodies",
            pathogenClass = PathogenClass.ENVELOPED_RNA_VIRUS,
            tropismTarget = TropismTarget.LOWER_ALVEOLAR,
            accentHex = 0xFFFF5D73
        ),
        ViralSpikeEntry(
            id = "influenza_h1n1",
            virusName = "Influenza A (H1N1 pdm09)",
            spikeProteinName = "Hemagglutinin (HA1/HA2 Trimer)",
            pdbId = "3LZG",
            trimerClass = "Class I pH-Triggered Fusion Trimer",
            cognateHostReceptorId = "sialic_a26",
            cognateHostReceptorName = "α2,6-Linked Sialic Acid",
            secondaryCoFactor = "Neuraminidase (NA) & Endosomal Acidification",
            kdNanomolar = 140.0,
            glycanShieldSites = 11,
            antigenicDriftIndex = 0.72,
            replicationFitness = 1.32,
            immuneEvasionIndex = 0.58,
            tissueTropism = "Ciliated Tracheal & Bronchial Epithelium",
            antibodyVulnerability = "HA Head RBS & Conserved HA2 Stem bNAbs",
            pathogenClass = PathogenClass.SEGMENTED_RNA_VIRUS,
            tropismTarget = TropismTarget.UPPER_RESPIRATORY,
            accentHex = 0xFFFFC857
        ),
        ViralSpikeEntry(
            id = "hiv1_gp120",
            virusName = "HIV-1 (Clade B R5-Tropic)",
            spikeProteinName = "Env gp120/gp41 Trimer",
            pdbId = "1GC1",
            trimerClass = "Class I Dense-Glycan Envelope",
            cognateHostReceptorId = "cd4_ccr5",
            cognateHostReceptorName = "CD4 + CCR5 Coreceptor",
            secondaryCoFactor = "CXCR4 (X4 Switch in Late Stages)",
            kdNanomolar = 4.1,
            glycanShieldSites = 28,
            antigenicDriftIndex = 1.42,
            replicationFitness = 0.85,
            immuneEvasionIndex = 1.38,
            tissueTropism = "CD4+ Helper T Cells & Mucosal Macrophages",
            antibodyVulnerability = "CD4-Binding Site (VRC01), V3-Glycan, MPER",
            pathogenClass = PathogenClass.RETROVIRUS,
            tropismTarget = TropismTarget.CD4_LYMPHOID,
            accentHex = 0xFFE04FB0
        ),
        ViralSpikeEntry(
            id = "rsv_f_protein",
            virusName = "RSV (Respiratory Syncytial Virus)",
            spikeProteinName = "Pre-Fusion F & Attachment G",
            pdbId = "4JHW",
            trimerClass = "Class I Spring-Loaded Fusion Trimer",
            cognateHostReceptorId = "cx3cr1_nuc",
            cognateHostReceptorName = "CX3CR1 / Nucleolin",
            secondaryCoFactor = "Heparan Sulfate Proteoglycans (HSPG)",
            kdNanomolar = 18.0,
            glycanShieldSites = 6,
            antigenicDriftIndex = 0.30,
            replicationFitness = 1.24,
            immuneEvasionIndex = 0.74,
            tissueTropism = "Bronchiolar Syncytial Ciliated Cells",
            antibodyVulnerability = "Pre-F Site Ø (Nirsevimab) & Site II (Palivizumab)",
            pathogenClass = PathogenClass.ENVELOPED_RNA_VIRUS,
            tropismTarget = TropismTarget.LOWER_ALVEOLAR,
            accentHex = 0xFF3DDC97
        ),
        ViralSpikeEntry(
            id = "ebola_gp",
            virusName = "Ebola Zaire Filovirus",
            spikeProteinName = "Glycoprotein GP1,2 (Chalice Trimer)",
            pdbId = "5JQ3",
            trimerClass = "Class I Endosomal Cleavage Trimer",
            cognateHostReceptorId = "npc1",
            cognateHostReceptorName = "Endosomal NPC1",
            secondaryCoFactor = "TIM-1 Macropinocytosis + Cathepsin B/L Trim",
            kdNanomolar = 85.0,
            glycanShieldSites = 17,
            antigenicDriftIndex = 0.28,
            replicationFitness = 1.18,
            immuneEvasionIndex = 1.35,
            tissueTropism = "Dendritic Cells, Macrophages & Hepatocytes",
            antibodyVulnerability = "GP1/GP2 Base & NPC1-Binding Cap (Inmazeb/Ebanga)",
            pathogenClass = PathogenClass.ENVELOPED_RNA_VIRUS,
            tropismTarget = TropismTarget.MACROPHAGE_NICHE,
            accentHex = 0xFF8B6BFF
        ),
        ViralSpikeEntry(
            id = "measles_h",
            virusName = "Measles Morbillivirus",
            spikeProteinName = "Hemagglutinin (H) β-Propeller + F",
            pdbId = "2ZB6",
            trimerClass = "Paramyxovirus Tetrameric H + Trimeric F",
            cognateHostReceptorId = "slam_nectin4",
            cognateHostReceptorName = "SLAM (CD150) & Nectin-4",
            secondaryCoFactor = "CD46 (Vaccine Edmonston Strain Adaptation)",
            kdNanomolar = 45.0,
            glycanShieldSites = 5,
            antigenicDriftIndex = 0.08,
            replicationFitness = 1.45,
            immuneEvasionIndex = 0.85,
            tissueTropism = "Memory B/T Lymphocytes -> Basolateral Airway",
            antibodyVulnerability = "Monotypic H Receptor-Binding Head (Lifelong Vaccine Efficacy)",
            pathogenClass = PathogenClass.ENVELOPED_RNA_VIRUS,
            tropismTarget = TropismTarget.CD4_LYMPHOID,
            accentHex = 0xFF70A1FF
        )
    )

    val HOST_RECEPTORS: List<HostSurfaceReceptorEntry> = listOf(
        HostSurfaceReceptorEntry(
            id = "ace2",
            receptorName = "ACE2 (Angiotensin-Converting Enzyme 2)",
            pdbId = "6M0J",
            molecularFamily = "Zinc Carboxypeptidase Ectoenzyme",
            cellularDistribution = "Nasal Goblet, Ciliated Airway, Alveolar Type II, Enterocytes",
            nativePhysiologicalRole = "Converts Angiotensin II -> Ang(1-7) to vasodilate & dampen lung inflammation.",
            recognizedMotif = "SARS-CoV-2 Spike S1 Receptor-Binding Motif (RBM)",
            defenseResponseRole = "Shedding by ADAM17 reduces cell-surface entry sites; soluble ACE2 acts as decoy.",
            accentHex = 0xFF22E5FF
        ),
        HostSurfaceReceptorEntry(
            id = "sialic_a26",
            receptorName = "α2,6-Sialic Acid Glycocalyx",
            pdbId = "3LZG",
            molecularFamily = "Terminal N-Acetylneuraminic Acid Glycans",
            cellularDistribution = "Human Upper Nasopharynx, Trachea & Bronchial Mucosa",
            nativePhysiologicalRole = "Maintains negative surface charge, mucus hydration, and Siglec self-recognition.",
            recognizedMotif = "Influenza Hemagglutinin HA1 Receptor-Binding Pocket",
            defenseResponseRole = "Mucins act as soluble sialylated decoys cleared rapidly via mucociliary escalator.",
            accentHex = 0xFFFFC857
        ),
        HostSurfaceReceptorEntry(
            id = "cd4_ccr5",
            receptorName = "CD4 + CCR5 Coreceptor Axis",
            pdbId = "1GC1",
            molecularFamily = "4-Ig-Domain Co-receptor + 7-TM GPCR",
            cellularDistribution = "Helper CD4+ T Cells, Tissue Macrophages, Dendritic Cells",
            nativePhysiologicalRole = "Stabilizes TCR–MHC Class II synapse & recruits Lck kinase; guides CCR5+ homing.",
            recognizedMotif = "HIV-1 gp120 Phe43 Cavity + V3 Loop Sulfotyrosine Motif",
            defenseResponseRole = "CCR5 Δ32 polymorphism and β-chemokines (CCL3/4/5) competitively block entry.",
            accentHex = 0xFFE04FB0
        ),
        HostSurfaceReceptorEntry(
            id = "cx3cr1_nuc",
            receptorName = "CX3CR1 / Apical Nucleolin",
            pdbId = "4JHW",
            molecularFamily = "Fractalkine Chemokine GPCR + Surface Chaperone",
            cellularDistribution = "Ciliated Bronchiolar Epithelial Cells & NK/Monocyte Subsets",
            nativePhysiologicalRole = "Senses CX3CL1 endothelial adhesion cues and stabilizes ciliary membranes.",
            recognizedMotif = "RSV G CX3C Chemokine Mimicry Motif & Pre-F Helix",
            defenseResponseRole = "Mucosal sIgA and Pre-F antibodies sterically prevent F-protein harpoon refolding.",
            accentHex = 0xFF3DDC97
        ),
        HostSurfaceReceptorEntry(
            id = "npc1",
            receptorName = "NPC1 (Niemann-Pick C1 Cholesterol Receptor)",
            pdbId = "5F1B",
            molecularFamily = "13-Pass Late Endosomal/Lysosomal Sterol Transporter",
            cellularDistribution = "Endolysosomal Membranes of Macrophages, DCs & Endothelium",
            nativePhysiologicalRole = "Exports LDL-derived unesterified cholesterol out of late endosomes.",
            recognizedMotif = "Cathepsin-Cleaved Ebola GP1 Receptor-Binding Core",
            defenseResponseRole = "IFITM2/IFITM3 endosomal restriction factors stiffen bilayers to block GP2 fusion.",
            accentHex = 0xFF8B6BFF
        ),
        HostSurfaceReceptorEntry(
            id = "slam_nectin4",
            receptorName = "SLAM (CD150) & Nectin-4",
            pdbId = "3ALZ",
            molecularFamily = "Ig-Superfamily Adhesion & Signaling Receptors",
            cellularDistribution = "Activated Memory T/B Cells, Thymocytes & Basolateral Airway",
            nativePhysiologicalRole = "Co-stimulates lymphocyte proliferation and epithelial adherens junctions.",
            recognizedMotif = "Morbillivirus Hemagglutinin (H) 6-Bladed β-Propeller",
            defenseResponseRole = "Type I IFN-stimulated MX1 & robust neutralizing IgG halt cell-to-cell spread.",
            accentHex = 0xFF70A1FF
        )
    )

    fun comparePair(
        spike: ViralSpikeEntry,
        receptor: HostSurfaceReceptorEntry
    ): SpikeReceptorComparisonResult {
        val isCognate = spike.cognateHostReceptorId == receptor.id
        // Kd lower = higher structural complementarity
        val rawKdScore = (100.0 - 16.0 * kotlin.math.ln(max(1.0, spike.kdNanomolar))).coerceIn(22.0, 96.0)
        val affinityScore = if (isCognate) rawKdScore else (rawKdScore * 0.22).coerceIn(4.0, 26.0)

        val entryEff = if (isCognate) {
            (0.65 * affinityScore + 22.0 * spike.replicationFitness).toInt().coerceIn(35, 98)
        } else {
            (affinityScore * 0.6).toInt().coerceIn(2, 18)
        }

        val escapeRisk = ((spike.glycanShieldSites * 1.4) + (spike.antigenicDriftIndex * 28.0) +
            (spike.immuneEvasionIndex * 18.0)).toInt().coerceIn(10, 96)

        val rawReff = if (isCognate) {
            (1.2 + (entryEff / 100.0) * spike.replicationFitness * (1.0 + 0.55 * spike.antigenicDriftIndex) * 3.1)
                .coerceIn(0.8, 15.0)
        } else {
            0.12
        }

        // Composite educational biological impact metric integrating replication, evasion, and entry fit
        val responseLoad = if (isCognate) {
            (0.35 * entryEff + 0.35 * escapeRisk + 18.0 * spike.replicationFitness).coerceIn(15.0, 96.0)
        } else {
            8.0
        }

        val note = if (isCognate) {
            "Cognate high-affinity lock-and-key fit (${"%.1f".format(spike.kdNanomolar)} nM) onto ${receptor.receptorName}. " +
                "Glycan shield (${spike.glycanShieldSites} N-glycans) & drift (${"%.2f".format(spike.antigenicDriftIndex)}) modulate neutralizing antibody pressure toward ${spike.antibodyVulnerability}."
        } else {
            "Tropism mismatch: ${spike.spikeProteinName} lacks complementary contact geometry for ${receptor.receptorName} (physiological target is ${spike.cognateHostReceptorName}), yielding negligible productive entry."
        }

        return SpikeReceptorComparisonResult(
            spike = spike,
            receptor = receptor,
            isCognateMatch = isCognate,
            bindingAffinityScore = affinityScore,
            entryEfficiencyPct = entryEff,
            projectedTransmissibilityReff = rawReff,
            immuneEvasionRiskPct = escapeRisk,
            hostResponseLoadScore = responseLoad,
            summaryInterpretation = note
        )
    }

    fun toPathogenDesign(spike: ViralSpikeEntry): PathogenDesign = PathogenDesign(
        id = spike.id,
        name = spike.virusName,
        pathogenClass = spike.pathogenClass,
        tropism = spike.tropismTarget,
        replicationRate = spike.replicationFitness,
        receptorAffinity = (1.6 - kotlin.math.ln(max(1.0, spike.kdNanomolar)) * 0.14).coerceIn(0.6, 1.9),
        immuneEvasion = spike.immuneEvasionIndex,
        antigenicDrift = spike.antigenicDriftIndex,
        cytokineTrigger = 0.72,
        tissueInvasiveness = 0.78,
        referenceNotes = "${spike.spikeProteinName} -> ${spike.cognateHostReceptorName} (Kd ${spike.kdNanomolar} nM)"
    )

    fun buildSpikeHostDocking3DScene(
        spike: ViralSpikeEntry,
        receptor: HostSurfaceReceptorEntry,
        phase: Float
    ): Scene3DModel {
        val isMatch = spike.cognateHostReceptorId == receptor.id
        val spikeCol = Color(spike.accentHex)
        val recCol = Color(receptor.accentHex)
        val spheres = mutableListOf<Sphere3D>()
        val bonds = mutableListOf<Bond3D>()

        // Docked distance tightens cleanly if cognate receptor, repels slightly if mismatched
        val gap = if (isMatch) 0.08f + 0.04f * sin(phase * 3.14f) else 0.36f + 0.06f * sin(phase * 3.14f)

        // Viral Spike Trimer (Upper hemisphere descending to dock)
        spheres += Sphere3D("viral_membrane", "Viral Envelope Anchor", 0f, 0.72f, 0f, 0.22f, Color(0xFF8B6BFF), 0.75f, "Virus")
        for (t in 0..2) {
            val ang = t * 2.094f
            val sx = 0.16f * cos(ang)
            val sz = 0.16f * sin(ang)
            val yTop = 0.50f
            val yRbd = gap + 0.09f
            spheres += Sphere3D("spike_stalk_$t", "${spike.spikeProteinName} Protomer", sx, yTop, sz, 0.12f, spikeCol, 0.90f, "Spike S2")
            spheres += Sphere3D(
                id = if (t == 0) "spike_rbd" else "rbd_$t",
                label = "${spike.spikeProteinName} Receptor-Binding Tip",
                x = sx * 0.75f, y = yRbd, z = sz * 0.75f,
                radius = 0.13f,
                color = spikeCol,
                alpha = 0.98f,
                groupTag = "Spike RBD"
            )
            bonds += Bond3D(0f, 0.72f, 0f, sx, yTop, sz, spikeCol.copy(alpha = 0.6f), 2.5f)
            bonds += Bond3D(sx, yTop, sz, sx * 0.75f, yRbd, sz * 0.75f, spikeCol, 3.0f)
        }
        // Glycan shield markers around spike stalk
        val glycanNodes = min(6, max(2, spike.glycanShieldSites / 5))
        for (g in 0 until glycanNodes) {
            val a = g * (6.283f / glycanNodes) + phase * 0.5f
            spheres += Sphere3D(
                id = if (g == 0) "glycan_shield" else "gly_$g",
                label = "N-Linked Glycan Shield (${spike.glycanShieldSites} sites)",
                x = 0.26f * cos(a),
                y = 0.44f,
                z = 0.26f * sin(a),
                radius = 0.06f,
                color = Color(0xFFFFC857),
                alpha = 0.90f,
                groupTag = "Glycans"
            )
        }

        // Host Cell Membrane & Surface Receptor Ectodomain (Lower hemisphere)
        for (m in -2..2) {
            spheres += Sphere3D("host_bilayer_$m", "Host Cell Plasma Membrane", m * 0.24f, -0.72f, 0f, 0.11f, Color(0xFF3DDC97), 0.55f, "Host Membrane")
        }
        spheres += Sphere3D("host_receptor", receptor.receptorName, 0f, -0.20f - gap * 0.35f, 0f, 0.22f, recCol, 0.96f, "Host Receptor")
        spheres += Sphere3D("host_cofactor", "Co-Factor: ${spike.secondaryCoFactor.take(28)}", 0.34f, -0.45f, 0.15f, 0.13f, Color(0xFFE04FB0), 0.92f, "Co-Factor")
        bonds += Bond3D(0f, -0.72f, 0f, 0f, -0.20f - gap * 0.35f, 0f, recCol, 3.5f)

        // Highlight hydrogen-bond / salt-bridge interface if cognate match
        if (isMatch) {
            bonds += Bond3D(0.08f, gap + 0.09f, 0f, 0.04f, -0.20f - gap * 0.35f, 0f, Color(0xFF2EE6C5), 3.0f)
            bonds += Bond3D(-0.08f, gap + 0.09f, 0.05f, -0.04f, -0.20f - gap * 0.35f, 0.04f, Color(0xFF2EE6C5), 3.0f)
        }

        return Scene3DModel(
            title = "3D Spike–Receptor Interface (${spike.pdbId} × ${receptor.pdbId})",
            spheres = spheres,
            bonds = bonds,
            caption = if (isMatch) {
                "Cognate Lock-and-Key Fit • Kd ${spike.kdNanomolar} nM • ${spike.tissueTropism}"
            } else {
                "Non-Cognate Mismatch • Steric/Electrostatic Rejection • Low Entry"
            },
            legendItems = listOf(
                LegendItem3D("Viral Spike", spikeCol, "spike_rbd"),
                LegendItem3D("Glycan Shield", Color(0xFFFFC857), "glycan_shield"),
                LegendItem3D("Host Receptor", recCol, "host_receptor"),
                LegendItem3D("Host Co-Factor", Color(0xFFE04FB0), "host_cofactor")
            )
        )
    }
}
