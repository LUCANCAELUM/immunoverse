package com.example.data.kb

import com.example.domain.PathogenClass
import com.example.domain.PathogenDesign
import com.example.domain.TropismTarget

enum class PathogenKingdom(val label: String) {
    VIRUS("Viruses"),
    BACTERIUM("Bacteria"),
    FUNGUS("Fungi"),
    PARASITE("Protozoan Parasites"),
    AMOEBA("Amoebae"),
    HELMINTH("Helminths")
}

data class PathogenOrganellePart(
    val id: String,
    val label: String,
    val role: String
)

data class PathogenVariantNode(
    val id: String,
    val generation: Int,
    val strainLabel: String,
    val lineageOrClade: String,
    val mutationSummary: String,
    val mutatedPartId: String,
    val phenotypicShift: String,
    val immuneEscapePct: Int,
    val transmissionMult: Double,
    val drugResistanceNote: String,
    val designOverrides: PathogenDesign
)

data class PathogenTaxonomyEntry(
    val id: String,
    val kingdom: PathogenKingdom,
    val organismName: String,
    val morphologyTag: String,
    val sizeScale: String,
    val summary: String,
    val parts: List<PathogenOrganellePart>,
    val variants: List<PathogenVariantNode>
)

object PathogenTaxonomyKb {
    val SPECIMENS: List<PathogenTaxonomyEntry> = listOf(
        PathogenTaxonomyEntry(
            id = "virus_sarscov2",
            kingdom = PathogenKingdom.VIRUS,
            organismName = "SARS-CoV-2 (Coronavirus)",
            morphologyTag = "Enveloped +ssRNA Virion with Trimeric Spikes",
            sizeScale = "~100–120 nm diameter",
            summary = "Pleomorphic lipid-enveloped virion crowned with homotrimeric Spike (S) glycoproteins housing a helically encapsidated ~30 kb +ssRNA genome.",
            parts = listOf(
                PathogenOrganellePart("spike_trimers", "Spike (S) RBD Trimers", "Receptor-binding domain engages host ACE2; primary target of neutralizing IgG and RBD escape mutations."),
                PathogenOrganellePart("lipid_env", "Lipid Bilayer Envelope", "Host-derived phospholipid membrane studded with E and M structural proteins; disrupted by detergents."),
                PathogenOrganellePart("nucleocapsid_rna", "Nucleocapsid (N) + ssRNA", "Phosphoprotein N packages positive-sense genomic RNA for cytosolic RdRp replication."),
                PathogenOrganellePart("rdrp_core", "RdRp Replicase Complex", "nsp12 RNA-dependent RNA polymerase (target of remdesivir / molnupiravir); proofreading nsp14 ExoN slows mutation rate.")
            ),
            variants = listOf(
                PathogenVariantNode(
                    id = "gen0_wuhan",
                    generation = 0,
                    strainLabel = "Gen 0: Ancestral (Wuhan-Hu-1)",
                    lineageOrClade = "Lineage A / B",
                    mutationSummary = "Wild-type Spike (D614)",
                    mutatedPartId = "spike_trimers",
                    phenotypicShift = "Baseline ACE2 affinity & lower-lung TMPRSS2 fusion; minimal pre-existing antibody escape.",
                    immuneEscapePct = 5,
                    transmissionMult = 1.0,
                    drugResistanceNote = "Susceptible to ancestral monoclonal antibodies & nirmatrelvir/remdesivir.",
                    designOverrides = PathogenDesign(
                        id = "scov2_wuhan_tax", name = "SARS-CoV-2 Ancestral", pathogenClass = PathogenClass.ENVELOPED_RNA_VIRUS,
                        tropism = TropismTarget.LOWER_ALVEOLAR, replicationRate = 1.05, receptorAffinity = 0.95, immuneEvasion = 0.65, antigenicDrift = 0.15
                    )
                ),
                PathogenVariantNode(
                    id = "gen1_alpha",
                    generation = 1,
                    strainLabel = "Gen 1: Alpha Variant",
                    lineageOrClade = "B.1.1.7 (Clade 20I)",
                    mutationSummary = "S: N501Y, Δ69-70, P681H, ORF8 Q27*",
                    mutatedPartId = "spike_trimers",
                    phenotypicShift = "N501Y tightens ACE2 affinity ~1.5x; Δ69-70 increases pseudotyped infectivity.",
                    immuneEscapePct = 22,
                    transmissionMult = 1.5,
                    drugResistanceNote = "Retains sensitivity to most mAbs & all direct-acting antivirals.",
                    designOverrides = PathogenDesign(
                        id = "scov2_alpha_tax", name = "SARS-CoV-2 Alpha (B.1.1.7)", pathogenClass = PathogenClass.ENVELOPED_RNA_VIRUS,
                        tropism = TropismTarget.LOWER_ALVEOLAR, replicationRate = 1.25, receptorAffinity = 1.25, immuneEvasion = 0.72, antigenicDrift = 0.35
                    )
                ),
                PathogenVariantNode(
                    id = "gen2_delta",
                    generation = 2,
                    strainLabel = "Gen 2: Delta Variant",
                    lineageOrClade = "B.1.617.2 (Clade 21A)",
                    mutationSummary = "S: L452R, T478K, P681R",
                    mutatedPartId = "lipid_env",
                    phenotypicShift = "P681R furin cleavage accelerates cell–cell syncytia fusion & peak airway load.",
                    immuneEscapePct = 42,
                    transmissionMult = 2.1,
                    drugResistanceNote = "Bamlanivimab reduced; sensitive to casirivimab/imdevimab & nirmatrelvir.",
                    designOverrides = PathogenDesign(
                        id = "scov2_delta_tax", name = "SARS-CoV-2 Delta (B.1.617.2)", pathogenClass = PathogenClass.ENVELOPED_RNA_VIRUS,
                        tropism = TropismTarget.LOWER_ALVEOLAR, replicationRate = 1.48, receptorAffinity = 1.40, immuneEvasion = 0.80, antigenicDrift = 0.55
                    )
                ),
                PathogenVariantNode(
                    id = "gen3_omicron_ba1",
                    generation = 3,
                    strainLabel = "Gen 3: Omicron BA.1",
                    lineageOrClade = "B.1.1.529 (Clade 21K)",
                    mutationSummary = "S: >30 muts (G339D, K417N, E484A, Q493R, N501Y)",
                    mutatedPartId = "spike_trimers",
                    phenotypicShift = "Endosomal cathepsin entry shift favoring upper bronchi; massive Class 1/2/3 RBD escape.",
                    immuneEscapePct = 78,
                    transmissionMult = 3.2,
                    drugResistanceNote = "Escapes most first-gen mAbs; conserved nsp5 Mpro remains nirmatrelvir-sensitive.",
                    designOverrides = PathogenDesign(
                        id = "scov2_ba1_tax", name = "SARS-CoV-2 Omicron BA.1", pathogenClass = PathogenClass.ENVELOPED_RNA_VIRUS,
                        tropism = TropismTarget.UPPER_RESPIRATORY, replicationRate = 1.50, receptorAffinity = 1.65, immuneEvasion = 0.88, antigenicDrift = 1.15
                    )
                ),
                PathogenVariantNode(
                    id = "gen4_jn1",
                    generation = 4,
                    strainLabel = "Gen 4: Omicron JN.1 / KP.3",
                    lineageOrClade = "BA.2.86 descendant (Clade 24A)",
                    mutationSummary = "S: L455S + F456L ('FLiRT'), K478R",
                    mutatedPartId = "rdrp_core",
                    phenotypicShift = "Convergent L455S+F456L evasion of XBB-imprinted neutralizing sera while retaining high ACE2 engagement.",
                    immuneEscapePct = 91,
                    transmissionMult = 3.6,
                    drugResistanceNote = "Escapes legacy mAbs; conserved RdRp/Mpro antivirals remain effective.",
                    designOverrides = PathogenDesign(
                        id = "scov2_jn1_tax", name = "SARS-CoV-2 JN.1", pathogenClass = PathogenClass.ENVELOPED_RNA_VIRUS,
                        tropism = TropismTarget.UPPER_RESPIRATORY, replicationRate = 1.55, receptorAffinity = 1.75, immuneEvasion = 0.92, antigenicDrift = 1.32
                    )
                )
            )
        ),
        PathogenTaxonomyEntry(
            id = "bact_staph_tb",
            kingdom = PathogenKingdom.BACTERIUM,
            organismName = "Staphylococcus aureus / Bacillus Model",
            morphologyTag = "Capsulated Cell Wall with Resistance Plasmid & Pili/Flagella",
            sizeScale = "~0.8–2.5 µm",
            summary = "Prokaryotic pathogen shielded by a peptidoglycan–capsule envelope enclosing circular chromosomal DNA, 70S ribosomes, and horizontally transferable resistance plasmids.",
            parts = listOf(
                PathogenOrganellePart("capsule_wall", "Capsule & Peptidoglycan Wall", "Polysaccharide microcapsule + cross-linked murein sacculus; PBP transpeptidases are targeted by beta-lactams."),
                PathogenOrganellePart("plasmid_ring", "R-Plasmid (mecA / blaZ)", "Extrachromosomal circular DNA carrying beta-lactamase or PBP2a cassettes acquired via horizontal gene transfer."),
                PathogenOrganellePart("nucleoid_core", "Circular Nucleoid + Gyrase", "Supercoiled bacterial chromosome; DNA gyrase/RNA polymerase (rpoB) are targeted by fluoroquinolones & rifamycins."),
                PathogenOrganellePart("pili_flagellum", "Adhesin Pili / Flagellum", "Surface protein filaments mediating mucosal colonization, biofilm seeding, and motility (TLR5 agonist).")
            ),
            variants = listOf(
                PathogenVariantNode(
                    id = "bact_gen0",
                    generation = 0,
                    strainLabel = "Gen 0: MSSA Ancestral Strain",
                    lineageOrClade = "CC8 Wild-Type",
                    mutationSummary = "Native PBP1-4 (no SCCmec cassette)",
                    mutatedPartId = "capsule_wall",
                    phenotypicShift = "Standard peptidoglycan synthesis; high susceptibility to oxacillin, cefazolin, and opsonophagocytosis.",
                    immuneEscapePct = 20,
                    transmissionMult = 1.0,
                    drugResistanceNote = "Beta-lactam susceptible.",
                    designOverrides = PathogenDesign(
                        id = "mssa_wt", name = "MSSA Wild-Type", pathogenClass = PathogenClass.EXTRACELLULAR_BACTERIUM,
                        tropism = TropismTarget.SYSTEMIC_ENDOTHELIUM, replicationRate = 1.15, receptorAffinity = 0.95, immuneEvasion = 0.55, antigenicDrift = 0.15
                    )
                ),
                PathogenVariantNode(
                    id = "bact_gen1",
                    generation = 1,
                    strainLabel = "Gen 1: Penicillinase Plasmid Strain",
                    lineageOrClade = "blaZ+ Plasmid Lineage",
                    mutationSummary = "Plasmid blaZ beta-lactamase acquisition",
                    mutatedPartId = "plasmid_ring",
                    phenotypicShift = "Hydrolyzes penicillin G/ampicillin via secreted serine beta-lactamase.",
                    immuneEscapePct = 30,
                    transmissionMult = 1.15,
                    drugResistanceNote = "Resistant to penicillin; susceptible to oxacillin/nafcillin/vancomycin.",
                    designOverrides = PathogenDesign(
                        id = "mssa_blaz", name = "S. aureus blaZ+", pathogenClass = PathogenClass.EXTRACELLULAR_BACTERIUM,
                        tropism = TropismTarget.SYSTEMIC_ENDOTHELIUM, replicationRate = 1.20, receptorAffinity = 1.00, immuneEvasion = 0.65, antigenicDrift = 0.20
                    )
                ),
                PathogenVariantNode(
                    id = "bact_gen2",
                    generation = 2,
                    strainLabel = "Gen 2: MRSA USA300",
                    lineageOrClade = "ST8 / SCCmec IV",
                    mutationSummary = "mecA (PBP2a low beta-lactam affinity) + PVL toxin",
                    mutatedPartId = "capsule_wall",
                    phenotypicShift = "Alternative transpeptidase PBP2a builds cell wall even in presence of methicillin; Panton-Valentine leukocidin lyses neutrophils.",
                    immuneEscapePct = 58,
                    transmissionMult = 1.45,
                    drugResistanceNote = "Pan-beta-lactam resistant (except ceftaroline); requires vancomycin, daptomycin, or linezolid.",
                    designOverrides = PathogenDesign(
                        id = "mrsa_usa300_tax", name = "MRSA USA300", pathogenClass = PathogenClass.EXTRACELLULAR_BACTERIUM,
                        tropism = TropismTarget.SYSTEMIC_ENDOTHELIUM, replicationRate = 1.35, receptorAffinity = 1.05, immuneEvasion = 0.95, antigenicDrift = 0.25
                    )
                ),
                PathogenVariantNode(
                    id = "bact_gen3",
                    generation = 3,
                    strainLabel = "Gen 3: VISA Thick-Wall Variant",
                    lineageOrClade = "walKR / vraSR Adaptive",
                    mutationSummary = "walKR + graSR mutations (thickened murein)",
                    mutatedPartId = "capsule_wall",
                    phenotypicShift = "Overproduces false D-Ala-D-Ala decoy peptidoglycan layers trapping vancomycin before it reaches plasma membrane.",
                    immuneEscapePct = 70,
                    transmissionMult = 1.30,
                    drugResistanceNote = "Vancomycin-Intermediate (MIC 4–8 µg/mL); switch to daptomycin/linezolid.",
                    designOverrides = PathogenDesign(
                        id = "visa_tax", name = "VISA Thick-Wall", pathogenClass = PathogenClass.EXTRACELLULAR_BACTERIUM,
                        tropism = TropismTarget.SYSTEMIC_ENDOTHELIUM, replicationRate = 1.18, receptorAffinity = 1.05, immuneEvasion = 1.10, antigenicDrift = 0.35
                    )
                ),
                PathogenVariantNode(
                    id = "bact_gen4",
                    generation = 4,
                    strainLabel = "Gen 4: Biofilm Hyper-Pili Persister",
                    lineageOrClade = "agr-Dysfunctional Clone",
                    mutationSummary = "agrC loss + fnbA biofilm upregulation",
                    mutatedPartId = "pili_flagellum",
                    phenotypicShift = "Forms dense extracellular polymeric biofilms shielding stationary persister cells from both antibodies and neutrophils.",
                    immuneEscapePct = 82,
                    transmissionMult = 1.40,
                    drugResistanceNote = "High biofilm tolerance; requires source control + rifampin/daptomycin combination.",
                    designOverrides = PathogenDesign(
                        id = "staph_biofilm", name = "Biofilm Persister Strain", pathogenClass = PathogenClass.EXTRACELLULAR_BACTERIUM,
                        tropism = TropismTarget.SYSTEMIC_ENDOTHELIUM, replicationRate = 0.95, receptorAffinity = 1.15, immuneEvasion = 1.25, antigenicDrift = 0.45
                    )
                )
            )
        ),
        PathogenTaxonomyEntry(
            id = "fungi_candida",
            kingdom = PathogenKingdom.FUNGUS,
            organismName = "Candida auris (Eukaryotic Yeast)",
            morphologyTag = "Budding Eukaryotic Yeast with Chitin-Glucan Wall & Ergosterol Membrane",
            sizeScale = "~3–6 µm",
            summary = "Opportunistic budding fungal pathogen featuring a layered mannan/β-glucan/chitin cell wall, ergosterol-rich membrane, and drug efflux pumps.",
            parts = listOf(
                PathogenOrganellePart("chitin_wall", "Mannan / β-Glucan / Chitin Wall", "Multilayered fungal cell wall sensed by host Dectin-1; FKS1 glucan synthase is inhibited by echinocandins."),
                PathogenOrganellePart("ergosterol_mem", "Ergosterol Membrane & Efflux", "Contains CYP51 (ERG11 target of azoles) and CDR1/MDR1 ABC transporters driving multidrug antifungal efflux."),
                PathogenOrganellePart("fungal_nucleus", "Eukaryotic Nucleus (ERG11/FKS1)", "Diploid/haploid chromatin encoding stress-response heat-shock chaperones (Hsp90) and resistance alleles."),
                PathogenOrganellePart("daughter_bud", "Budding Scar / Hyphal Apex", "Polarized chitin-ring bud site enabling rapid clonal expansion and biofilm adherence on medical polymers.")
            ),
            variants = listOf(
                PathogenVariantNode(
                    id = "fung_gen0",
                    generation = 0,
                    strainLabel = "Gen 0: Azole-Susceptible Ancestor",
                    lineageOrClade = "Wild-Type Baseline",
                    mutationSummary = "Wild-type ERG11 & FKS1",
                    mutatedPartId = "fungal_nucleus",
                    phenotypicShift = "Normal CYP51 lanosterol 14α-demethylase & glucan synthase; cleared cleanly by fluconazole or caspofungin.",
                    immuneEscapePct = 25,
                    transmissionMult = 1.0,
                    drugResistanceNote = "Susceptible to azoles, echinocandins, and amphotericin B.",
                    designOverrides = PathogenDesign(
                        id = "candida_wt", name = "Candida Wild-Type", pathogenClass = PathogenClass.EXTRACELLULAR_BACTERIUM,
                        tropism = TropismTarget.SYSTEMIC_ENDOTHELIUM, replicationRate = 0.85, receptorAffinity = 0.85, immuneEvasion = 0.65, antigenicDrift = 0.15
                    )
                ),
                PathogenVariantNode(
                    id = "fung_gen1",
                    generation = 1,
                    strainLabel = "Gen 1: Clade I South Asian (ERG11)",
                    lineageOrClade = "Clade I (Y132F / K143R)",
                    mutationSummary = "ERG11 Y132F point mutation",
                    mutatedPartId = "ergosterol_mem",
                    phenotypicShift = "Alters azole heme-binding pocket in CYP51, conferring high-level fluconazole/voriconazole resistance.",
                    immuneEscapePct = 40,
                    transmissionMult = 1.35,
                    drugResistanceNote = "Fluconazole resistant; echinocandins active.",
                    designOverrides = PathogenDesign(
                        id = "candida_c1", name = "Candida auris Clade I", pathogenClass = PathogenClass.EXTRACELLULAR_BACTERIUM,
                        tropism = TropismTarget.SYSTEMIC_ENDOTHELIUM, replicationRate = 0.95, receptorAffinity = 0.95, immuneEvasion = 0.80, antigenicDrift = 0.30
                    )
                ),
                PathogenVariantNode(
                    id = "fung_gen2",
                    generation = 2,
                    strainLabel = "Gen 2: CDR1 Efflux Overexpressor",
                    lineageOrClade = "TAC1B Gain-of-Function",
                    mutationSummary = "TAC1B A640V → CDR1 pump amplification",
                    mutatedPartId = "ergosterol_mem",
                    phenotypicShift = "Constitutively pumps triazoles out of fungal cytosol while maintaining membrane ergosterol.",
                    immuneEscapePct = 52,
                    transmissionMult = 1.45,
                    drugResistanceNote = "Cross-resistant across triazoles.",
                    designOverrides = PathogenDesign(
                        id = "candida_efflux", name = "Candida TAC1B Efflux", pathogenClass = PathogenClass.EXTRACELLULAR_BACTERIUM,
                        tropism = TropismTarget.SYSTEMIC_ENDOTHELIUM, replicationRate = 1.00, receptorAffinity = 1.00, immuneEvasion = 0.88, antigenicDrift = 0.40
                    )
                ),
                PathogenVariantNode(
                    id = "fung_gen3",
                    generation = 3,
                    strainLabel = "Gen 3: Echinocandin Escape (FKS1)",
                    lineageOrClade = "FKS1 Hotspot-1 Mutant",
                    mutationSummary = "FKS1 S639P / S639F",
                    mutatedPartId = "chitin_wall",
                    phenotypicShift = "Compensatory chitin elevation masks β-glucan from host Dectin-1 macrophages and prevents micafungin/caspofungin block.",
                    immuneEscapePct = 72,
                    transmissionMult = 1.38,
                    drugResistanceNote = "Echinocandin resistant; requires liposomal amphotericin B or ibrexafungerp.",
                    designOverrides = PathogenDesign(
                        id = "candida_fks1", name = "Candida FKS1 Mutant", pathogenClass = PathogenClass.EXTRACELLULAR_BACTERIUM,
                        tropism = TropismTarget.SYSTEMIC_ENDOTHELIUM, replicationRate = 0.92, receptorAffinity = 1.05, immuneEvasion = 1.15, antigenicDrift = 0.55
                    )
                ),
                PathogenVariantNode(
                    id = "fung_gen4",
                    generation = 4,
                    strainLabel = "Gen 4: Pan-Resistant Aggregative Strain",
                    lineageOrClade = "ERG11 + FKS1 + ERG3/6",
                    mutationSummary = "Triple-class Ergosterol/Glucan/Efflux mutant",
                    mutatedPartId = "daughter_bud",
                    phenotypicShift = "Aggregative phenotype resists neutrophil NETosis and survives clinical concentrations of all 3 major antifungal classes.",
                    immuneEscapePct = 86,
                    transmissionMult = 1.50,
                    drugResistanceNote = "Pan-resistant outbreak clone; experimental synergy combinations required.",
                    designOverrides = PathogenDesign(
                        id = "candida_pan", name = "Candida Pan-Resistant", pathogenClass = PathogenClass.EXTRACELLULAR_BACTERIUM,
                        tropism = TropismTarget.SYSTEMIC_ENDOTHELIUM, replicationRate = 0.98, receptorAffinity = 1.10, immuneEvasion = 1.28, antigenicDrift = 0.70
                    )
                )
            )
        ),
        PathogenTaxonomyEntry(
            id = "parasite_plasmodium",
            kingdom = PathogenKingdom.PARASITE,
            organismName = "Plasmodium falciparum (Apicomplexan)",
            morphologyTag = "Polarized Merozoite with Apical Complex & Apicoplast",
            sizeScale = "~1.5 µm merozoite / 8 µm trophozoite",
            summary = "Intracellular protozoan malaria parasite equipped with specialized apical invasion organelles (rhoptries/micronemes), relict apicoplast plastid, and hemozoin digestive vacuole.",
            parts = listOf(
                PathogenOrganellePart("apical_complex", "Apical Complex (Rhoptries/AMA1)", "Secretory invasion machinery ejecting RON2/AMA1 moving-junction proteins to penetrate host erythrocytes."),
                PathogenOrganellePart("apicoplast", "Apicoplast Plastid (IPP)", "4-membrane relict plastid essential for isoprenoid/fatty-acid synthesis; target of doxycycline & clindamycin."),
                PathogenOrganellePart("food_vacuole", "Digestive Vacuole (Hemozoin)", "Acidic compartment digesting host hemoglobin into toxic heme detoxified into hemozoin crystals (PfCRT site)."),
                PathogenOrganellePart("parasite_nucleus", "Parasite Nucleus (var / PfEMP1)", "Contains ~60 hypervariable var genes encoding PfEMP1 antigenic surface switches to evade splenic clearance.")
            ),
            variants = listOf(
                PathogenVariantNode(
                    id = "para_gen0",
                    generation = 0,
                    strainLabel = "Gen 0: 3D7 Drug-Sensitive Strain",
                    lineageOrClade = "Reference Laboratory Clone",
                    mutationSummary = "Wild-type PfCRT (K76) & Kelch13",
                    mutatedPartId = "food_vacuole",
                    phenotypicShift = "Retains chloroquine inside acidic food vacuole, blocking heme polymerization cleanly.",
                    immuneEscapePct = 45,
                    transmissionMult = 1.0,
                    drugResistanceNote = "Sensitive to chloroquine, sulfadoxine-pyrimethamine, and artemisinin.",
                    designOverrides = PathogenDesign(
                        id = "pf_3d7", name = "P. falciparum 3D7", pathogenClass = PathogenClass.INTRACELLULAR_BACTERIUM,
                        tropism = TropismTarget.SYSTEMIC_ENDOTHELIUM, replicationRate = 0.90, receptorAffinity = 1.05, immuneEvasion = 0.85, antigenicDrift = 0.95
                    )
                ),
                PathogenVariantNode(
                    id = "para_gen1",
                    generation = 1,
                    strainLabel = "Gen 1: Chloroquine-Resistant (Dd2)",
                    lineageOrClade = "PfCRT K76T Mutation",
                    mutationSummary = "PfCRT K76T transporter efflux",
                    mutatedPartId = "food_vacuole",
                    phenotypicShift = "Positively charged Lys→Thr allows protonated chloroquine to leak out of digestive vacuole.",
                    immuneEscapePct = 55,
                    transmissionMult = 1.15,
                    drugResistanceNote = "Chloroquine resistant; switch to ACT (artemisinin combination therapy).",
                    designOverrides = PathogenDesign(
                        id = "pf_dd2", name = "P. falciparum Dd2 (PfCRT)", pathogenClass = PathogenClass.INTRACELLULAR_BACTERIUM,
                        tropism = TropismTarget.SYSTEMIC_ENDOTHELIUM, replicationRate = 0.95, receptorAffinity = 1.10, immuneEvasion = 0.95, antigenicDrift = 1.05
                    )
                ),
                PathogenVariantNode(
                    id = "para_gen2",
                    generation = 2,
                    strainLabel = "Gen 2: Triple DHFR Antifol Mutant",
                    lineageOrClade = "PfDHFR N51I + C59R + S108N",
                    mutationSummary = "PfDHFR triple active-site steric shift",
                    mutatedPartId = "parasite_nucleus",
                    phenotypicShift = "Sterically excludes pyrimethamine while preserving folate cofactor turnover.",
                    immuneEscapePct = 62,
                    transmissionMult = 1.25,
                    drugResistanceNote = "Pyrimethamine/SP resistance; ACT remains effective.",
                    designOverrides = PathogenDesign(
                        id = "pf_dhfr", name = "P. falciparum DHFR-3x", pathogenClass = PathogenClass.INTRACELLULAR_BACTERIUM,
                        tropism = TropismTarget.SYSTEMIC_ENDOTHELIUM, replicationRate = 1.00, receptorAffinity = 1.10, immuneEvasion = 1.00, antigenicDrift = 1.10
                    )
                ),
                PathogenVariantNode(
                    id = "para_gen3",
                    generation = 3,
                    strainLabel = "Gen 3: Kelch13 Artemisinin-Delay",
                    lineageOrClade = "KEL1/PLA1 (PfKelch13 C580Y)",
                    mutationSummary = "PfK13 C580Y → Reduced hemoglobin endocytosis",
                    mutatedPartId = "apicoplast",
                    phenotypicShift = "Slows ring-stage hemoglobin uptake, lowering heme-mediated activation of artemisinin endoperoxide bridges.",
                    immuneEscapePct = 76,
                    transmissionMult = 1.35,
                    drugResistanceNote = "Delayed parasite clearance under artemisinin; requires triple ACT regimens.",
                    designOverrides = PathogenDesign(
                        id = "pf_k13", name = "P. falciparum Kelch13 C580Y", pathogenClass = PathogenClass.INTRACELLULAR_BACTERIUM,
                        tropism = TropismTarget.SYSTEMIC_ENDOTHELIUM, replicationRate = 0.88, receptorAffinity = 1.18, immuneEvasion = 1.22, antigenicDrift = 1.25
                    )
                ),
                PathogenVariantNode(
                    id = "para_gen4",
                    generation = 4,
                    strainLabel = "Gen 4: PfHRP2/3 Diagnostic Escape",
                    lineageOrClade = "Subtelomeric hrp2/hrp3 Deletion",
                    mutationSummary = "Chr 8 & Chr 13 pfhrp2/pfhrp3 deletion",
                    mutatedPartId = "apical_complex",
                    phenotypicShift = "False-negative standard HRP2 rapid diagnostic tests allow undetected transmission & late treatment.",
                    immuneEscapePct = 85,
                    transmissionMult = 1.55,
                    drugResistanceNote = "Requires pLDH-based diagnostics or PCR/microscopy.",
                    designOverrides = PathogenDesign(
                        id = "pf_hrpdel", name = "P. falciparum hrp2/3-del", pathogenClass = PathogenClass.INTRACELLULAR_BACTERIUM,
                        tropism = TropismTarget.SYSTEMIC_ENDOTHELIUM, replicationRate = 1.02, receptorAffinity = 1.22, immuneEvasion = 1.30, antigenicDrift = 1.35
                    )
                )
            )
        ),
        PathogenTaxonomyEntry(
            id = "amoeba_entamoeba",
            kingdom = PathogenKingdom.AMOEBA,
            organismName = "Entamoeba histolytica / Naegleria",
            morphologyTag = "Motile Trophozoite with Dynamic Pseudopodia & Vacuoles",
            sizeScale = "~15–40 µm",
            summary = "Free-living or mucosal-invasive amoebozoan/percolozoan protozoa using actin-driven pseudopodia, Gal/GalNAc lectins, amebapores, and phagocytic vacuoles.",
            parts = listOf(
                PathogenOrganellePart("pseudopod_lobes", "Actin Pseudopodia & Lectin", "Dynamic actin-polymerizing lobes studded with Gal/GalNAc adhesin lectins binding colonic mucin & target cells."),
                PathogenOrganellePart("amoeba_nucleus", "Nucleus & Central Karyosome", "Eukaryotic nucleus with characteristic central chromatin karyosome & peripheral ring driving trophozoite gene expression."),
                PathogenOrganellePart("contractile_vac", "Contractile / Osmotic Vacuole", "Regulates cytosolic turgor pressure during fresh-water or tissue transitions."),
                PathogenOrganellePart("phago_vacuole", "Phagocytic Vacuole & Amebapore", "Engulfs host erythrocytes & bacteria; releases pore-forming amebapores and cysteine proteases upon contact.")
            ),
            variants = listOf(
                PathogenVariantNode(
                    id = "am_gen0",
                    generation = 0,
                    strainLabel = "Gen 0: Dormant Quadrinucleate Cyst",
                    lineageOrClade = "Environmental Transmission Stage",
                    mutationSummary = "Chitinous Jacob-lectin cyst wall",
                    mutatedPartId = "contractile_vac",
                    phenotypicShift = "Acid- and chlorine-tolerant environmentally stable stage enabling fecal-oral survival.",
                    immuneEscapePct = 35,
                    transmissionMult = 1.2,
                    drugResistanceNote = "Luminal agents (paromomycin / diloxanide) required to clear cysts.",
                    designOverrides = PathogenDesign(
                        id = "amoeba_cyst", name = "Entamoeba Cyst Stage", pathogenClass = PathogenClass.INTRACELLULAR_BACTERIUM,
                        tropism = TropismTarget.MACROPHAGE_NICHE, replicationRate = 0.55, receptorAffinity = 0.85, immuneEvasion = 1.15, antigenicDrift = 0.20
                    )
                ),
                PathogenVariantNode(
                    id = "am_gen1",
                    generation = 1,
                    strainLabel = "Gen 1: Luminal Commensal Trophozoite",
                    lineageOrClade = "Low-Virulence HM-1:IMSS Variant",
                    mutationSummary = "Baseline EhCP1-5 protease expression",
                    mutatedPartId = "amoeba_nucleus",
                    phenotypicShift = "Excysts in ileum; colonizes mucus layer without deep submucosal ulceration.",
                    immuneEscapePct = 42,
                    transmissionMult = 1.0,
                    drugResistanceNote = "Susceptible to metronidazole & tinidazole.",
                    designOverrides = PathogenDesign(
                        id = "amoeba_comm", name = "Entamoeba Commensal", pathogenClass = PathogenClass.EXTRACELLULAR_BACTERIUM,
                        tropism = TropismTarget.MACROPHAGE_NICHE, replicationRate = 0.80, receptorAffinity = 0.90, immuneEvasion = 0.80, antigenicDrift = 0.30
                    )
                ),
                PathogenVariantNode(
                    id = "am_gen2",
                    generation = 2,
                    strainLabel = "Gen 2: Gal/GalNAc Invasive Strain",
                    lineageOrClade = "Mucosal Attachment Activated",
                    mutationSummary = "hgl1/igl lectin clustering + EhCP-A5",
                    mutatedPartId = "pseudopod_lobes",
                    phenotypicShift = "Cleaves MUC2 mucin barrier, adheres tightly to colonocytes, and initiates flask-shaped ulcers.",
                    immuneEscapePct = 60,
                    transmissionMult = 1.25,
                    drugResistanceNote = "Treated with nitroimidazoles + luminal agent.",
                    designOverrides = PathogenDesign(
                        id = "amoeba_inv", name = "Entamoeba Invasive", pathogenClass = PathogenClass.EXTRACELLULAR_BACTERIUM,
                        tropism = TropismTarget.SYSTEMIC_ENDOTHELIUM, replicationRate = 1.05, receptorAffinity = 1.30, immuneEvasion = 0.95, antigenicDrift = 0.45
                    )
                ),
                PathogenVariantNode(
                    id = "am_gen3",
                    generation = 3,
                    strainLabel = "Gen 3: Amebapore & Liver-Abscess Strain",
                    lineageOrClade = "Hyper-Erythrophagocytic Variant",
                    mutationSummary = "EhA1–3 amebapore overexpression + complement cleavage",
                    mutatedPartId = "phago_vacuole",
                    phenotypicShift = "Enters portal circulation, cleaves C3a/C5a/IgA, and forms cytolytic hepatic abscess lesions.",
                    immuneEscapePct = 78,
                    transmissionMult = 1.35,
                    drugResistanceNote = "High tissue invasiveness; requires systemic metronidazole.",
                    designOverrides = PathogenDesign(
                        id = "amoeba_hep", name = "Entamoeba Hepatic", pathogenClass = PathogenClass.EXTRACELLULAR_BACTERIUM,
                        tropism = TropismTarget.SYSTEMIC_ENDOTHELIUM, replicationRate = 1.20, receptorAffinity = 1.45, immuneEvasion = 1.22, antigenicDrift = 0.55
                    )
                ),
                PathogenVariantNode(
                    id = "am_gen4",
                    generation = 4,
                    strainLabel = "Gen 4: Neurotropic Flagellate-Switch (Naegleria)",
                    lineageOrClade = "Ameba–Flagellate Plasticity",
                    mutationSummary = "Olfactory-nerve chemotaxis + Nfa1 pore cytotoxin",
                    mutatedPartId = "pseudopod_lobes",
                    phenotypicShift = "Rapidly switches between biflagellate swimming dispersal and destructive trogocytic pseudopod feeding.",
                    immuneEscapePct = 88,
                    transmissionMult = 1.60,
                    drugResistanceNote = "Requires rapid multi-agent CNS-penetrant miltefosine + amphotericin B regimen.",
                    designOverrides = PathogenDesign(
                        id = "naegleria_gen4", name = "Neurotropic Amoeba", pathogenClass = PathogenClass.EXTRACELLULAR_BACTERIUM,
                        tropism = TropismTarget.SYSTEMIC_ENDOTHELIUM, replicationRate = 1.55, receptorAffinity = 1.60, immuneEvasion = 1.35, antigenicDrift = 0.65
                    )
                )
            )
        ),
        PathogenTaxonomyEntry(
            id = "helminth_schisto",
            kingdom = PathogenKingdom.HELMINTH,
            organismName = "Schistosoma mansoni (Blood Fluke)",
            morphologyTag = "Multicellular Trematode with Suckers & Syncytial Tegument",
            sizeScale = "~10–15 mm adult",
            summary = "Vascular helminth cloaked in a host-antigen-masking syncytial tegument and double lipid bilayer, anchored by oral & ventral suckers in mesenteric venules.",
            parts = listOf(
                PathogenOrganellePart("tegument_shield", "Syncytial Tegument & Glycocalyx", "Double-membrane surface absorbing host ABO/MHC glycolipids to mask the worm from complement and IgE/eosinophil ADCC."),
                PathogenOrganellePart("oral_ventral_sucker", "Oral & Ventral Acetabulum Suckers", "Muscular attachment organs anchoring paired worms against mesenteric venous blood shear."),
                PathogenOrganellePart("gut_cecum", "Bifurcated Blind Gut & Hemozoin", "Digests ingested host erythrocytes and regurgitates dark hematin pigment."),
                PathogenOrganellePart("sensory_spines", "Surface Tubercles & Spines", "Cholinergic/TRP calcium-channel targets activated by praziquantel, causing spastic tegumental disruption.")
            ),
            variants = listOf(
                PathogenVariantNode(
                    id = "helm_gen0",
                    generation = 0,
                    strainLabel = "Gen 0: Miracidium Ciliated Stage",
                    lineageOrClade = "Freshwater Snail-Infective",
                    mutationSummary = "Ciliated epidermal plates",
                    mutatedPartId = "oral_ventral_sucker",
                    phenotypicShift = "Chemotactically locates Biomphalaria intermediate snail host.",
                    immuneEscapePct = 15,
                    transmissionMult = 1.0,
                    drugResistanceNote = "Aquatic pre-infective stage.",
                    designOverrides = PathogenDesign(
                        id = "helm_mir", name = "S. mansoni Miracidium", pathogenClass = PathogenClass.EXTRACELLULAR_BACTERIUM,
                        tropism = TropismTarget.SYSTEMIC_ENDOTHELIUM, replicationRate = 0.60, receptorAffinity = 0.90, immuneEvasion = 0.50, antigenicDrift = 0.20
                    )
                ),
                PathogenVariantNode(
                    id = "helm_gen1",
                    generation = 1,
                    strainLabel = "Gen 1: Cercaria Skin-Penetrating",
                    lineageOrClade = "Elastase Secretory Variant",
                    mutationSummary = "SmCE elastase + forked dispersal tail",
                    mutatedPartId = "oral_ventral_sucker",
                    phenotypicShift = "Penetrates intact human epidermis within minutes and sheds glycocalyx to transform into schistosomulum.",
                    immuneEscapePct = 45,
                    transmissionMult = 1.35,
                    drugResistanceNote = "Praziquantel less active on migrating immature schistosomula.",
                    designOverrides = PathogenDesign(
                        id = "helm_cerc", name = "S. mansoni Cercaria", pathogenClass = PathogenClass.EXTRACELLULAR_BACTERIUM,
                        tropism = TropismTarget.SYSTEMIC_ENDOTHELIUM, replicationRate = 0.72, receptorAffinity = 1.25, immuneEvasion = 0.90, antigenicDrift = 0.35
                    )
                ),
                PathogenVariantNode(
                    id = "helm_gen2",
                    generation = 2,
                    strainLabel = "Gen 2: Adult Host-Masked Pair",
                    lineageOrClade = "Mature Mesenteric Strain",
                    mutationSummary = "Sm29 / CD59-mimic tegument shield",
                    mutatedPartId = "tegument_shield",
                    phenotypicShift = "Th2/Treg skewing (IL-10/TGF-β) prevents sterile clearance for years; susceptible to praziquantel Ca2+ influx.",
                    immuneEscapePct = 82,
                    transmissionMult = 1.25,
                    drugResistanceNote = "Fully susceptible to single-dose praziquantel.",
                    designOverrides = PathogenDesign(
                        id = "helm_adult", name = "S. mansoni Adult Pair", pathogenClass = PathogenClass.EXTRACELLULAR_BACTERIUM,
                        tropism = TropismTarget.SYSTEMIC_ENDOTHELIUM, replicationRate = 0.65, receptorAffinity = 1.05, immuneEvasion = 1.32, antigenicDrift = 0.60
                    )
                ),
                PathogenVariantNode(
                    id = "helm_gen3",
                    generation = 3,
                    strainLabel = "Gen 3: Reduced PZQ Sensitivity (TRPM)",
                    lineageOrClade = "Sm.TRPM-PZQ Regulatory Variant",
                    mutationSummary = "TRP melastatin Ca2+ channel polymorphism",
                    mutatedPartId = "sensory_spines",
                    phenotypicShift = "Blunts praziquantel-induced Ca2+ overload and tegumental blebbing while maintaining host immune masking.",
                    immuneEscapePct = 88,
                    transmissionMult = 1.30,
                    drugResistanceNote = "Reduced praziquantel susceptibility; requires repeat dosing or oxamniquine.",
                    designOverrides = PathogenDesign(
                        id = "helm_pzq", name = "S. mansoni TRPM-Variant", pathogenClass = PathogenClass.EXTRACELLULAR_BACTERIUM,
                        tropism = TropismTarget.SYSTEMIC_ENDOTHELIUM, replicationRate = 0.68, receptorAffinity = 1.05, immuneEvasion = 1.38, antigenicDrift = 0.72
                    )
                )
            )
        )
    )
}
