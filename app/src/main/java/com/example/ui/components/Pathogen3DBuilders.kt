package com.example.ui.components

import androidx.compose.ui.graphics.Color
import com.example.domain.HostSimStep
import com.example.domain.PathogenClass
import com.example.domain.PathogenDesign
import com.example.domain.TropismTarget
import kotlin.math.cos
import kotlin.math.sin

fun buildCustomPathogen3DScene(design: PathogenDesign): Scene3DModel {
    val spheres = mutableListOf<Sphere3D>()
    val bonds = mutableListOf<Bond3D>()

    val coreColor = when (design.pathogenClass) {
        PathogenClass.ENVELOPED_RNA_VIRUS -> Color(0xFF22E5FF)
        PathogenClass.SEGMENTED_RNA_VIRUS -> Color(0xFFFFC857)
        PathogenClass.RETROVIRUS -> Color(0xFFE04FB0)
        PathogenClass.INTRACELLULAR_BACTERIUM -> Color(0xFF3DDC97)
        PathogenClass.EXTRACELLULAR_BACTERIUM -> Color(0xFFFF5D73)
    }

    val spikeColor = when (design.tropism) {
        TropismTarget.UPPER_RESPIRATORY -> Color(0xFF22E5FF)
        TropismTarget.LOWER_ALVEOLAR -> Color(0xFFFF5D73)
        TropismTarget.CD4_LYMPHOID -> Color(0xFFE04FB0)
        TropismTarget.MACROPHAGE_NICHE -> Color(0xFFFFC857)
        TropismTarget.SYSTEMIC_ENDOTHELIUM -> Color(0xFF8B6BFF)
    }

    // Outer immune-evasion glycan/capsule shield scales with immuneEvasion
    val evasionRadius = (0.68f + (design.immuneEvasion * 0.16).toFloat()).coerceIn(0.65f, 0.96f)
    spheres += Sphere3D(
        id = "evasion_shield",
        label = "Evasion Capsule (${"%.2f".format(design.immuneEvasion)})",
        x = 0f,
        y = 0f,
        z = 0f,
        radius = evasionRadius,
        color = Color(0xFF8B6BFF),
        alpha = (0.14f + (design.immuneEvasion * 0.12).toFloat()).coerceAtMost(0.36f),
        groupTag = "Immune Evasion"
    )

    // Capsid / Cell wall core
    val isBacterium = design.pathogenClass == PathogenClass.INTRACELLULAR_BACTERIUM ||
        design.pathogenClass == PathogenClass.EXTRACELLULAR_BACTERIUM
    if (isBacterium) {
        for (k in -2..2) {
            spheres += Sphere3D(
                id = if (k == 0) "core_body" else "body_$k",
                label = if (k == 0) "${design.pathogenClass.label} Body" else "Cell Wall Segment",
                x = k * 0.18f,
                y = 0f,
                z = 0f,
                radius = 0.28f,
                color = coreColor,
                alpha = 0.88f,
                groupTag = "Pathogen Core"
            )
        }
    } else {
        spheres += Sphere3D(
            id = "core_body",
            label = "Genome & Capsid (${design.pathogenClass.label})",
            x = 0f,
            y = 0f,
            z = 0f,
            radius = 0.42f,
            color = coreColor,
            alpha = 0.82f,
            groupTag = "Genome & Capsid"
        )
    }

    // Surface Tropism Spikes / Adhesins density scales with receptorAffinity
    val spikeCount = (6 + (design.receptorAffinity * 6).toInt()).coerceIn(6, 18)
    for (i in 0 until spikeCount) {
        val phi = i * 2.39996f // golden angle
        val y = 1f - (i / (spikeCount - 1f).coerceAtLeast(1f)) * 2f
        val rProj = kotlin.math.sqrt((1f - y * y).coerceAtLeast(0f))
        val r = 0.62f + (design.receptorAffinity * 0.10).toFloat()
        val sx = r * cos(phi) * rProj
        val sy = r * y
        val sz = r * sin(phi) * rProj
        spheres += Sphere3D(
            id = if (i == 0) "tropism_spike" else "spike_$i",
            label = "Binding Motif: ${design.tropism.label}",
            x = sx,
            y = sy,
            z = sz,
            radius = (0.07f + (design.antigenicDrift * 0.03).toFloat()).coerceAtMost(0.13f),
            color = spikeColor,
            alpha = 0.96f,
            groupTag = "Tropism Spikes"
        )
        bonds += Bond3D(0f, 0f, 0f, sx, sy, sz, spikeColor.copy(alpha = 0.4f), 2.0f)
    }

    return Scene3DModel(
        title = "3D Pathogen Architect: ${design.name.take(28)}",
        spheres = spheres,
        bonds = bonds,
        caption = "${design.tropism.organSite} • Drift ${"%.2f".format(design.antigenicDrift)} • Drag to orbit",
        legendItems = listOf(
            LegendItem3D("Core/Genome", coreColor, "core_body"),
            LegendItem3D("Tropism Ligand", spikeColor, "tropism_spike"),
            LegendItem3D("Evasion Shield", Color(0xFF8B6BFF), "evasion_shield")
        )
    )
}

