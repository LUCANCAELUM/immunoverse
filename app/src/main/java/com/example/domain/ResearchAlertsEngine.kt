package com.example.domain

import com.example.data.kb.ImmunologyKb
import com.example.data.model.BookmarkItem
import com.example.data.model.ResearchProject
import com.example.data.network.LiteraturePaper
import com.example.ui.components.AppRoute

data class ResearchAlertItem(
    val id: String,
    val title: String,
    val summary: String,
    val source: String,
    val publishedLabel: String,
    val matchedInterest: String,
    val route: AppRoute,
    val doi: String? = null,
    val url: String? = null,
    val isLivePaper: Boolean = false
)

object ResearchAlertsEngine {
    val PRESET_INTEREST_TOPICS: List<String> = listOf(
        "SARS-CoV-2 Spike & Variants",
        "CAR-T & Checkpoint Blockade",
        "Proteomics & Mass Spectrometry",
        "HIV Latency & bNAbs",
        "Innate Inflammasome & Cytokines",
        "mRNA Vaccines & Adjuvants",
        "Autoimmunity & Regulatory T Cells",
        "Antimicrobial & Fungal Resistance"
    )

    fun extractEffectiveInterests(
        explicitInterests: Set<String>,
        bookmarks: List<BookmarkItem>,
        projects: List<ResearchProject>
    ): List<String> {
        val out = linkedSetOf<String>()
        out.addAll(explicitInterests.map { it.trim() }.filter { it.isNotBlank() })
        bookmarks.take(8).forEach { b ->
            if (b.title.isNotBlank()) out.add(b.title.trim())
        }
        projects.take(5).forEach { p ->
            if (p.name.isNotBlank()) out.add(p.name.trim())
        }
        if (out.isEmpty()) {
            out.add("CAR-T & Checkpoint Blockade")
            out.add("SARS-CoV-2 Spike & Variants")
        }
        return out.toList()
    }

    fun buildEuropePmcInterestQuery(interests: List<String>): String {
        val keywords = interests.take(4).map { raw ->
            raw.replace(Regex("[^A-Za-z0-9\\s-]"), " ")
                .trim()
                .split(Regex("\\s+"))
                .filter { it.length >= 3 }
                .take(3)
                .joinToString(" AND ")
        }.filter { it.isNotBlank() }

        return if (keywords.isEmpty()) {
            "immunology OR immunotherapy OR virology"
        } else {
            keywords.joinToString(" OR ") { "($it)" }
        }
    }

