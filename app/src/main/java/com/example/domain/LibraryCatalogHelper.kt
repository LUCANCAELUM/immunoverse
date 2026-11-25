package com.example.domain

import com.example.data.model.BookmarkItem
import com.example.ui.components.AppRoute

enum class LibraryCategory(val label: String) {
    ALL("All"),
    ARTICLES("Articles"),
    PATHOGENS("Pathogens & Spikes"),
    TOPICS("Topics & Concepts"),
    LABS("Labs & Molecules")
}

object LibraryCatalogHelper {
    val SUGGESTED_RESEARCH_TOPICS: List<Pair<String, String>> = listOf(
        "SARS-CoV-2 Spike RBD Escape" to "Conformational RBD drift vs neutralizing mAbs",
        "CAR-T Exhaustion & PD-1" to "Sustaining effector memory CAR-T in solid tumors",
        "NLRP3 Inflammasome Pyroptosis" to "ASC speck assembly & Gasdermin-D IL-1β release",
        "Cross-Presentation in cDC1" to "XCR1+ dendritic cell MHC Class I priming",
        "HIV-1 Envelope & bNAbs" to "CD4bs broadly neutralizing cocktails & latency",
        "mRNA LNP Germinal Centers" to "Tfh priming & somatic hypermutation breadth"
    )

    fun classify(item: BookmarkItem): LibraryCategory = when (item.itemType.lowercase()) {
        "paper", "article", "literature alert", "literature" -> LibraryCategory.ARTICLES
        "pathogen", "variant", "spike", "receptor", "disease" -> LibraryCategory.PATHOGENS
        "topic", "research_topic", "glossary", "process" -> LibraryCategory.TOPICS
        else -> LibraryCategory.LABS
    }

    fun filter(
        bookmarks: List<BookmarkItem>,
        category: LibraryCategory,
        query: String
    ): List<BookmarkItem> {
        val q = query.trim().lowercase()
        return bookmarks.filter { item ->
            val catOk = category == LibraryCategory.ALL || classify(item) == category
            val queryOk = q.isEmpty() ||
                item.title.lowercase().contains(q) ||
                item.itemType.lowercase().contains(q) ||
                (item.subtitle?.lowercase()?.contains(q) == true) ||
                (item.meta?.lowercase()?.contains(q) == true)
            catOk && queryOk
        }
    }

    fun countByCategory(bookmarks: List<BookmarkItem>): Map<LibraryCategory, Int> {
        val counts = mutableMapOf<LibraryCategory, Int>()
        counts[LibraryCategory.ALL] = bookmarks.size
        LibraryCategory.entries.filter { it != LibraryCategory.ALL }.forEach { cat ->
            counts[cat] = bookmarks.count { classify(it) == cat }
        }
        return counts
    }

    fun resolveRoute(item: BookmarkItem): AppRoute = when (item.itemType.lowercase()) {
        "paper", "article", "literature alert", "literature" -> AppRoute.LITERATURE
        "pathogen", "variant", "spike", "receptor" -> AppRoute.PATHOGEN_LAB
        "disease" -> AppRoute.DISEASES
        "glossary" -> AppRoute.GLOSSARY
        "process", "organelle" -> AppRoute.CELLS
        "topic", "research_topic" -> AppRoute.LITERATURE
        "molecule" -> AppRoute.MOLECULES
        "pathway" -> AppRoute.SIGNALING
        "organ", "cell" -> AppRoute.ATLAS
        "lab" -> AppRoute.VIRTUAL_LAB
        "proteomics" -> AppRoute.PROTEOMICS
        "pharmacology" -> AppRoute.PHARMACOLOGY
        else -> AppRoute.DASHBOARD
    }

    fun displayBadge(itemType: String): String = when (itemType.lowercase()) {
        "paper", "article" -> "Scientific Article"
        "literature alert" -> "Literature Alert"
        "pathogen" -> "Pathogen Profile"
        "variant" -> "Variant / Strain"
        "spike" -> "Spike vs Receptor"
        "receptor" -> "Host Receptor"
        "disease" -> "Disease Profile"
        "glossary" -> "Glossary Concept"
        "process" -> "3D Cellular Process"
        "organelle" -> "3D Organelle"
        "topic", "research_topic" -> "Research Topic"
        "molecule" -> "3D Molecule"
        "pathway" -> "Signaling Pathway"
        "organ" -> "Immune Organ"
        "cell" -> "Immune Cell"
        "lab" -> "Virtual Lab"
        else -> itemType.replaceFirstChar { it.uppercaseChar() }
    }

    fun extractHttpUrl(meta: String?): String? {
        if (meta.isNullOrBlank()) return null
        val match = Regex("https?://[^\\s•]+").find(meta)
        return match?.value
    }
}
