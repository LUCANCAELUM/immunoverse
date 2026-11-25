package com.example.data.kb

enum class GlossaryDiagramType(val label: String, val parts: List<String>) {
    ANTIBODY_Y_STRUCTURE(
        "Immunoglobulin Y-Architecture",
        listOf("Fab Antigen Site", "CDR Hypervariable Loop", "Hinge Disulfide (S-S)", "Fc Effector & C1q Domain")
    ),
    MHC_PRESENTATION(
        "MHC Class I vs Class II Antigen Presentation",
        listOf("MHC-I + β2m (Endogenous)", "CD8+ CTL Synapse", "MHC-II (Exogenous)", "CD4+ Helper Synapse")
    ),
    THREE_SIGNAL_ACTIVATION(
        "3-Signal T-Cell Activation & Checkpoints",
        listOf("Signal 1: pMHC-TCR", "Signal 2: CD28-CD80/86", "CTLA-4 / PD-1 Brake", "Signal 3: Polarizing Cytokines")
    ),
    COMPLEMENT_CASCADE(
        "Complement Pathways & MAC Assembly",
        listOf("Classical/Lectin/Alt Triggers", "C3 Convertase (C3b + C3a)", "C5b-C9 MAC Pore", "Opsonization")
    ),
    VDJ_RECOMBINATION(
        "Somatic V(D)J Gene Rearrangement",
        listOf("Germline V/D/J Segments", "RAG1/RAG2 Synapsis", "TdT Junctional Diversity", "Rearranged Ig/TCR Exon")
    ),
    GERMINAL_CENTER(
        "Germinal Center Affinity Maturation",
        listOf("Dark Zone (AID SHM)", "FDC Antigen Capture", "Tfh CD40L Selection", "Plasma / Memory B Output")
    ),
    CYTOKINE_JAK_STAT(
        "Cytokine Receptor JAK–STAT Transduction",
        listOf("Cytokine Receptor Dimer", "JAK1/JAK2/TYK2 Phosphorylation", "STAT Dimerization", "Nuclear ISRE/GAS Transcription")
    ),
    INFLAMMASOME_PYROPTOSIS(
        "NLRP3 Inflammasome & Gasdermin D Pyroptosis",
        listOf("TLR4/NF-κB Priming", "NLRP3-ASC-Caspase-1", "Mature IL-1β / IL-18", "GSDMD Membrane Pore")
    )
}

data class GlossaryTerm(
    val id: String,
    val term: String,
    val acronym: String,
    val category: String,
    val shortDefinition: String,
    val detailedMechanism: String,
    val clinicalRelevance: String,
    val diagramType: GlossaryDiagramType,
    val defaultFocusedPart: String,
    val relatedTermIds: List<String>
)

object ImmunologyGlossaryKb {
    val CATEGORIES = listOf(
        "All",
        "Antibodies & Complement",
        "Adaptive Immunity",
        "Innate Immunity",
        "Genetics & Receptors",
        "Clinical & Tolerance"
    )

