package com.example.ui.components

import androidx.compose.ui.graphics.Color
import com.example.domain.MolecularStructureProfile

fun buildLabeledMolecule3DScene(
    mol: MolecularStructureProfile,
    highlightFunctionalGroup: String? = null,
    selectedAtomId: String? = null
): Scene3DModel {
    fun elementCpkColor(symbol: String): Color = when (symbol) {
        "C" -> Color(0xFF94A3B8)
        "N" -> Color(0xFF3B82F6)
        "O" -> Color(0xFFFF4D5A)
        "S" -> Color(0xFFFFC857)
        "Cl" -> Color(0xFF22C55E)
        "F" -> Color(0xFF22E5FF)
        else -> Color(0xFFE2E8F0)
    }

    fun elementRadius(symbol: String): Float = when (symbol) {
        "S", "Cl" -> 0.115f
        "O", "N" -> 0.100f
        "H" -> 0.068f
        else -> 0.095f
    }

    val spheres = mol.atoms.map { atom ->
        val isGroupMatch = highlightFunctionalGroup != null &&
            atom.functionalGroup.equals(highlightFunctionalGroup, ignoreCase = true)
        val isFocused = atom.id == selectedAtomId || isGroupMatch
        val baseColor = elementCpkColor(atom.symbol)
        val finalAlpha = when {
            highlightFunctionalGroup == null -> 0.96f
            isGroupMatch || atom.id == selectedAtomId -> 1.0f
            else -> 0.34f
        }
        Sphere3D(
            id = atom.id,
            label = atom.label,
            x = atom.x,
            y = atom.y,
            z = atom.z,
            radius = if (isFocused) elementRadius(atom.symbol) * 1.24f else elementRadius(atom.symbol),
            color = if (isGroupMatch) Color(0xFF22E5FF) else baseColor,
            alpha = finalAlpha,
            groupTag = atom.label
        )
    }

    val bonds = mutableListOf<Bond3D>()
    mol.bonds.forEach { b ->
        if (b.fromIdx in mol.atoms.indices && b.toIdx in mol.atoms.indices) {
            val a1 = mol.atoms[b.fromIdx]
            val a2 = mol.atoms[b.toIdx]
            val bondColor = when {
                b.isAromatic -> Color(0xFFA78BFA)
                b.order >= 2 -> Color(0xFF22E5FF)
                else -> Color(0xFFCBD5E1)
            }
            val thickness = when {
                b.order >= 3 -> 5.2f
                b.order == 2 -> 4.4f
                b.isAromatic -> 3.6f
                else -> 2.8f
            }
            bonds += Bond3D(a1.x, a1.y, a1.z, a2.x, a2.y, a2.z, bondColor, thickness)
            // Secondary parallel strand hint for double bonds
            if (b.order == 2) {
                bonds += Bond3D(
                    a1.x + 0.025f, a1.y + 0.025f, a1.z + 0.015f,
                    a2.x + 0.025f, a2.y + 0.025f, a2.z + 0.015f,
                    bondColor.copy(alpha = 0.6f), 2.0f
                )
            }
        }
    }

    val firstCarbon = mol.atoms.firstOrNull { it.symbol == "C" }?.id
    val firstOxygen = mol.atoms.firstOrNull { it.symbol == "O" }?.id
    val firstNitrogen = mol.atoms.firstOrNull { it.symbol == "N" }?.id
    val firstSulfur = mol.atoms.firstOrNull { it.symbol == "S" }?.id
    val firstHalogen = mol.atoms.firstOrNull { it.symbol == "Cl" || it.symbol == "F" }?.id

    val legend = buildList {
        add(LegendItem3D("Carbon (C)", elementCpkColor("C"), firstCarbon))
        if (firstNitrogen != null) add(LegendItem3D("Nitrogen (N)", elementCpkColor("N"), firstNitrogen))
        if (firstOxygen != null) add(LegendItem3D("Oxygen (O)", elementCpkColor("O"), firstOxygen))
        if (firstSulfur != null) add(LegendItem3D("Sulfur (S)", elementCpkColor("S"), firstSulfur))
        if (firstHalogen != null) add(LegendItem3D("Halogen (Cl/F)", elementCpkColor("Cl"), firstHalogen))
        add(LegendItem3D("Double / π Bond", Color(0xFF22E5FF), null))
        add(LegendItem3D("Aromatic Ring", Color(0xFFA78BFA), null))
    }

    return Scene3DModel(
        title = "3D Structure: ${mol.name} (${mol.formula})",
        spheres = spheres,
        bonds = bonds,
        caption = "Labels ON • MW ${"%.2f".format(mol.monoisotopicMass)} • IHD ${mol.ihd} • Tap any labeled atom",
        legendItems = legend
    )
}