fun buildVirtualHostInfection3DScene(step: HostSimStep, design: PathogenDesign): Scene3DModel {
    val spheres = mutableListOf<Sphere3D>()
    val bonds = mutableListOf<Bond3D>()

    // Organ Target Compartment Stress Halo
    val stressFrac = (step.organStressPct / 100.0).toFloat().coerceIn(0f, 1f)
    val organCol = when {
        stressFrac > 0.55f -> Color(0xFFFF5D73)
        stressFrac > 0.25f -> Color(0xFFFFC857)
        else -> Color(0xFF2EE6C5)
    }
    spheres += Sphere3D(
        id = "host_organ",
        label = "${design.tropism.organSite} (${"%.0f".format(step.organStressPct)}% Stress)",
        x = 0f,
        y = 0f,
        z = 0f,
        radius = 0.78f,
        color = organCol,
        alpha = 0.22f + stressFrac * 0.22f,
        groupTag = "Target Organ"
    )

    // Pathogen Load Particles
    val virionCount = (step.log10ViralLoad * 1.6).toInt().coerceIn(0, 14)
    val tPhase = (step.day * 0.5).toFloat()
    for (k in 0 until virionCount) {
        val ang = tPhase + k * 0.85f
        val elev = ((k % 3) - 1) * 0.42f
        val r = 0.22f + (k % 2) * 0.14f
        spheres += Sphere3D(
            id = if (k == 0) "pathogen_load" else "p_$k",
            label = "Load ${"%.1f".format(step.log10ViralLoad)} log10",
            x = r * cos(ang),
            y = elev,
            z = r * sin(ang),
            radius = 0.085f,
            color = Color(0xFFFF5D73),
            alpha = 0.96f,
            groupTag = "Pathogen Load"
        )
    }

    // Host Immune Responders: IFN/Innate, Neutralizing IgG, CTLs
    spheres += Sphere3D(
        id = "host_ifn",
        label = "IFN-I / Innate (${"%.2f".format(step.innateIfn)})",
        x = -0.58f,
        y = 0.30f,
        z = 0.22f,
        radius = (0.12f + step.innateIfn.toFloat() * 0.05f).coerceIn(0.11f, 0.26f),
        color = Color(0xFFFFC857),
        alpha = 0.92f,
        groupTag = "Innate IFN"
    )
    spheres += Sphere3D(
        id = "host_ab",
        label = "Neutralizing Ab (${"%.2f".format(step.neutralizingAb)})",
        x = 0.58f,
        y = -0.25f,
        z = 0.30f,
        radius = (0.12f + step.neutralizingAb.toFloat() * 0.06f).coerceIn(0.11f, 0.28f),
        color = Color(0xFF3DDC97),
        alpha = 0.94f,
        groupTag = "Antibody"
    )
    spheres += Sphere3D(
        id = "host_ctl",
        label = "CD8+ CTL (${"%.2f".format(step.effectorCtl)})",
        x = 0.35f,
        y = 0.46f,
        z = -0.45f,
        radius = (0.12f + step.effectorCtl.toFloat() * 0.05f).coerceIn(0.11f, 0.26f),
        color = Color(0xFF22E5FF),
        alpha = 0.94f,
        groupTag = "Effector CTL"
    )
    bonds += Bond3D(0.58f, -0.25f, 0.30f, 0f, 0f, 0f, Color(0xFF3DDC97), 2.0f)
    bonds += Bond3D(0.35f, 0.46f, -0.45f, 0f, 0f, 0f, Color(0xFF22E5FF), 2.0f)

    return Scene3DModel(
        title = "Virtual Host Infection • Day ${"%.1f".format(step.day)}",
        spheres = spheres,
        bonds = bonds,
        caption = "Temp ${"%.1f".format(step.feverCelsius)}°C • R_eff ${"%.2f".format(step.sheddingReff)} • Stress ${"%.0f".format(step.organStressPct)}%",
        legendItems = listOf(
            LegendItem3D("Target Organ", organCol, "host_organ"),
            LegendItem3D("Pathogen Load", Color(0xFFFF5D73), "pathogen_load"),
            LegendItem3D("Innate IFN-I", Color(0xFFFFC857), "host_ifn"),
            LegendItem3D("Neutralizing Ab", Color(0xFF3DDC97), "host_ab"),
            LegendItem3D("CD8+ CTL", Color(0xFF22E5FF), "host_ctl")
        )
    )
}