    fun buildAlerts(
        interests: List<String>,
        livePapers: List<LiteraturePaper>
    ): List<ResearchAlertItem> {
        val alerts = mutableListOf<ResearchAlertItem>()
        val lowerInterests = interests.map { it to tokenizeInterest(it) }

        // 1. Live literature papers matched against user's saved research interests
        for (paper in livePapers) {
            val haystack = "${paper.title} ${paper.abstractText} ${paper.journal}".lowercase()
            val bestMatch = lowerInterests
                .map { (label, tokens) ->
                    val hits = tokens.count { tk -> haystack.contains(tk) }
                    label to hits
                }
                .maxByOrNull { it.second }

            val matchedLabel = if (bestMatch != null && bestMatch.second > 0) {
                bestMatch.first
            } else {
                interests.firstOrNull() ?: "General Immunology"
            }

            alerts += ResearchAlertItem(
                id = "live_${paper.id}",
                title = paper.title,
                summary = listOfNotNull(
                    paper.authors.takeIf { it.isNotBlank() },
                    paper.journal.takeIf { it.isNotBlank() },
                    paper.abstractText.take(150).takeIf { it.isNotBlank() }
                ).joinToString(" • ").ifBlank { "Recent peer-reviewed study matching your research focus." },
                source = paper.source,
                publishedLabel = paper.year.ifBlank { "Recent" },
                matchedInterest = matchedLabel,
                route = AppRoute.LITERATURE,
                doi = paper.doi,
                url = paper.url,
                isLivePaper = true
            )
        }

        // 2. Curated immunology breakthroughs & landmark updates matched to saved interests
        val curatedBreakthroughs = listOf(
            ResearchAlertItem(
                id = "curated_flirt",
                title = "Convergent Spike RBD Escape (L455S + F456L) vs Broadly Neutralizing Class-3 Sera",
                summary = "Explores altered ACE2 dwell kinetics and neutralizing antibody sensitivity across JN.1 / KP.3 lineages.",
                source = "Breakthrough Digest",
                publishedLabel = "Structural Virology",
                matchedInterest = "SARS-CoV-2 Spike & Variants",
                route = AppRoute.PATHOGEN_LAB
            ),
            ResearchAlertItem(
                id = "curated_cart",
                title = "Armored IL-18/IL-15 CAR-T Persistence & Exhaustion-Resistant TOX Chromatin Rewiring",
                summary = "Sustained effector memory CAR-T expansion in solid tumor microenvironments with mitigated CRS.",
                source = "Immunotherapy Update",
                publishedLabel = "Translational",
                matchedInterest = "CAR-T & Checkpoint Blockade",
                route = AppRoute.SIGNALING
            ),
            ResearchAlertItem(
                id = "curated_proteomics",
                title = "DIA-PASEF Immunopeptidomics & Deep LFQ Quantification of TCR Synapse Phospho-Sites",
                summary = "Sub-ppm tandem b/y ion matching expands neoantigen discovery on HLA Class I/II alleles.",
                source = "Proteomics Brief",
                publishedLabel = "Mass Spectrometry",
                matchedInterest = "Proteomics & Mass Spectrometry",
                route = AppRoute.PROTEOMICS
            ),
            ResearchAlertItem(
                id = "curated_inflammasome",
                title = "Gasdermin-D Oligomer Pore Repair via ESCRT-III vs Pyroptotic IL-1β Burst",
                summary = "Calcium influx tunes sublytic cytokine secretion vs full pyroptotic membrane rupture.",
                source = "Innate Immunity Alert",
                publishedLabel = "Cellular Arena",
                matchedInterest = "Innate Inflammasome & Cytokines",
                route = AppRoute.CELLS
            ),
            ResearchAlertItem(
                id = "curated_hiv",
                title = "CD4bs Broadly Neutralizing Antibody + LRA Cocktails Shrinking Intact Proviral Reservoirs",
                summary = "Triple bNAb combinations delay viral rebound and enhance ADCC clearance of reactivated latently infected T cells.",
                source = "Virology Dispatch",
                publishedLabel = "HIV Research",
                matchedInterest = "HIV Latency & bNAbs",
                route = AppRoute.DISEASES
            ),
            ResearchAlertItem(
                id = "curated_mrna",
                title = "N1-Methylpseudouridine & Ionizable Lipid Nanoparticle Adjuvancy in Germinal Center Tfh Priming",
                summary = "LNPs boost IL-6/IL-21 follicular helper T-cell cues and somatic hypermutation breadth.",
                source = "Vaccine Immunology",
                publishedLabel = "Adaptive",
                matchedInterest = "mRNA Vaccines & Adjuvants",
                route = AppRoute.SIMULATOR
            )
        ) + ImmunologyKb.DAILY.take(4).mapIndexed { idx, disc ->
            ResearchAlertItem(
                id = "disc_${disc.refId}_$idx",
                title = disc.title,
                summary = disc.body,
                source = "Landmark Discovery",
                publishedLabel = disc.status,
                matchedInterest = interests.firstOrNull { intLabel ->
                    tokenizeInterest(intLabel).any { tk ->
                        "${disc.title} ${disc.body} ${disc.status}".lowercase().contains(tk)
                    }
                } ?: disc.status,
                route = AppRoute.GLOSSARY
            )
        }

        // Prioritize curated breakthroughs matching user interests first
        val rankedCurated = curatedBreakthroughs.sortedByDescending { alert ->
            lowerInterests.maxOfOrNull { (label, tokens) ->
                if (alert.matchedInterest.equals(label, ignoreCase = true)) 10
                else tokens.count { tk -> "${alert.title} ${alert.summary} ${alert.matchedInterest}".lowercase().contains(tk) }
            } ?: 0
        }

        return (alerts + rankedCurated).distinctBy { it.id }.take(18)
    }

    private fun tokenizeInterest(raw: String): List<String> =
        raw.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 3 && it !in setOf("and", "the", "for", "with") }
}
