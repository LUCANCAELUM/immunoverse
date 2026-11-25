package com.example.ui.components

import androidx.compose.ui.graphics.Color
import com.example.data.kb.CellInfo
import com.example.data.kb.ImmunologyKb
import com.example.data.network.PdbStructureData
import com.example.domain.SimParams
import com.example.domain.SimStep
import kotlin.math.cos
import kotlin.math.sin

fun buildOrgans3DScene(selectedOrganId: String?, isolateGroup: String?): Scene3DModel {
    val organs = if (isolateGroup.isNullOrBlank() || isolateGroup == "All") {
        ImmunologyKb.ORGANS
    } else {
        ImmunologyKb.ORGANS.filter { it.kind.contains(isolateGroup, ignoreCase = true) }
    }
    val spheres = organs.map { o ->
        Sphere3D(
            id = o.id,
            label = o.name,
            x = o.x,
            y = o.y,
            z = o.z,
            radius = if (o.id == selectedOrganId) o.radius * 1.25f else o.radius,
            color = Color(o.colorHex),
            alpha = if (selectedOrganId == null || selectedOrganId == o.id) 0.94f else 0.45f,
            groupTag = o.name
        )
    }
    val bonds = listOf(
        Bond3D(-0.2f, -0.35f, 0.1f, 0.0f, 0.48f, 0.22f, Color(0xFF22E5FF), 2.5f),
        Bond3D(0.0f, 0.48f, 0.22f, -0.18f, 0.72f, 0.08f, Color(0xFF3DDC97), 2.0f),
        Bond3D(-0.18f, 0.72f, 0.08f, -0.58f, 0.28f, 0.05f, Color(0xFF3DDC97), 2.0f),
        Bond3D(0.52f, 0.12f, -0.1f, 0.05f, -0.18f, 0.32f, Color(0xFF8B6BFF), 2.0f),
        Bond3D(-0.36f, -0.05f, 0.28f, 0.05f, -0.18f, 0.32f, Color(0xFFE04FB0), 2.0f)
    )
    return Scene3DModel(
        title = "3D Sim 1: Lymphoid Organ Atlas",
        spheres = spheres,
        bonds = bonds,
        caption = "Tap any organ node • Primary vs Secondary vs Mucosal",
        legendItems = listOf(
            LegendItem3D("Bone Marrow", Color(0xFF2EE6C5), "marrow"),
            LegendItem3D("Thymus", Color(0xFF22E5FF), "thymus"),
            LegendItem3D("Spleen", Color(0xFF8B6BFF), "spleen"),
            LegendItem3D("Lymph Nodes", Color(0xFF3DDC97), "ln_cervical"),
            LegendItem3D("Mucosal / Peyer's", Color(0xFFE04FB0), "peyers")
        )
    )
}

fun buildCell3DScene(cell: CellInfo, showReceptors: Boolean): Scene3DModel {
    val col = Color(cell.colorHex)
    val spheres = mutableListOf(
        Sphere3D("membrane", "Plasma Membrane", 0f, 0f, 0f, 0.95f, col, alpha = 0.24f, groupTag = "Plasma Membrane"),
        Sphere3D("nucleus", "Nucleus", 0.05f, 0.05f, 0.0f, 0.36f, Color(0xFF8B6BFF), alpha = 0.88f, groupTag = "Nucleus"),
        Sphere3D("mito1", "Mitochondria", -0.35f, 0.22f, 0.15f, 0.14f, Color(0xFFFFC857), alpha = 0.9f, groupTag = "Mitochondria"),
        Sphere3D("golgi", "Golgi Apparatus", 0.32f, -0.22f, -0.12f, 0.16f, Color(0xFFE04FB0), alpha = 0.9f, groupTag = "Golgi Apparatus")
    )
    val bonds = mutableListOf<Bond3D>()
    if (showReceptors) {
        cell.receptors.take(6).forEachIndexed { idx, rec ->
            val phi = idx * 1.05f
            val theta = (idx % 3 - 1) * 0.55f
            val r = 0.88f
            val rx = r * cos(phi) * cos(theta)
            val ry = r * sin(theta)
            val rz = r * sin(phi) * cos(theta)
            spheres += Sphere3D(
                id = "rec_$idx",
                label = rec,
                x = rx,
                y = ry,
                z = rz,
                radius = 0.10f,
                color = Color(0xFF22E5FF),
                alpha = 0.96f,
                groupTag = rec
            )
            bonds += Bond3D(0f, 0f, 0f, rx, ry, rz, Color(0xFF22E5FF).copy(alpha = 0.25f), 1.5f)
        }
    }
    return Scene3DModel(
        title = "3D Sim 2: ${cell.name}",
        spheres = spheres,
        bonds = bonds,
        caption = "${cell.name} • Drag to orbit • Slide Slice Z for interior",
        legendItems = buildList {
            add(LegendItem3D("Membrane", col, "membrane"))
            add(LegendItem3D("Nucleus", Color(0xFF8B6BFF), "nucleus"))
            add(LegendItem3D("Mitochondria", Color(0xFFFFC857), "mito1"))
            add(LegendItem3D("Golgi", Color(0xFFE04FB0), "golgi"))
            if (showReceptors && cell.receptors.isNotEmpty()) {
                add(LegendItem3D("Surface Receptors", Color(0xFF22E5FF), "rec_0"))
            }
        }
    )
}