fun buildTaxonomyPathogen3DScene(
    specimen: com.example.data.kb.PathogenTaxonomyEntry,
    variant: com.example.data.kb.PathogenVariantNode,
    selectedPartId: String? = null
): Scene3DModel {
    val spheres = mutableListOf<Sphere3D>()
    val bonds = mutableListOf<Bond3D>()
    val mutColor = Color(0xFFFF5D73)
    val mutId = variant.mutatedPartId
    val genScale = 1.0f + variant.generation * 0.06f

    fun col(partId: String, defaultColor: Color): Color =
        if (partId == mutId) mutColor else defaultColor

    when (specimen.kingdom) {
        com.example.data.kb.PathogenKingdom.VIRUS -> {
            // Lipid Envelope shell
            spheres += Sphere3D("lipid_env", "Lipid Envelope (M/E)", 0f, 0f, 0f, 0.62f, col("lipid_env", Color(0xFF8B6BFF)), 0.24f, "Lipid Envelope")
            // Nucleocapsid + ssRNA helical core
            for (k in -3..3) {
                val t = k * 0.11f
                spheres += Sphere3D(
                    id = if (k == 0) "nucleocapsid_rna" else "nc_$k",
                    label = "Nucleocapsid + ssRNA",
                    x = 0.20f * cos(k * 1.1f),
                    y = t,
                    z = 0.20f * sin(k * 1.1f),
                    radius = 0.095f,
                    color = col("nucleocapsid_rna", Color(0xFF3DDC97)),
                    alpha = 0.94f,
                    groupTag = "Nucleocapsid"
                )
            }
            spheres += Sphere3D("rdrp_core", "RdRp Replicase Complex", -0.14f, -0.08f, 0.14f, 0.13f, col("rdrp_core", Color(0xFFFFC857)), 0.96f, "RdRp")
            // Spike trimers scaling in density / drift coloration with generation
            val spikes = (10 + variant.generation * 2).coerceAtMost(18)
            for (i in 0 until spikes) {
                val phi = i * 2.39996f
                val y = 1f - (i / (spikes - 1f)) * 2f
                val rp = kotlin.math.sqrt((1f - y * y).coerceAtLeast(0f))
                val r = 0.68f * genScale.coerceAtMost(1.16f)
                val sx = r * cos(phi) * rp
                val sy = r * y
                val sz = r * sin(phi) * rp
                val sCol = col("spike_trimers", Color(0xFF22E5FF))
                spheres += Sphere3D(
                    id = if (i == 0) "spike_trimers" else "vsp_$i",
                    label = if (variant.generation > 0) "Mutated Spike RBD (${variant.generation}G)" else "Wild-Type Spike RBD",
                    x = sx, y = sy, z = sz,
                    radius = if (mutId == "spike_trimers") 0.11f else 0.085f,
                    color = sCol,
                    alpha = 0.96f,
                    groupTag = "Spike RBD"
                )
                bonds += Bond3D(0f, 0f, 0f, sx, sy, sz, sCol.copy(alpha = 0.42f), 2.0f)
            }
        }

        com.example.data.kb.PathogenKingdom.BACTERIUM -> {
            // Rod-shaped capsule & thickened wall
            val wallAlpha = (0.28f + variant.generation * 0.05f).coerceAtMost(0.48f)
            val wallCol = col("capsule_wall", Color(0xFF3DDC97))
            for (k in -3..3) {
                spheres += Sphere3D(
                    id = if (k == 0) "capsule_wall" else "bwall_$k",
                    label = if (variant.generation >= 3) "Thickened Peptidoglycan Wall" else "Capsule & Cell Wall",
                    x = k * 0.15f, y = 0f, z = 0f,
                    radius = 0.34f,
                    color = wallCol,
                    alpha = wallAlpha,
                    groupTag = "Cell Wall"
                )
            }
            // Nucleoid chromosomal loop
            spheres += Sphere3D("nucleoid_core", "Circular Nucleoid (DNA)", -0.15f, 0.04f, 0.02f, 0.18f, col("nucleoid_core", Color(0xFF22E5FF)), 0.92f, "Nucleoid")
            // Extrachromosomal Resistance Plasmid
            spheres += Sphere3D("plasmid_ring", "R-Plasmid (mecA / blaZ)", 0.25f, 0.12f, 0.16f, 0.11f, col("plasmid_ring", Color(0xFFFFC857)), 0.98f, "R-Plasmid")
            // Helical Flagellum + surface pili
            val flagCol = col("pili_flagellum", Color(0xFF8B6BFF))
            var px = -0.52f
            var py = 0f
            var pz = 0f
            for (f in 1..6) {
                val nx = -0.52f - f * 0.07f
                val ny = sin(f * 1.1f) * 0.18f
                val nz = cos(f * 1.1f) * 0.16f
                bonds += Bond3D(px, py, pz, nx, ny, nz, flagCol, 3.2f)
                px = nx; py = ny; pz = nz
            }
            spheres += Sphere3D("pili_flagellum", "Flagellum & Adhesin Pili", px, py, pz, 0.08f, flagCol, 0.96f, "Pili/Flagellum")
            for (p in 0..5) {
                val ang = p * 1.05f
                val bx = (p - 2.5f) * 0.16f
                val by = sin(ang) * 0.50f
                val bz = cos(ang) * 0.48f
                bonds += Bond3D(bx, 0f, 0f, bx, by, bz, flagCol.copy(alpha = 0.65f), 2.0f)
            }
        }

        com.example.data.kb.PathogenKingdom.FUNGUS -> {
            // Mother Yeast Cell Chitin/Glucan Wall & Ergosterol membrane
            spheres += Sphere3D("chitin_wall", "Chitin / β-1,3-Glucan Wall", -0.10f, -0.05f, 0f, 0.68f, col("chitin_wall", Color(0xFFFFC857)), 0.25f, "Chitin Wall")
            spheres += Sphere3D("ergosterol_mem", "Ergosterol Membrane & CDR1", -0.10f, -0.05f, 0f, 0.56f, col("ergosterol_mem", Color(0xFF8B6BFF)), 0.30f, "Ergosterol")
            spheres += Sphere3D("fungal_nucleus", "Eukaryotic Nucleus (ERG11)", -0.18f, -0.10f, 0.12f, 0.24f, col("fungal_nucleus", Color(0xFF22E5FF)), 0.94f, "Nucleus")
            // Budding Daughter Blastoconidium
            spheres += Sphere3D("daughter_bud", "Budding Daughter Blastospore", 0.56f, 0.36f, 0.08f, 0.28f, col("daughter_bud", Color(0xFF3DDC97)), 0.90f, "Budding Apex")
            bonds += Bond3D(-0.10f, -0.05f, 0f, 0.56f, 0.36f, 0.08f, Color.White.copy(alpha = 0.45f), 3.0f)
            // Surface efflux pump badges around membrane
            for (e in 0..5) {
                val a = e * 1.05f
                spheres += Sphere3D("pump_$e", "ABC Efflux / CYP51 Site", -0.10f + 0.56f * cos(a), -0.05f + 0.56f * sin(a), 0.15f, 0.065f, col("ergosterol_mem", Color(0xFFE04FB0)), 0.95f, "Ergosterol")
            }
        }

        com.example.data.kb.PathogenKingdom.PARASITE -> {
            // Polarized Apicomplexan Merozoite body (elongated tapered ovoid)
            for (k in -2..2) {
                val frac = (k + 2) / 4f
                spheres += Sphere3D(
                    id = if (k == 2) "apical_complex" else "pbody_$k",
                    label = if (k == 2) "Apical Complex (Rhoptries/AMA1)" else "Inner Membrane Complex",
                    x = k * 0.22f,
                    y = sin(frac * 1.2f) * 0.08f,
                    z = 0f,
                    radius = if (k == 2) 0.14f else (0.34f - k * 0.04f),
                    color = if (k == 2) col("apical_complex", Color(0xFF22E5FF)) else Color(0xFF8B6BFF),
                    alpha = if (k == 2) 0.96f else 0.32f,
                    groupTag = if (k == 2) "Apical Complex" else "Membrane"
                )
            }
            spheres += Sphere3D("parasite_nucleus", "Nucleus (var / PfEMP1)", -0.30f, -0.04f, 0.10f, 0.18f, col("parasite_nucleus", Color(0xFFE04FB0)), 0.94f, "Nucleus")
            spheres += Sphere3D("apicoplast", "Apicoplast Plastid (4-Membrane)", 0.10f, 0.16f, 0.14f, 0.13f, col("apicoplast", Color(0xFF3DDC97)), 0.96f, "Apicoplast")
            spheres += Sphere3D("food_vacuole", "Food Vacuole (Hemozoin/PfCRT)", -0.05f, -0.18f, -0.12f, 0.15f, col("food_vacuole", Color(0xFFFFC857)), 0.96f, "Food Vacuole")
        }

        com.example.data.kb.PathogenKingdom.AMOEBA -> {
            // Irregular asymmetric amoeboid lobes + pseudopodia extensions
            val pseudoColor = col("pseudopod_lobes", Color(0xFF2EE6C5))
            val lobes = listOf(
                Triple(0f, 0f, 0f) to 0.48f,
                Triple(0.56f, 0.22f, 0.14f) to 0.24f,
                Triple(-0.52f, 0.34f, -0.16f) to 0.22f,
                Triple(0.32f, -0.52f, 0.18f) to 0.21f,
                Triple(-0.44f, -0.40f, 0.12f) to 0.20f
            )
            lobes.forEachIndexed { idx, (pos, r) ->
                spheres += Sphere3D(
                    id = if (idx == 1) "pseudopod_lobes" else "alobe_$idx",
                    label = if (idx == 0) "Ectoplasm Cortex" else "Actin Pseudopodium + Lectin",
                    x = pos.first, y = pos.second, z = pos.third,
                    radius = r,
                    color = pseudoColor,
                    alpha = 0.28f,
                    groupTag = "Pseudopodia"
                )
            }
            spheres += Sphere3D("amoeba_nucleus", "Nucleus + Central Karyosome", -0.05f, 0.08f, 0.04f, 0.20f, col("amoeba_nucleus", Color(0xFF8B6BFF)), 0.96f, "Nucleus")
            spheres += Sphere3D("contractile_vac", "Contractile Osmotic Vacuole", -0.28f, -0.18f, 0.16f, 0.13f, col("contractile_vac", Color(0xFF22E5FF)), 0.94f, "Contractile Vac")
            spheres += Sphere3D("phago_vacuole", "Phagocytic Vacuole + Amebapore", 0.24f, -0.16f, -0.14f, 0.15f, col("phago_vacuole", Color(0xFFFFC857)), 0.94f, "Phago Vacuole")
        }

        com.example.data.kb.PathogenKingdom.HELMINTH -> {
            // Elongated sinusoidal trematode worm body + tegument shield + suckers
            val tegCol = col("tegument_shield", Color(0xFF3DDC97))
            for (k in -4..4) {
                val x = k * 0.17f
                val y = sin(k * 0.55f) * 0.22f
                val z = cos(k * 0.45f) * 0.10f
                spheres += Sphere3D(
                    id = if (k == 0) "tegument_shield" else "hseg_$k",
                    label = "Syncytial Tegument + Host Mask",
                    x = x, y = y, z = z,
                    radius = (0.24f - kotlin.math.abs(k) * 0.025f).coerceAtLeast(0.10f),
                    color = tegCol,
                    alpha = 0.34f,
                    groupTag = "Tegument"
                )
            }
            spheres += Sphere3D("oral_ventral_sucker", "Oral & Ventral Suckers", -0.66f, 0.16f, 0.10f, 0.15f, col("oral_ventral_sucker", Color(0xFFE04FB0)), 0.96f, "Suckers")
            spheres += Sphere3D("gut_cecum", "Bifurcated Gut Cecum", 0.15f, -0.05f, 0.05f, 0.14f, col("gut_cecum", Color(0xFF8B6BFF)), 0.92f, "Gut Cecum")
            spheres += Sphere3D("sensory_spines", "Surface Tubercles & TRPM Spines", 0.42f, 0.18f, 0.14f, 0.12f, col("sensory_spines", Color(0xFFFFC857)), 0.96f, "PZQ Spines")
        }
    }

    val legend = buildList {
        specimen.parts.forEach { pt ->
            val colVal = if (pt.id == mutId) mutColor else if (pt.id == selectedPartId) Color.White else Color(0xFF22E5FF)
            val prefix = if (pt.id == mutId) "★ MUTATED: " else ""
            add(LegendItem3D("$prefix${pt.label}", colVal, pt.id))
        }
    }

    return Scene3DModel(
        title = "3D Pathogen Explorer: ${specimen.organismName}",
        spheres = spheres,
        bonds = bonds,
        caption = "${variant.strainLabel} • Red ★ = Active Mutation Site • Drag to rotate",
        legendItems = legend
    )
}

