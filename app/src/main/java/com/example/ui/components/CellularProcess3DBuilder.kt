package com.example.ui.components

import androidx.compose.ui.graphics.Color
import com.example.data.kb.CellProcessInfo
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Generates an accurate, step-reactive, continuously animated 3D arena for each
 * cellular biology process in [com.example.data.kb.ImmunologyKb.PROCESSES],
 * animating all involved organelles and molecular complexes in concert.
 */
fun buildCellularProcess3DScene(
    process: CellProcessInfo,
    continuousProgress: Float,
    selectedObjectId: String? = null
): Scene3DModel {
    val totalSteps = process.steps.size.coerceAtLeast(1)
    val clamped = continuousProgress.coerceIn(0f, totalSteps.toFloat())
    val stepIdx = clamped.toInt().coerceIn(0, totalSteps - 1)
    val frac = clamped - stepIdx
    val u = ((stepIdx + frac) / totalSteps.toFloat()).coerceIn(0f, 1f)
    val pulse = 0.5f + 0.5f * sin(u * PI.toFloat() * 4f)

    val spheres = mutableListOf<Sphere3D>()
    val bonds = mutableListOf<Bond3D>()
    val legend = mutableListOf<LegendItem3D>()

    fun focusAlpha(id: String, base: Float): Float =
        if (selectedObjectId != null && selectedObjectId == id) 1.0f else base

    when (process.id) {
        "endocytosis" -> {
            val invag = (0.28f + 0.68f * u).coerceIn(0f, 1f)
            // Plasma Membrane bilayer curve bending into a pit
            for (k in -4..4) {
                val x = k * 0.17f
                val dip = if (kotlin.math.abs(k) <= 2) -0.30f * invag * (1f - kotlin.math.abs(k) * 0.32f) else 0f
                spheres += Sphere3D(
                    id = if (k == -4) "plasma_membrane" else "mem_$k",
                    label = "Plasma Membrane Phospholipid Bilayer",
                    x = x, y = 0.45f + dip, z = 0f,
                    radius = 0.065f,
                    color = Color(0xFF22E5FF),
                    alpha = focusAlpha("plasma_membrane", 0.85f),
                    groupTag = "Membrane"
                )
                if (k > -4) {
                    val prevDip = if (kotlin.math.abs(k - 1) <= 2) -0.30f * invag * (1f - kotlin.math.abs(k - 1) * 0.32f) else 0f
                    bonds += Bond3D((k - 1) * 0.17f, 0.45f + prevDip, 0f, x, 0.45f + dip, 0f, Color(0xFF22E5FF).copy(alpha = 0.45f), 2f)
                }
            }
            // Clathrin Triskelion Lattice lining the invaginating pit
            for (c in -1..1) {
                spheres += Sphere3D(
                    id = if (c == 0) "clathrin_coat" else "clath_$c",
                    label = "Clathrin Triskelion Coat + AP-2 Adaptor",
                    x = c * 0.16f, y = 0.25f - 0.18f * invag, z = 0.10f * (c % 2),
                    radius = 0.085f,
                    color = Color(0xFFFFC857),
                    alpha = focusAlpha("clathrin_coat", 0.94f),
                    groupTag = "Clathrin"
                )
            }
            // Dynamin-2 GTPase Scission Collar pinching neck during Step 2
            val dynScale = 0.14f * (1f - 0.45f * ((u - 0.33f) / 0.35f).coerceIn(0f, 1f))
            spheres += Sphere3D("dynamin_collar", "Dynamin-2 GTPase Pinch Collar", 0.12f, 0.28f - 0.10f * u, -0.06f, dynScale.coerceAtLeast(0.06f), Color(0xFFE04FB0), focusAlpha("dynamin_collar", 0.96f), "Dynamin")
            // Extracellular Antigen + Engaged Surface Receptor (BCR / FcR)
            val cargoY = 0.64f - 0.98f * u
            spheres += Sphere3D("cargo_antigen", "Soluble Antigen Ligand + Surface Receptor", 0f, cargoY, 0.04f, 0.13f, Color(0xFFFF5D73), focusAlpha("cargo_antigen", 0.98f), "Cargo/Receptor")
            // Early Endosome Organelle + Rab5 GTPase + V-ATPase Proton Pumps
            spheres += Sphere3D("early_endosome", "Early Endosome Organelle (pH ~5.8)", 0f, -0.36f, 0f, 0.34f + 0.08f * u, Color(0xFF8B6BFF), focusAlpha("early_endosome", 0.32f), "Endosome")
            spheres += Sphere3D("rab5_gtpase", "Rab5 GTPase + EEA1 Tethering Factor", -0.34f, -0.36f, 0.12f, 0.09f, Color(0xFF2EE6C5), focusAlpha("rab5_gtpase", 0.95f), "Rab5")
            for (p in 0..3) {
                val ang = p * 1.57f + u * 2.4f
                val px = 0.36f * cos(ang)
                val py = -0.36f + 0.24f * sin(ang)
                spheres += Sphere3D(if (p == 0) "v_atpase" else "hpump_$p", "V-ATPase H+ Proton Pump", px, py, 0.14f * sin(ang), 0.07f, Color(0xFF3DDC97), focusAlpha("v_atpase", 0.94f), "V-ATPase")
                bonds += Bond3D(0f, -0.36f, 0f, px, py, 0.14f * sin(ang), Color(0xFF3DDC97).copy(alpha = 0.45f), 2f)
            }
            legend += LegendItem3D("Membrane + Clathrin", Color(0xFFFFC857), "clathrin_coat")
            legend += LegendItem3D("Dynamin / Rab5", Color(0xFFE04FB0), "dynamin_collar")
            legend += LegendItem3D("Antigen + Receptor", Color(0xFFFF5D73), "cargo_antigen")
            legend += LegendItem3D("Endosome + V-ATPase", Color(0xFF8B6BFF), "early_endosome")
        }

        "exocytosis" -> {
            // Nucleus -> Rough ER -> Trans-Golgi -> MTOC Centrosome -> Kinesin Vesicle -> SNARE -> Secreted Molecules
            spheres += Sphere3D("er_source", "Rough ER & Signal Peptide Entry", -0.68f, -0.52f, -0.10f, 0.18f, Color(0xFF2EE6C5), focusAlpha("er_source", 0.85f), "Rough ER")
            spheres += Sphere3D("trans_golgi", "Trans-Golgi Network (Sorting)", -0.36f, -0.44f, 0f, 0.24f, Color(0xFFE04FB0), focusAlpha("trans_golgi", 0.90f), "Golgi")
            bonds += Bond3D(-0.68f, -0.52f, -0.10f, -0.36f, -0.44f, 0f, Color(0xFF2EE6C5), 2.5f)
            // MTOC Centrosome & Polarized Microtubule Rail toward Immune Synapse
            spheres += Sphere3D("mtoc_centrosome", "MTOC Centrosome + α/β-Tubulin Track", 0.06f, 0.04f, -0.12f, 0.12f, Color(0xFF70A1FF), focusAlpha("mtoc_centrosome", 0.94f), "MTOC")
            bonds += Bond3D(-0.36f, -0.44f, 0f, 0.46f, 0.46f, 0f, Color(0xFF70A1FF), 3.5f)
            // Kinesin-1 Motor + Secretory Granule carrying Perforin/Granzyme/IFN-gamma
            val vx = -0.32f + 0.74f * u
            val vy = -0.40f + 0.84f * u
            spheres += Sphere3D("secretory_vesicle", "Secretory Vesicle (Perforin + Granzyme B + IFN-γ)", vx, vy, 0.08f, 0.16f, Color(0xFFFFC857), focusAlpha("secretory_vesicle", 0.98f), "Granule")
            spheres += Sphere3D("kinesin_motor", "Kinesin-1 ATPase Motor Protein", vx - 0.10f, vy - 0.08f, -0.02f, 0.08f, Color(0xFF22E5FF), focusAlpha("kinesin_motor", 0.95f), "Kinesin")
            bonds += Bond3D(vx, vy, 0.08f, vx - 0.10f, vy - 0.08f, -0.02f, Color(0xFF22E5FF), 2.5f)
            // Synaptotagmin Ca2+ Sensor & v-/t-SNARE Zippering Complex at Plasma Membrane
            spheres += Sphere3D("snare_complex", "v-/t-SNARE + Synaptotagmin (Ca2+ Fusion)", 0.48f, 0.48f, 0f, 0.15f, Color(0xFF22E5FF), focusAlpha("snare_complex", 0.96f), "SNARE")
            val burstStart = ((u - 0.50f) / 0.50f).coerceIn(0f, 1f)
            for (c in 0..5) {
                val a = -0.4f + c * 0.34f
                val dist = 0.10f + 0.44f * burstStart
                spheres += Sphere3D(
                    id = if (c == 0) "released_cytokines" else "rel_$c",
                    label = if (c % 2 == 0) "Secreted Perforin & Granzyme B Molecules" else "Secreted Cytokine (IFN-γ / IL-2)",
                    x = 0.48f + dist * cos(a) * 0.65f,
                    y = 0.50f + dist * sin(a) * 0.55f,
                    z = (c - 2.5f) * 0.08f,
                    radius = 0.065f + 0.025f * burstStart,
                    color = if (c % 2 == 0) Color(0xFFFF5D73) else Color(0xFF3DDC97),
                    alpha = 0.35f + 0.65f * burstStart,
                    groupTag = "Secreted Effectors"
                )
            }
            legend += LegendItem3D("ER → Trans-Golgi", Color(0xFFE04FB0), "trans_golgi")
            legend += LegendItem3D("MTOC & Kinesin-1", Color(0xFF70A1FF), "kinesin_motor")
            legend += LegendItem3D("Granule + SNARE", Color(0xFFFFC857), "secretory_vesicle")
            legend += LegendItem3D("Perforin / IFN-γ", Color(0xFF3DDC97), "released_cytokines")
        }

        "phagocytosis" -> {
            // Microbe coated in C3b / IgG opsonins + Host FcγR/CR3 + Arp2/3 Actin Cup + NOX2 + Lysosome + Phagolysosome ROS
            val microX = 0.56f - 0.72f * u
            spheres += Sphere3D("opsonized_microbe", "Target Microbe (Opsonized w/ IgG & C3b)", microX, 0.05f, 0f, 0.18f, Color(0xFFFF5D73), focusAlpha("opsonized_microbe", 0.98f), "Microbe")
            spheres += Sphere3D("opsonin_tags", "C3b Complement & IgG Fc Opsonin Tags", microX + 0.14f, 0.18f, 0.10f, 0.075f, Color(0xFFE04FB0), focusAlpha("opsonin_tags", 0.96f), "Opsonin")
            val wrap = (u * 1.45f).coerceIn(0f, 1f)
            for (k in -2..2) {
                val ang = k * 0.48f * (1f - 0.45f * wrap)
                val px = microX - 0.24f + 0.22f * cos(ang)
                val py = 0.05f + (if (k == 0) 0f else k * 0.16f * (1f - 0.55f * wrap))
                spheres += Sphere3D(
                    id = if (k == 2) "arp23_actin" else "pseudo_$k",
                    label = "FcγR / CR3 Receptors + Arp2/3 Actin Cup",
                    x = px, y = py, z = 0.08f * sin(k.toFloat()),
                    radius = 0.10f,
                    color = Color(0xFF22E5FF),
                    alpha = focusAlpha("arp23_actin", 0.90f),
                    groupTag = "Actin Cup"
                )
            }
            val lysoX = -0.52f + 0.36f * ((u - 0.35f) / 0.65f).coerceIn(0f, 1f)
            spheres += Sphere3D("lysosome_mpo", "Lysosome Organelle (Myeloperoxidase + Cathepsins)", lysoX, -0.38f + 0.36f * u, 0.10f, 0.16f, Color(0xFFFFC857), focusAlpha("lysosome_mpo", 0.95f), "Lysosome")
            spheres += Sphere3D("nox2_complex", "NOX2 NADPH Oxidase → Superoxide / HOCl ROS", microX - 0.05f, -0.14f, 0.18f, 0.095f + 0.03f * pulse, Color(0xFF3DDC97), focusAlpha("nox2_complex", 0.96f), "NOX2 ROS")
            bonds += Bond3D(lysoX, -0.38f + 0.36f * u, 0.10f, microX, 0.05f, 0f, Color(0xFFFFC857).copy(alpha = 0.65f), 2.8f)
            if (stepIdx >= 1) {
                spheres += Sphere3D("phagolysosome", "Maturing Phagolysosome Compartment", microX, 0.05f, 0f, 0.33f + 0.05f * pulse, Color(0xFF3DDC97), focusAlpha("phagolysosome", 0.28f), "Phagolysosome")
            }
            legend += LegendItem3D("Microbe + IgG/C3b", Color(0xFFFF5D73), "opsonized_microbe")
            legend += LegendItem3D("FcγR / Arp2/3 Actin", Color(0xFF22E5FF), "arp23_actin")
            legend += LegendItem3D("Lysosome + MPO", Color(0xFFFFC857), "lysosome_mpo")
            legend += LegendItem3D("NOX2 ROS Burst", Color(0xFF3DDC97), "nox2_complex")
        }

        "apoptosis" -> {
            // FasL/FasR Death Receptor + Mitochondrion BAX/BAK -> Cytochrome c -> Apaf-1 + Caspase-9/3 -> CAD/Nucleus + PS+ Blebs
            spheres += Sphere3D("fas_disc", "FasR / FADD / Caspase-8 DISC Complex", -0.64f, 0.38f, 0.08f, 0.13f, Color(0xFF3DDC97), focusAlpha("fas_disc", 0.94f), "Death Receptor")
            spheres += Sphere3D("mito_bax", "Mitochondrion (BAX/BAK Outer Pore)", -0.44f, -0.18f, 0f, 0.22f, Color(0xFFFFC857), focusAlpha("mito_bax", 0.92f), "Mitochondria")
            // Moving Cytochrome c shuttle molecule
            val cycX = -0.38f + 0.44f * (u * 1.4f).coerceIn(0f, 1f)
            spheres += Sphere3D("cytochrome_c", "Released Cytochrome c Molecules", cycX, -0.10f + 0.14f * u, 0.14f, 0.085f, Color(0xFFFF5D73), focusAlpha("cytochrome_c", 0.98f), "Cytochrome c")
            spheres += Sphere3D("apoptosome", "Apaf-1 Apoptosome + Caspase-9 / Executioner Caspase-3", 0.10f, 0.06f, 0.10f, 0.16f + 0.09f * u, Color(0xFFE04FB0), focusAlpha("apoptosome", 0.96f), "Caspases")
            bonds += Bond3D(-0.64f, 0.38f, 0.08f, 0.10f, 0.06f, 0.10f, Color(0xFF3DDC97).copy(alpha = 0.55f), 2f)
            bonds += Bond3D(-0.44f, -0.18f, 0f, 0.10f, 0.06f, 0.10f, Color(0xFFFF5D73), 3f)
            val nucRad = (0.34f - 0.16f * u).coerceAtLeast(0.14f)
            spheres += Sphere3D("condensed_nucleus", "Nucleus (CAD Endonuclease DNA Ladder & Lamin Cleavage)", -0.02f, 0.32f, -0.16f, nucRad, Color(0xFF8B6BFF), focusAlpha("condensed_nucleus", 0.90f), "Nucleus")
            val blebDist = 0.28f + 0.44f * u
            for (b in 0..5) {
                val a = b * 1.05f + u * 0.9f
                spheres += Sphere3D(
                    id = if (b == 0) "ps_bleb" else "bleb_$b",
                    label = "Phosphatidylserine (PS+) Apoptotic Body (Efferocytosis)",
                    x = blebDist * cos(a), y = blebDist * sin(a) * 0.78f, z = 0.20f * cos(a * 2f),
                    radius = 0.11f + 0.04f * u,
                    color = Color(0xFF22E5FF),
                    alpha = focusAlpha("ps_bleb", 0.78f),
                    groupTag = "PS+ Blebs"
                )
            }
            legend += LegendItem3D("Mito / BAX Pore", Color(0xFFFFC857), "mito_bax")
            legend += LegendItem3D("Cyto c / Apoptosome", Color(0xFFE04FB0), "apoptosome")
            legend += LegendItem3D("Nucleus / CAD", Color(0xFF8B6BFF), "condensed_nucleus")
            legend += LegendItem3D("PS+ Apoptotic Blebs", Color(0xFF22E5FF), "ps_bleb")
        }

        "necrosis" -> {
            val swell = (0.56f + 0.36f * u).coerceAtMost(0.95f)
            spheres += Sphere3D("swelling_cell", "Rupturing Plasma Membrane (Oncosis)", 0f, 0f, 0f, swell, Color(0xFFFF5D73), focusAlpha("swelling_cell", 0.25f + 0.08f * pulse), "Membrane")
            spheres += Sphere3D("nlrp3_speck", "NLRP3 Inflammasome + ASC Speck + Active Caspase-1", -0.12f, -0.06f, 0f, 0.20f, Color(0xFFFFC857), focusAlpha("nlrp3_speck", 0.98f), "NLRP3")
            spheres += Sphere3D("swollen_er_mito", "Swollen ER & Mito Cristae Loss (ROS)", 0.24f, 0.14f, -0.18f, 0.16f + 0.06f * u, Color(0xFF8B6BFF), focusAlpha("swollen_er_mito", 0.85f), "Organelles")
            for (p in 0..5) {
                val ang = p * 1.047f
                spheres += Sphere3D(
                    id = if (p == 0) "gsdmd_pore" else "pore_$p",
                    label = "Gasdermin-D (N-GSDMD) Lytic Oligomer Pore",
                    x = swell * cos(ang), y = swell * sin(ang) * 0.85f, z = (p % 2 - 0.5f) * 0.30f,
                    radius = 0.095f + 0.03f * u,
                    color = Color(0xFF22E5FF),
                    alpha = focusAlpha("gsdmd_pore", 0.96f),
                    groupTag = "GSDMD Pore"
                )
            }
            val escape = 0.22f + 0.68f * u
            for (d in 0..5) {
                val a = d * 1.05f + 0.4f
                spheres += Sphere3D(
                    id = if (d == 0) "damp_il1b" else "damp_$d",
                    label = if (d % 2 == 0) "Cleaved IL-1β / IL-18 Cytokine Release" else "Extracellular DAMPs (HMGB1, ATP)",
                    x = escape * cos(a), y = escape * sin(a), z = 0.28f * sin(a * 1.7f),
                    radius = 0.08f,
                    color = if (d % 2 == 0) Color(0xFF3DDC97) else Color(0xFFE04FB0),
                    alpha = focusAlpha("damp_il1b", 0.96f),
                    groupTag = "DAMPs / IL-1β"
                )
            }
            legend += LegendItem3D("NLRP3 + Caspase-1", Color(0xFFFFC857), "nlrp3_speck")
            legend += LegendItem3D("Swollen ER & Mito", Color(0xFF8B6BFF), "swollen_er_mito")
            legend += LegendItem3D("Gasdermin-D Pores", Color(0xFF22E5FF), "gsdmd_pore")
            legend += LegendItem3D("IL-1β / HMGB1 / ATP", Color(0xFFE04FB0), "damp_il1b")
        }

        "autophagy" -> {
            spheres += Sphere3D("damaged_mito", "Ubiquitinated Damaged Mitochondrion (p62/SQSTM1)", 0f, 0f, 0f, 0.22f, Color(0xFFFF5D73), focusAlpha("damaged_mito", 0.96f), "Cargo")
            spheres += Sphere3D("ulk1_beclin", "ULK1 / Beclin-1 / VPS34 Nucleation Complex", -0.45f, 0.32f, 0.14f, 0.13f, Color(0xFF8B6BFF), focusAlpha("ulk1_beclin", 0.95f), "Nucleation")
            val arcCount = (4 + (u * 7).toInt()).coerceIn(4, 11)
            for (i in 0 until arcCount) {
                val ang = -2.3f + i * 0.56f
                spheres += Sphere3D(
                    id = if (i == 0) "lc3_phagophore" else "lc3_$i",
                    label = "LC3-II Decorated Double-Membrane Phagophore",
                    x = 0.44f * cos(ang), y = 0.44f * sin(ang), z = 0.14f * sin(ang * 2f),
                    radius = 0.10f,
                    color = Color(0xFF2EE6C5),
                    alpha = focusAlpha("lc3_phagophore", 0.90f),
                    groupTag = "Autophagosome"
                )
            }
            val lx = 0.62f - 0.38f * u
            val ly = -0.46f + 0.36f * u
            spheres += Sphere3D("auto_lysosome", "LAMP2 Lysosome + Acid Hydrolase Fusion", lx, ly, 0f, 0.19f, Color(0xFFFFC857), focusAlpha("auto_lysosome", 0.96f), "Lysosome")
            spheres += Sphere3D("mhc2_loader", "MHC Class II Endogenous Peptide Loading", -0.38f, -0.34f, 0.22f, 0.11f, Color(0xFF22E5FF), focusAlpha("mhc2_loader", 0.92f), "MHC-II")
            bonds += Bond3D(lx, ly, 0f, 0f, 0f, 0f, Color(0xFFFFC857), 2.5f)
            legend += LegendItem3D("Damaged Mito + p62", Color(0xFFFF5D73), "damaged_mito")
            legend += LegendItem3D("ULK1 / LC3-II Cup", Color(0xFF2EE6C5), "lc3_phagophore")
            legend += LegendItem3D("LAMP2 Lysosome", Color(0xFFFFC857), "auto_lysosome")
            legend += LegendItem3D("MHC II Peptide Route", Color(0xFF22E5FF), "mhc2_loader")
        }

        "division" -> {
            val poleDist = 0.22f + 0.56f * u
            spheres += Sphere3D("centrosome_l", "Centrosome MTOC Pole + Cyclin-B/CDK1", -poleDist, 0f, 0f, 0.13f, Color(0xFFFFC857), focusAlpha("centrosome_l", 0.98f), "Centrosome")
            spheres += Sphere3D("centrosome_r", "Opposing Centrosome MTOC Pole", poleDist, 0f, 0f, 0.13f, Color(0xFFFFC857), 0.98f, "Centrosome")
            val chromSep = (u - 0.28f).coerceIn(0f, 0.72f) * 0.68f
            for (c in -1..1) {
                val cy = c * 0.20f
                spheres += Sphere3D(
                    id = if (c == 0) "sister_chromatids" else "chL_$c",
                    label = "Kinetochore-Attached Sister Chromatids (Cohesin/Separase)",
                    x = -0.05f - chromSep, y = cy, z = 0.08f * c,
                    radius = 0.095f,
                    color = Color(0xFF8B6BFF),
                    alpha = focusAlpha("sister_chromatids", 0.96f),
                    groupTag = "Chromatids"
                )
                spheres += Sphere3D("chR_$c", "Segregating Sister Chromatids", 0.05f + chromSep, cy, -0.08f * c, 0.095f, Color(0xFFE04FB0), 0.96f, "Chromatids")
                bonds += Bond3D(-poleDist, 0f, 0f, -0.05f - chromSep, cy, 0.08f * c, Color(0xFF22E5FF).copy(alpha = 0.6f), 2f)
                bonds += Bond3D(poleDist, 0f, 0f, 0.05f + chromSep, cy, -0.08f * c, Color(0xFF22E5FF).copy(alpha = 0.6f), 2f)
            }
            // Actomyosin Cytokinesis Contractile Ring pinching equator
            val ringRadius = (0.36f - 0.22f * ((u - 0.60f) / 0.40f).coerceIn(0f, 1f))
            spheres += Sphere3D("contractile_ring", "RhoA / Myosin-II Actin Contractile Ring", 0f, ringRadius, 0.14f, 0.09f, Color(0xFF22E5FF), focusAlpha("contractile_ring", 0.95f), "Actin Ring")
            val cellOffset = 0.15f + 0.35f * u
            spheres += Sphere3D("daughter_a", "Daughter Lymphocyte Clone A (2n)", -cellOffset, 0f, 0f, 0.56f, Color(0xFF3DDC97), 0.20f, "Clone A")
            spheres += Sphere3D("daughter_b", "Daughter Lymphocyte Clone B (2n)", cellOffset, 0f, 0f, 0.56f, Color(0xFF3DDC97), 0.20f, "Clone B")
            legend += LegendItem3D("MTOC / CDK1", Color(0xFFFFC857), "centrosome_l")
            legend += LegendItem3D("Sister Chromatids", Color(0xFF8B6BFF), "sister_chromatids")
            legend += LegendItem3D("Actomyosin Ring", Color(0xFF22E5FF), "contractile_ring")
            legend += LegendItem3D("Daughter Clones", Color(0xFF3DDC97), "daughter_a")
        }

        "synthesis" -> {
            spheres += Sphere3D("tx_nucleus", "Nucleus + RNA Polymerase II (Spliced mRNA)", -0.62f, 0.28f, 0f, 0.25f, Color(0xFF8B6BFF), focusAlpha("tx_nucleus", 0.88f), "Nucleus")
            spheres += Sphere3D("mrna_strand", "Capped mRNA + tRNA Adaptors", -0.34f, 0.08f, 0.14f, 0.10f, Color(0xFFFF5D73), focusAlpha("mrna_strand", 0.96f), "mRNA/tRNA")
            spheres += Sphere3D("rough_er", "Rough ER Sec61 Translocon + BiP/PDI Chaperones", -0.10f, -0.12f, 0.10f, 0.24f, Color(0xFF2EE6C5), focusAlpha("rough_er", 0.84f), "Rough ER")
            spheres += Sphere3D("ribosome_chain", "80S Polyribosome Translation Complexes", -0.12f, -0.02f, 0.28f, 0.11f, Color(0xFFFFC857), focusAlpha("ribosome_chain", 0.96f), "80S Ribosome")
            spheres += Sphere3D("golgi_mod", "Golgi Cis/Trans Glycosyltransferases", 0.44f, -0.16f, -0.06f, 0.21f, Color(0xFFE04FB0), focusAlpha("golgi_mod", 0.90f), "Golgi")
            bonds += Bond3D(-0.62f, 0.28f, 0f, -0.10f, -0.12f, 0.10f, Color(0xFF22E5FF), 2.5f)
            bonds += Bond3D(-0.10f, -0.12f, 0.10f, 0.44f, -0.16f, -0.06f, Color(0xFF3DDC97), 2.5f)
            val px = -0.60f + 1.24f * u
            val py = 0.26f - 0.18f * u + sin(u * PI.toFloat() * 2f) * 0.14f
            spheres += Sphere3D("nascent_igg", "Folding Antibody / Cytokine Polypeptide", px, py, 0.20f, 0.12f, Color(0xFF22E5FF), focusAlpha("nascent_igg", 0.98f), "Nascent IgG")
            legend += LegendItem3D("Nucleus / mRNA", Color(0xFF8B6BFF), "tx_nucleus")
            legend += LegendItem3D("ER / 80S Ribosome", Color(0xFFFFC857), "ribosome_chain")
            legend += LegendItem3D("Golgi N-Glycans", Color(0xFFE04FB0), "golgi_mod")
            legend += LegendItem3D("Nascent IgG Chain", Color(0xFF22E5FF), "nascent_igg")
        }

        "mito_fn" -> {
            spheres += Sphere3D("outer_mito", "Outer Mitochondrial Membrane + VDAC Porin", 0f, 0f, 0f, 0.70f, Color(0xFFFFC857), 0.20f, "Outer Membrane")
            val complexNames = listOf(
                "Complex I (NADH Dehydrogenase + H+)",
                "Complex II (Succinate Dehydrogenase)",
                "Complex III (Cytochrome bc1 + CoQ)",
                "Complex IV (Cytochrome c Oxidase → O2)"
            )
            for (i in 0..3) {
                val x = -0.46f + i * 0.23f
                val y = sin(i * 0.9f) * 0.12f
                spheres += Sphere3D(
                    id = if (i == 0) "etc_complex" else "etc_$i",
                    label = complexNames[i],
                    x = x, y = y, z = 0.08f,
                    radius = 0.105f,
                    color = Color(0xFF22E5FF),
                    alpha = focusAlpha("etc_complex", 0.95f),
                    groupTag = "ETC Complex"
                )
                if (i > 0) {
                    bonds += Bond3D(-0.46f + (i - 1) * 0.23f, sin((i - 1) * 0.9f) * 0.12f, 0.08f, x, y, 0.08f, Color(0xFFFFC857), 2.5f)
                }
            }
            // Flowing NADH/e- carrier and H+ Proton Gradient spheres
            val eX = -0.46f + 0.72f * ((u * 2f) % 1f)
            spheres += Sphere3D("electron_carrier", "NADH / Ubiquinone CoQ / Cytochrome c e- Shuttle", eX, 0.18f, 0.18f, 0.07f, Color(0xFFE04FB0), focusAlpha("electron_carrier", 0.96f), "e- Shuttle")
            spheres += Sphere3D("atp_synthase", "F0F1 ATP Synthase Chemiosmotic Rotor", 0.52f, 0.06f, 0.05f, 0.17f, Color(0xFF3DDC97), focusAlpha("atp_synthase", 0.98f), "ATP Synthase")
            spheres += Sphere3D("atp_pool", "Synthesized ATP Pool (~32 ATP / Glucose)", 0.52f, 0.12f + 0.44f * u, 0.22f, 0.11f + 0.03f * pulse, Color(0xFF2EE6C5), 0.96f, "ATP")
            spheres += Sphere3D("mt_ros", "mtROS / Cytochrome c Memory-Fate Signal", -0.10f, -0.34f, -0.10f, 0.12f + 0.04f * u, Color(0xFFFF5D73), focusAlpha("mt_ros", 0.92f), "mtROS")
            legend += LegendItem3D("ETC Complexes I–IV", Color(0xFF22E5FF), "etc_complex")
            legend += LegendItem3D("CoQ / Cyto c Shuttle", Color(0xFFE04FB0), "electron_carrier")
            legend += LegendItem3D("F0F1 ATP Synthase", Color(0xFF3DDC97), "atp_synthase")
            legend += LegendItem3D("mtROS / Fate", Color(0xFFFF5D73), "mt_ros")
        }

        else -> {
            // Cellular Differentiation
            spheres += Sphere3D("lineage_receptor", "Surface Cytokine Receptor (IL-12R / IL-4R / Notch)", -0.62f, 0.16f, 0f, 0.16f, Color(0xFF22E5FF), focusAlpha("lineage_receptor", 0.96f), "Receptor")
            spheres += Sphere3D("jak_kinase", "Receptor-Bound JAK1/2 & TYK2 Kinases", -0.44f, 0.12f, 0.10f, 0.10f, Color(0xFFE04FB0), focusAlpha("jak_kinase", 0.95f), "JAK Kinase")
            val statX = -0.34f + 0.46f * u
            spheres += Sphere3D("stat_tf", "Phospho-STAT Dimer + Master TF (T-bet / GATA3 / FOXP3)", statX, 0.05f, 0.14f, 0.15f, Color(0xFFFFC857), focusAlpha("stat_tf", 0.98f), "STAT/TF")
            spheres += Sphere3D("epigenetic_chromatin", "Nuclear Euchromatin + p300/HAT Histone Remodeling", 0.22f, 0f, 0f, 0.36f, Color(0xFF8B6BFF), focusAlpha("epigenetic_chromatin", 0.34f), "Chromatin")
            bonds += Bond3D(-0.62f, 0.16f, 0f, statX, 0.05f, 0.14f, Color(0xFF22E5FF).copy(alpha = 0.65f), 2.5f)
            bonds += Bond3D(statX, 0.05f, 0.14f, 0.22f, 0f, 0f, Color(0xFFFFC857).copy(alpha = 0.65f), 2.5f)
            spheres += Sphere3D("effector_phenotype", "Committed Lineage Output (Effector / Memory Fate)", 0.65f, -0.18f, 0.05f, 0.11f + 0.14f * u, Color(0xFF3DDC97), focusAlpha("effector_phenotype", 0.96f), "Lineage Fate")
            legend += LegendItem3D("Receptor + JAK", Color(0xFF22E5FF), "lineage_receptor")
            legend += LegendItem3D("STAT / Master TF", Color(0xFFFFC857), "stat_tf")
            legend += LegendItem3D("HAT / Chromatin", Color(0xFF8B6BFF), "epigenetic_chromatin")
            legend += LegendItem3D("Effector Fate", Color(0xFF3DDC97), "effector_phenotype")
        }
    }

    val activeStepNum = stepIdx + 1
    return Scene3DModel(
        title = "Live 3D Cellular Arena: ${process.name}",
        spheres = spheres,
        bonds = bonds,
        caption = "Stage $activeStepNum/$totalSteps: ${process.steps[stepIdx]} • Tap molecules/organelles",
        legendItems = legend
    )
}