fun buildOrganelle3DScene(selectedId: String): Scene3DModel {
    val spheres = ImmunologyKb.ORGANELLES.map { org ->
        Sphere3D(
            id = org.id,
            label = org.name,
            x = org.x,
            y = org.y,
            z = org.z,
            radius = org.radius,
            color = Color(org.colorHex),
            alpha = when {
                org.id == "membrane" -> 0.20f
                org.id == selectedId -> 0.98f
                else -> 0.68f
            },
            groupTag = org.name
        )
    }
    return Scene3DModel(
        title = "3D Sim 3: Immune Organelle Map",
        spheres = spheres,
        caption = "Tap organelle sphere or legend to inspect antigen routing",
        legendItems = ImmunologyKb.ORGANELLES
            .filter { it.id != "cytoplasm" }
            .take(6)
            .map { LegendItem3D(it.name, Color(it.colorHex), it.id) }
    )
}

enum class PdbColorMode(val label: String) {
    BY_CHAIN("Chain"),
    SECONDARY_STRUCTURE("Sec Struct (HELIX/SHEET)"),
    BFACTOR_PLDDT("B-Factor / pLDDT"),
    N_TO_C_RAINBOW("N→C Gradient")
}

fun buildPdb3DScene(
    data: PdbStructureData,
    colorByChain: Boolean = true,
    colorMode: PdbColorMode = if (colorByChain) PdbColorMode.BY_CHAIN else PdbColorMode.N_TO_C_RAINBOW
): Scene3DModel {
    val chainColors = listOf(Color(0xFF22E5FF), Color(0xFF8B6BFF), Color(0xFF3DDC97), Color(0xFFFFC857), Color(0xFFE04FB0))
    val maxB = (data.caAtoms.maxOfOrNull { it.bFactor } ?: 60f).coerceAtLeast(10f)
    val minB = data.caAtoms.minOfOrNull { it.bFactor } ?: 0f

    fun atomColor(a: com.example.data.network.PdbAtom, idx: Int, total: Int): Color = when (colorMode) {
        PdbColorMode.BY_CHAIN ->
            chainColors[data.chains.indexOf(a.chain).coerceAtLeast(0) % chainColors.size]
        PdbColorMode.SECONDARY_STRUCTURE -> when (a.secStruct) {
            'H' -> Color(0xFF22E5FF) // α-Helix Cyan
            'E' -> Color(0xFFE04FB0) // β-Sheet Magenta
            else -> Color(0xFF3DDC97) // Turn/Loop Emerald
        }
        PdbColorMode.BFACTOR_PLDDT -> {
            if (data.isAlphaFoldModel) {
                when {
                    a.bFactor >= 90f -> Color(0xFF2563EB) // Very high pLDDT
                    a.bFactor >= 70f -> Color(0xFF22E5FF) // Confident
                    a.bFactor >= 50f -> Color(0xFFFFC857) // Low
                    else -> Color(0xFFFF5D73) // Very low / disordered
                }
            } else {
                val frac = ((a.bFactor - minB) / (maxB - minB).coerceAtLeast(1f)).coerceIn(0f, 1f)
                Color(red = 0.15f + 0.85f * frac, green = 0.85f - 0.55f * frac, blue = 1f - 0.8f * frac)
            }
        }
        PdbColorMode.N_TO_C_RAINBOW -> {
            val frac = idx.toFloat() / total.coerceAtLeast(1)
            Color(red = frac, green = 0.85f - frac * 0.35f, blue = 1f - frac * 0.5f)
        }
    }

    val atomLookup = mutableMapOf<String, com.example.data.network.PdbAtom>()
    val byChain = data.caAtoms.groupBy { it.chain }.entries.map { (chainName, list) ->
        val total = list.size.coerceAtLeast(1)
        list.mapIndexed { idx, a ->
            val key = "${a.chain}:${a.resSeq}"
            atomLookup[key] = a
            val col = atomColor(a, idx, total)
            val ssText = when (a.secStruct) {
                'H' -> "Helix"
                'E' -> "Sheet"
                else -> "Loop"
            }
            val metricText = if (data.isAlphaFoldModel) "pLDDT ${"%.0f".format(a.bFactor)}" else "B=${"%.1f".format(a.bFactor)}"
            Sphere3D(
                id = "${a.chain}:${a.resName}${a.resSeq}",
                label = "${a.resName}${a.resSeq} ($chainName • $ssText • $metricText)",
                x = a.x,
                y = a.y,
                z = a.z,
                radius = 0.052f,
                color = col,
                alpha = 0.92f,
                groupTag = "Chain $chainName"
            )
        }
    }

    // Disulfide S-S bridge lines between experimentally annotated SSBOND C-alphas
    val ssbondLines = data.disulfides.mapNotNull { ss ->
        val a1 = atomLookup["${ss.chain1}:${ss.resSeq1}"]
        val a2 = atomLookup["${ss.chain2}:${ss.resSeq2}"]
        if (a1 != null && a2 != null) {
            Bond3D(a1.x, a1.y, a1.z, a2.x, a2.y, a2.z, Color(0xFFFFC857), 2.8f)
        } else null
    }

    val hetSpheres = data.heteroAtoms.mapIndexed { idx, a ->
        Sphere3D(
            id = "het_${a.resName}_${a.resSeq}_$idx",
            label = "Ligand ${a.resName} ${a.resSeq} (${a.atomName})",
            x = a.x,
            y = a.y,
            z = a.z,
            radius = 0.078f,
            color = Color(0xFFFF5D73),
            alpha = 0.96f,
            groupTag = "Ligand ${a.resName}"
        )
    }

    val legend = buildList {
        when (colorMode) {
            PdbColorMode.BY_CHAIN -> data.chains.take(5).forEachIndexed { idx, ch ->
                add(LegendItem3D("Chain $ch", chainColors[idx % chainColors.size], null))
            }
            PdbColorMode.SECONDARY_STRUCTURE -> {
                add(LegendItem3D("α-Helix (HELIX)", Color(0xFF22E5FF), null))
                add(LegendItem3D("β-Sheet (SHEET)", Color(0xFFE04FB0), null))
                add(LegendItem3D("Loop / Turn", Color(0xFF3DDC97), null))
            }
            PdbColorMode.BFACTOR_PLDDT -> {
                if (data.isAlphaFoldModel) {
                    add(LegendItem3D("pLDDT ≥90 (High)", Color(0xFF2563EB), null))
                    add(LegendItem3D("70–90 (Confident)", Color(0xFF22E5FF), null))
                    add(LegendItem3D("50–70 (Low)", Color(0xFFFFC857), null))
                    add(LegendItem3D("<50 (Disordered)", Color(0xFFFF5D73), null))
                } else {
                    add(LegendItem3D("Rigid (Low B)", Color(0xFF26D9FF), null))
                    add(LegendItem3D("Flexible (High B)", Color(0xFFFF4D33), null))
                }
            }
            PdbColorMode.N_TO_C_RAINBOW -> {
                add(LegendItem3D("N-Terminus", Color(0xFF00D9FF), null))
                add(LegendItem3D("C-Terminus", Color(0xFFFF8880), null))
            }
        }
        if (ssbondLines.isNotEmpty()) {
            add(LegendItem3D("Disulfide (${ssbondLines.size})", Color(0xFFFFC857), null))
        }
        if (hetSpheres.isNotEmpty()) {
            add(LegendItem3D("Bound Ligand", Color(0xFFFF5D73), hetSpheres.first().id))
        }
    }

    val sourceTag = if (data.isAlphaFoldModel) "AlphaFold DB ${data.pdbId}" else "PDB ${data.pdbId} (${data.resolution})"
    return Scene3DModel(
        title = "Verified 3D: $sourceTag",
        spheres = hetSpheres,
        bonds = ssbondLines,
        backboneChain = byChain,
        caption = "${data.method} • ${data.chains.size} chain(s) • ${data.caAtoms.size} C-α • ${ssbondLines.size} S-S",
        legendItems = legend
    )
}