    val TERMS: List<GlossaryTerm> = listOf(
        GlossaryTerm(
            id = "antibody_igg",
            term = "Immunoglobulin G (Antibody)",
            acronym = "IgG / Ab",
            category = "Antibodies & Complement",
            shortDefinition = "Y-shaped tetrameric glycoprotein secreted by plasma cells (~150 kDa) composed of two identical heavy chains and two light chains linked by disulfide bonds.",
            detailedMechanism = "Two variable Fab arms recognize conformational or linear epitopes via 6 CDR loops, while the constant Fc stem mediates placental transport (FcRn), NK ADCC (FcγRIIIa), opsonization, and classical complement C1q fixation.",
            clinicalRelevance = "Dominant serum neutralizing isotype in secondary recall responses, vaccines, and therapeutic monoclonal antibodies.",
            diagramType = GlossaryDiagramType.ANTIBODY_Y_STRUCTURE,
            defaultFocusedPart = "Fab Antigen Site",
            relatedTermIds = listOf("fab_fc", "opsonization", "germinal_center")
        ),
        GlossaryTerm(
            id = "fab_fc",
            term = "Fab & Fc Antibody Domains",
            acronym = "Fab / Fc",
            category = "Antibodies & Complement",
            shortDefinition = "Functional cleavage fragments of an antibody: Fab binds specific antigen, whereas Fc engages host immune effector receptors and complement.",
            detailedMechanism = "Papain cleaves above the hinge disulfide bonds to yield two monovalent Fab fragments plus one crystallizable Fc dimer. Pepsin cleaves below the hinge to yield bivalent F(ab')2.",
            clinicalRelevance = "Fab fragments (e.g., certolizumab pegol, digoxin immune Fab) neutralize targets without triggering Fc-mediated inflammation or complement.",
            diagramType = GlossaryDiagramType.ANTIBODY_Y_STRUCTURE,
            defaultFocusedPart = "Fc Effector & C1q Domain",
            relatedTermIds = listOf("antibody_igg", "complement_mac", "opsonization")
        ),
        GlossaryTerm(
            id = "mhc_class_i",
            term = "MHC Class I (HLA-A, -B, -C)",
            acronym = "MHC-I",
            category = "Adaptive Immunity",
            shortDefinition = "Cell-surface heterodimer expressed on virtually all nucleated cells presenting 8–10 amino acid cytosolic peptides to CD8+ cytotoxic T lymphocytes.",
            detailedMechanism = "Intracellular or viral proteins are degraded by the immunoproteasome, shuttled into the ER via TAP1/2 transporters, loaded onto heavy chain + β2-microglobulin with tapasin/calreticulin, and exported to the plasma membrane.",
            clinicalRelevance = "Downregulation of MHC-I by viruses or tumors triggers 'missing-self' NK-cell killing; HLA-B*57:01 predicts abacavir hypersensitivity.",
            diagramType = GlossaryDiagramType.MHC_PRESENTATION,
            defaultFocusedPart = "MHC-I + β2m (Endogenous)",
            relatedTermIds = listOf("mhc_class_ii", "cross_presentation", "tcr_signal1")
        ),
        GlossaryTerm(
            id = "mhc_class_ii",
            term = "MHC Class II (HLA-DP, -DQ, -DR)",
            acronym = "MHC-II",
            category = "Adaptive Immunity",
            shortDefinition = "Antigen-presenting heterodimer restricted to professional APCs (dendritic cells, macrophages, B cells) displaying 13–25 aa exogenous peptides to CD4+ T cells.",
            detailedMechanism = "Synthesized in the ER bound to invariant chain (Ii/CD74), trafficked to acidified endosomes where cathepsins leave CLIP in the groove, and swapped for phagocytosed microbial peptides via HLA-DM.",
            clinicalRelevance = "MHC-II alleles (e.g., HLA-DR3/DR4, HLA-DQ2/8) drive strong genetic susceptibility in Type 1 diabetes, rheumatoid arthritis, and celiac disease.",
            diagramType = GlossaryDiagramType.MHC_PRESENTATION,
            defaultFocusedPart = "MHC-II (Exogenous)",
            relatedTermIds = listOf("mhc_class_i", "tcr_signal1", "treg_tolerance")
        ),
        GlossaryTerm(
            id = "tcr_signal1",
            term = "T-Cell Receptor & Signal 1",
            acronym = "TCR / CD3",
            category = "Adaptive Immunity",
            shortDefinition = "Clonally distributed αβ heterodimer paired with CD3γδεζ signaling chains that decodes peptide–MHC complexes.",
            detailedMechanism = "Co-receptor (CD4 or CD8) recruits Lck kinase to phosphorylate CD3 ITAM motifs, creating docking sites for ZAP-70, which phosphorylates LAT and SLP-76 to trigger PLCγ1/Ca2+/NFAT, Ras/ERK, and NF-κB.",
            clinicalRelevance = "Signal 1 alone without Signal 2 induces clonal anergy rather than full T-cell proliferation.",
            diagramType = GlossaryDiagramType.THREE_SIGNAL_ACTIVATION,
            defaultFocusedPart = "Signal 1: pMHC-TCR",
            relatedTermIds = listOf("costimulation_checkpoints", "mhc_class_i", "vdj_recombination")
        ),
        GlossaryTerm(
            id = "costimulation_checkpoints",
            term = "Co-Stimulation & Immune Checkpoints",
            acronym = "CD28 / CTLA-4 / PD-1",
            category = "Clinical & Tolerance",
            shortDefinition = "Balanced positive (CD28–CD80/86) and inhibitory (CTLA-4, PD-1) receptor pairs governing whether T cells expand or become exhausted/tolerant.",
            detailedMechanism = "CD28 amplifies PI3K–Akt–mTOR survival and IL-2 transcription (Signal 2). CTLA-4 outcompetes CD28 for B7 ligands via trans-endocytosis, while PD-1 recruits SHP-2 phosphatase to dephosphorylate CD28 and TCR tails.",
            clinicalRelevance = "Checkpoint blockade antibodies (ipilimumab, pembrolizumab, nivolumab) unleash anti-tumor CD8+ T cells.",
            diagramType = GlossaryDiagramType.THREE_SIGNAL_ACTIVATION,
            defaultFocusedPart = "CTLA-4 / PD-1 Brake",
            relatedTermIds = listOf("tcr_signal1", "treg_tolerance", "cytokine_storm")
        ),
        GlossaryTerm(
            id = "complement_mac",
            term = "Complement Cascade & MAC",
            acronym = "C3 / C5b-C9",
            category = "Antibodies & Complement",
            shortDefinition = "Heat-labile plasma zymogen cascade converging at C3 cleavage to drive opsonization, chemotactic inflammation (C3a/C5a), and direct osmotic lysis via C5b–C9 pores.",
            detailedMechanism = "Classical (C1q–antibody), Lectin (MBL–mannose), and Alternative (spontaneous C3 tick-over) pathways assemble C3 convertases, generating C3b tags and terminal C5b–C6–C7–C8–poly-C9 Membrane Attack Complexes.",
            clinicalRelevance = "Deficiencies in C5–C9 predispose to recurrent invasive Neisseria infections; eculizumab blocks C5 cleavage in PNH and aHUS.",
            diagramType = GlossaryDiagramType.COMPLEMENT_CASCADE,
            defaultFocusedPart = "C5b-C9 MAC Pore",
            relatedTermIds = listOf("opsonization", "antibody_igg", "nlrp3_pyroptosis")
        ),
        GlossaryTerm(
            id = "opsonization",
            term = "Opsonization & Phagocytic Clearance",
            acronym = "C3b / FcγR",
            category = "Antibodies & Complement",
            shortDefinition = "Coating of microbes or particles with complement C3b/iC3b or IgG antibodies to dramatically accelerate engulfment by macrophages and neutrophils.",
            detailedMechanism = "Fab arms or covalently thioester-linked C3b bridge microbial surfaces to phagocyte FcγRI/II/III and CR1/CR3 integrins, triggering actin-cup engulfment and NADPH oxidase respiratory burst.",
            clinicalRelevance = "Explains why splenic macrophages rapidly clear encapsulated bacteria (S. pneumoniae, H. influenzae) once opsonizing IgG/complement forms.",
            diagramType = GlossaryDiagramType.COMPLEMENT_CASCADE,
            defaultFocusedPart = "Opsonization",
            relatedTermIds = listOf("complement_mac", "antibody_igg", "fab_fc")
        ),
        GlossaryTerm(
            id = "vdj_recombination",
            term = "V(D)J Somatic Recombination",
            acronym = "RAG1 / RAG2 / TdT",
            category = "Genetics & Receptors",
            shortDefinition = "Site-specific DNA rearrangement joining germline Variable (V), Diversity (D), and Joining (J) gene segments to generate >10^11 unique BCR and TCR specificities.",
            detailedMechanism = "RAG1/RAG2 recombinases cleave adjacent 12/23-bp Recombination Signal Sequences (RSS); non-homologous end joining (Artemis, DNA-PKcs) and TdT non-templated N-nucleotide addition create hypervariable CDR3 junctions.",
            clinicalRelevance = "Null RAG1/2 mutations cause T−B−NK+ Severe Combined Immunodeficiency (SCID) or Omenn syndrome.",
            diagramType = GlossaryDiagramType.VDJ_RECOMBINATION,
            defaultFocusedPart = "RAG1/RAG2 Synapsis",
            relatedTermIds = listOf("shm_aid", "tcr_signal1", "antibody_igg")
        ),
        GlossaryTerm(
            id = "shm_aid",
            term = "Somatic Hypermutation & AID",
            acronym = "SHM / AID",
            category = "Genetics & Receptors",
            shortDefinition = "Targeted ~10^-3/bp/generation point-mutation process in germinal center B-cell Ig V-regions driving affinity maturation.",
            detailedMechanism = "Activation-Induced Cytidine Deaminase (AID) deaminates single-stranded DNA cytosines to uracils during transcription; error-prone base excision (UNG) and mismatch repair (Polη) introduce diversity across CDR loops.",
            clinicalRelevance = "Higher-affinity B-cell clones capture scarce FDC antigen and win Tfh survival signals, raising antibody avidity 100–1000-fold.",
            diagramType = GlossaryDiagramType.GERMINAL_CENTER,
            defaultFocusedPart = "Dark Zone (AID SHM)",
            relatedTermIds = listOf("class_switch", "germinal_center", "vdj_recombination")
        ),
        GlossaryTerm(
            id = "class_switch",
            term = "Class-Switch Recombination",
            acronym = "CSR / IgM→IgG/IgA/IgE",
            category = "Genetics & Receptors",
            shortDefinition = "Deletional DNA recombination swapping heavy-chain constant (CH) exons from Cμ (IgM) to Cγ, Cα, or Cε while preserving antigen specificity.",
            detailedMechanism = "Driven by CD40L–CD40 engagement plus Tfh polarizing cytokines (IFN-γ → IgG, IL-4 → IgE, TGF-β → IgA) and AID targeting repetitive intronic Switch (S) regions.",
            clinicalRelevance = "CD40L (CD154) deficiency causes X-linked Hyper-IgM Syndrome with opportunistic Pneumocystis and Cryptosporidium infections.",
            diagramType = GlossaryDiagramType.GERMINAL_CENTER,
            defaultFocusedPart = "Tfh CD40L Selection",
            relatedTermIds = listOf("shm_aid", "germinal_center", "antibody_igg")
        ),
        GlossaryTerm(
            id = "germinal_center",
            term = "Germinal Center Reaction",
            acronym = "GC (DZ / LZ)",
            category = "Adaptive Immunity",
            shortDefinition = "Specialized microanatomical follicle inside secondary lymphoid organs where B cells undergo iterative cycles of proliferation, mutation, and Darwinian affinity selection.",
            detailedMechanism = "B cells express CXCR4 to proliferate and mutate in the Dark Zone (centroblasts), then downregulate CXCR4 and follow CXCL13 into the Light Zone to test mutated BCRs on FDC immune complexes and compete for Tfh CD40L/IL-21 help.",
            clinicalRelevance = "Produces long-lived bone-marrow plasma cells and high-affinity memory B cells essential for durable vaccine efficacy.",
            diagramType = GlossaryDiagramType.GERMINAL_CENTER,
            defaultFocusedPart = "Plasma / Memory B Output",
            relatedTermIds = listOf("shm_aid", "class_switch", "secondary_recall")
        ),
        GlossaryTerm(
            id = "jak_stat",
            term = "JAK–STAT Cytokine Signaling",
            acronym = "JAK / STAT",
            category = "Innate Immunity",
            shortDefinition = "Direct membrane-to-nucleus signaling relay used by >50 cytokines, interferons, and growth factors (IL-2, IL-6, IFN-γ, IFN-α/β, EPO).",
            detailedMechanism = "Cytokine-induced receptor dimerization brings receptor-associated Janus kinases (JAK1/2/3, TYK2) together for trans-phosphorylation, creating pTyr docking sites for STAT SH2 domains; phosphorylated STAT dimers translocate to DNA GAS/ISRE elements.",
            clinicalRelevance = "Small-molecule JAK inhibitors (tofacitinib, baricitinib, ruxolitinib) treat rheumatoid arthritis, alopecia areata, and severe COVID-19 hyperinflammation.",
            diagramType = GlossaryDiagramType.CYTOKINE_JAK_STAT,
            defaultFocusedPart = "STAT Dimerization",
            relatedTermIds = listOf("cytokine_storm", "nlrp3_pyroptosis", "tcr_signal1" /* handled gracefully */)
        ),
        GlossaryTerm(
            id = "nlrp3_pyroptosis",
            term = "NLRP3 Inflammasome & Pyroptosis",
            acronym = "NLRP3 / GSDMD",
            category = "Innate Immunity",
            shortDefinition = "Multiprotein cytosolic innate danger sensor that activates Caspase-1 to mature IL-1β/IL-18 and trigger lytic Gasdermin D pyroptosis.",
            detailedMechanism = "Requires priming (TLR/NF-κB upregulating NLRP3 and pro-IL-1β) plus activation (K+ efflux, MSU crystals, ATP, pore toxins) driving ASC prion filament assembly, Caspase-1 autocleavage, and N-terminal GSDMD pore insertion.",
            clinicalRelevance = "NLRP3 gain-of-function drives cryopyrin-associated periodic syndromes (CAPS) and gouty arthritis; targeted by IL-1 blockade (anakinra/canakinumab).",
            diagramType = GlossaryDiagramType.INFLAMMASOME_PYROPTOSIS,
            defaultFocusedPart = "GSDMD Membrane Pore",
            relatedTermIds = listOf("jak_stat", "complement_mac", "cytokine_storm")
        ),
        GlossaryTerm(
            id = "treg_tolerance",
            term = "Regulatory T Cells & Tolerance",
            acronym = "Treg / FOXP3",
            category = "Clinical & Tolerance",
            shortDefinition = "CD4+CD25hiFOXP3+ immunosuppressive T-cell lineage responsible for suppressing autoreactive lymphocytes and terminating excessive inflammation.",
            detailedMechanism = "Consumes local IL-2 via high-affinity CD25, secretes IL-10, TGF-β, and IL-35, hydrolyzes extracellular ATP via CD39/CD73 to adenosine, and strips CD80/86 from APCs via constitutive CTLA-4.",
            clinicalRelevance = "Loss-of-function FOXP3 mutations cause fatal IPEX syndrome; Treg stability limits autoimmune flares in our ODE tolerance models.",
            diagramType = GlossaryDiagramType.THREE_SIGNAL_ACTIVATION,
            defaultFocusedPart = "CTLA-4 / PD-1 Brake",
            relatedTermIds = listOf("costimulation_checkpoints", "mhc_class_ii", "cytokine_storm")
        ),
        GlossaryTerm(
            id = "secondary_recall",
            term = "Secondary Anamnestic Recall",
            acronym = "Memory B / Tcm",
            category = "Adaptive Immunity",
            shortDefinition = "Accelerated, higher-magnitude, affinity-matured immune response upon re-exposure to a previously encountered or vaccine-primed antigen.",
            detailedMechanism = "Pre-expanded class-switched memory B cells and Trm/Tcm subsets bypass naive repertoire frequency limits, differentiating into IgG-secreting plasmablasts within 2–4 days instead of 7–14 days.",
            clinicalRelevance = "Basis of prime-boost vaccination schedules and sterilizing neutralization upon subsequent pathogen challenge.",
            diagramType = GlossaryDiagramType.GERMINAL_CENTER,
            defaultFocusedPart = "Plasma / Memory B Output",
            relatedTermIds = listOf("germinal_center", "antibody_igg", "shm_aid")
        ),
        GlossaryTerm(
            id = "cross_presentation",
            term = "Cross-Presentation (cDC1)",
            acronym = "XCR1+ cDC1",
            category = "Innate Immunity",
            shortDefinition = "Specialized pathway allowing conventional type 1 dendritic cells (cDC1) to load phagocytosed exogenous viral/tumor antigens onto MHC Class I for CD8+ T-cell priming.",
            detailedMechanism = "Exogenous antigen escapes from Sec61/phagosomal pores into the cytosol, undergoes proteasomal cleavage, and enters ER/phagosomal MHC-I compartments via TAP1/2.",
            clinicalRelevance = "Essential for initiating CD8+ CTL immunity against viruses that do not directly infect dendritic cells and for cancer vaccine efficacy.",
            diagramType = GlossaryDiagramType.MHC_PRESENTATION,
            defaultFocusedPart = "CD8+ CTL Synapse",
            relatedTermIds = listOf("mhc_class_i", "mhc_class_ii", "tcr_signal1")
        ),
        GlossaryTerm(
            id = "cytokine_storm",
            term = "Cytokine Release Syndrome",
            acronym = "CRS / Hyperinflammation",
            category = "Clinical & Tolerance",
            shortDefinition = "Self-amplifying systemic inflammatory cascade driven by runaway macrophage/T-cell activation and high IL-6, IFN-γ, TNF, and IL-1β levels.",
            detailedMechanism = "Positive feedback loops between activated macrophages, endothelial cells, and T cells via JAK–STAT and NF-κB drive vascular permeability, fever, coagulopathy, and tissue injury.",
            clinicalRelevance = "Managed clinically using IL-6R blockade (tocilizumab), corticosteroids, or JAK inhibitors while monitoring ferritin and CRP.",
            diagramType = GlossaryDiagramType.CYTOKINE_JAK_STAT,
            defaultFocusedPart = "Nuclear ISRE/GAS Transcription",
            relatedTermIds = listOf("jak_stat", "nlrp3_pyroptosis", "treg_tolerance")
        )
    )

    fun filterTerms(query: String, category: String): List<GlossaryTerm> {
        val q = query.trim().lowercase()
        return TERMS.filter { t ->
            val catOk = category == "All" || t.category.equals(category, ignoreCase = true)
            val queryOk = q.isEmpty() ||
                t.term.lowercase().contains(q) ||
                t.acronym.lowercase().contains(q) ||
                t.shortDefinition.lowercase().contains(q) ||
                t.detailedMechanism.lowercase().contains(q) ||
                t.clinicalRelevance.lowercase().contains(q)
            catOk && queryOk
        }
    }

    fun byId(id: String): GlossaryTerm? = TERMS.firstOrNull { it.id == id }
}