fun buildHivVirion3DScene(): Scene3DModel {
    val spheres = mutableListOf<Sphere3D>()
    spheres += Sphere3D("env", "Lipid Envelope", 0f, 0f, 0f, 0.88f, Color(0xFF8B6BFF), alpha = 0.22f, groupTag = "Lipid Envelope")
    spheres += Sphere3D("p17", "p17 Matrix Shell", 0f, 0f, 0f, 0.72f, Color(0xFF22E5FF), alpha = 0.20f, groupTag = "p17 Matrix")
    for (k in -2..2) {
        val t = (k + 2) / 4f
        spheres += Sphere3D(
            id = if (k == 0) "p24_0" else "p24_$k",
            label = "p24 Capsid Cone + ssRNA",
            x = k * 0.08f,
            y = k * 0.12f,
            z = 0f,
            radius = 0.16f + t * 0.12f,
            color = Color(0xFFFFC857),
            alpha = 0.90f,
            groupTag = "p24 Capsid Core"
        )
    }
    for (i in 0 until 12) {
        val phi = i * 1.15f
        val theta = ((i % 5) - 2) * 0.45f
        val r = 0.86f
        spheres += Sphere3D(
            id = "gp120_$i",
            label = "Env gp120/gp41 Spike",
            x = r * cos(phi) * cos(theta),
            y = r * sin(theta),
            z = r * sin(phi) * cos(theta),
            radius = 0.11f,
            color = Color(0xFFE04FB0),
            alpha = 0.95f,
            groupTag = "Env gp120/gp41"
        )
    }
    return Scene3DModel(
        title = "3D Sim 4: HIV-1 Virion Cutaway",
        spheres = spheres,
        caption = "Slide Slice Z < 0.2 to cut open Lipid Envelope & inspect p24 core",
        legendItems = listOf(
            LegendItem3D("Env gp120/gp41", Color(0xFFE04FB0), "gp120_0"),
            LegendItem3D("Lipid Envelope", Color(0xFF8B6BFF), "env"),
            LegendItem3D("p17 Matrix", Color(0xFF22E5FF), "p17"),
            LegendItem3D("p24 Capsid + RNA", Color(0xFFFFC857), "p24_0")
        )
    )
}

fun buildImmuneResponse3DScene(step: SimStep, params: SimParams): Scene3DModel {
    val spheres = mutableListOf<Sphere3D>()
    val bonds = mutableListOf<Bond3D>()
    val phaseRad = (step.day * 0.45).toFloat()

    // Outer Tolerance / Treg shield ring radius modulates by autoTolerance
    val tolColor = if (params.autoTolerance < 0.5) Color(0xFFFF5D73) else Color(0xFF2EE6C5)
    spheres += Sphere3D(
        id = "treg_shield",
        label = "Treg Tolerance (${"%.2f".format(step.tregSuppression)})",
        x = 0f,
        y = 0f,
        z = 0f,
        radius = (0.72f + (step.tregSuppression * 0.14).toFloat()).coerceIn(0.65f, 1.02f),
        color = tolColor,
        alpha = if (params.autoTolerance < 0.5) 0.16f else 0.14f,
        groupTag = "Treg Tolerance"
    )

    // Pathogen / Antigen Swarm (red/magenta spheres orbiting origin)
    val agNodes = (step.antigen * 4.0).toInt().coerceIn(if (step.antigen > 0.03) 1 else 0, 12)
    for (k in 0 until agNodes) {
        val ang = phaseRad + k * 0.95f
        val elev = ((k % 3) - 1) * 0.35f
        val r = 0.22f + (k % 2) * 0.14f
        val px = r * cos(ang) * cos(elev)
        val py = r * sin(elev)
        val pz = r * sin(ang) * cos(elev)
        spheres += Sphere3D(
            id = if (k == 0) "pathogen_cluster" else "ag_$k",
            label = "Pathogen (${"%.2f".format(step.antigen)})",
            x = px,
            y = py,
            z = pz,
            radius = (0.08f + (step.antigen * 0.015).toFloat()).coerceAtMost(0.16f),
            color = Color(0xFFFF5D73),
            alpha = 0.95f,
            groupTag = "Pathogen"
        )
    }

    // Innate Sentinel / APC Node (gold)
    val apcScale = (0.14f + (step.innate * 0.035).toFloat()).coerceIn(0.12f, 0.30f)
    spheres += Sphere3D(
        id = "innate_sentinel",
        label = "Innate/APC (${"%.2f".format(step.innate)})",
        x = -0.52f * cos(phaseRad * 0.4f),
        y = 0.28f,
        z = -0.35f * sin(phaseRad * 0.4f),
        radius = apcScale,
        color = Color(0xFFFFC857),
        alpha = 0.92f,
        groupTag = "Innate/APC"
    )

    // Effector CD4/CD8 T Cells (cyan)
    val tNodes = (step.tCell * 3.0).toInt().coerceIn(1, 8)
    for (k in 0 until tNodes) {
        val ang = -phaseRad * 0.7f + k * 0.88f
        val tx = 0.56f * cos(ang)
        val ty = -0.22f + (k % 3) * 0.18f
        val tz = 0.56f * sin(ang)
        spheres += Sphere3D(
            id = if (k == 0) "t_effector" else "t_$k",
            label = "T Effectors (${"%.2f".format(step.tCell)})",
            x = tx,
            y = ty,
            z = tz,
            radius = (0.09f + (step.tCell * 0.014).toFloat()).coerceAtMost(0.18f),
            color = Color(0xFF22E5FF),
            alpha = 0.92f,
            groupTag = "T Effectors"
        )
        if (k == 0) {
            bonds += Bond3D(-0.52f * cos(phaseRad * 0.4f), 0.28f, -0.35f * sin(phaseRad * 0.4f), tx, ty, tz, Color(0xFF22E5FF), 2.2f)
        }
    }

    // B Cells / Germinal Center & Antibodies (emerald)
    val abScale = (0.12f + (step.antibody * 0.028).toFloat()).coerceIn(0.10f, 0.28f)
    val bx = 0.58f * cos(phaseRad * 0.35f + 1.4f)
    val by = -0.38f
    val bz = 0.52f * sin(phaseRad * 0.35f + 1.4f)
    spheres += Sphere3D(
        id = "b_gc_plasma",
        label = "B/GC & IgG (${"%.2f".format(step.antibody)})",
        x = bx,
        y = by,
        z = bz,
        radius = abScale,
        color = Color(0xFF3DDC97),
        alpha = 0.94f,
        groupTag = "B/GC & IgG"
    )
    if (agNodes > 0) {
        bonds += Bond3D(bx, by, bz, 0f, 0f, 0f, Color(0xFF3DDC97), 2.4f)
    }

    // Memory Pool Niche (violet)
    if (step.memory > 0.04) {
        spheres += Sphere3D(
            id = "memory_pool",
            label = "Memory (${"%.2f".format(step.memory)})",
            x = 0.15f,
            y = 0.66f,
            z = -0.45f,
            radius = (0.11f + (step.memory * 0.04).toFloat()).coerceIn(0.11f, 0.24f),
            color = Color(0xFF8B6BFF),
            alpha = 0.95f,
            groupTag = "Memory Pool"
        )
    }

    return Scene3DModel(
        title = "3D Sim 6: ${step.phaseTag}",
        spheres = spheres,
        bonds = bonds,
        caption = "Day ${"%.1f".format(step.day)} • Affinity ${"%.2f".format(step.affinityMaturation)}x • Tap nodes",
        legendItems = listOf(
            LegendItem3D("Pathogen", Color(0xFFFF5D73), "pathogen_cluster"),
            LegendItem3D("Innate/APC", Color(0xFFFFC857), "innate_sentinel"),
            LegendItem3D("T Effectors", Color(0xFF22E5FF), "t_effector"),
            LegendItem3D("B/GC & IgG", Color(0xFF3DDC97), "b_gc_plasma"),
            LegendItem3D("Memory", Color(0xFF8B6BFF), "memory_pool"),
            LegendItem3D("Treg Halo", tolColor, "treg_shield")
        )
    )
}
