package com.example.data.kb

data class RefSource(
    val id: String,
    val citation: String,
    val url: String
)

data class OrganInfo(
    val id: String,
    val name: String,
    val kind: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val radius: Float,
    val colorHex: Long,
    val summary: String,
    val functionText: String,
    val clinicalNote: String
)

data class CellInfo(
    val id: String,
    val name: String,
    val group: String,
    val colorHex: Long,
    val morphology: String,
    val development: String,
    val location: String,
    val markers: List<String>,
    val receptors: List<String>,
    val secreted: List<String>,
    val activation: String,
    val clinical: String,
    val levels: Map<String, String>,
    val refIds: List<String>
)

data class InteractionInfo(
    val id: String,
    val fromName: String,
    val toName: String,
    val fromColorHex: Long,
    val toColorHex: Long,
    val mechanism: String,
    val outcome: String
)

data class OrganelleInfo(
    val id: String,
    val name: String,
    val role: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val radius: Float,
    val colorHex: Long,
    val detail: String
)

data class CellProcessInfo(
    val id: String,
    val name: String,
    val steps: List<String>,
    val summary: String
)

data class MoleculeInfo(
    val id: String,
    val name: String,
    val category: String,
    val pdbId: String?,
    val uniprotId: String?,
    val status: String,
    val summary: String,
    val domains: String,
    val ligandSite: String
)

data class PathwayNode(
    val id: String,
    val label: String,
    val x: Float,
    val y: Float,
    val role: String
)

data class PathwayEdge(
    val from: String,
    val to: String,
    val action: String = "activates"
)

data class PathwayInfo(
    val id: String,
    val name: String,
    val summary: String,
    val nodes: List<PathwayNode>,
    val edges: List<PathwayEdge>,
    val outcome: String
)

data class DailyDiscovery(
    val title: String,
    val status: String,
    val body: String,
    val refId: String
)

object ImmunologyKb {
    val REFS = mapOf(
        "abbas" to RefSource("abbas", "Abbas AK, Lichtman AH, Pillai S. Cellular and Molecular Immunology (10th ed.). Elsevier.", "https://www.elsevier.com"),
        "janeway" to RefSource("janeway", "Murphy K, Weaver C. Janeway's Immunobiology (10th ed.). W. W. Norton.", "https://wwnorton.com"),
        "kuby" to RefSource("kuby", "Punt J, Stranford S, Jones P, Owen JA. Kuby Immunology (8th ed.). Macmillan.", "https://www.macmillanlearning.com"),
        "alberts" to RefSource("alberts", "Alberts B et al. Molecular Biology of the Cell (7th ed.). W. W. Norton.", "https://www.ncbi.nlm.nih.gov/books/NBK21054/"),
        "lehninger" to RefSource("lehninger", "Nelson DL, Cox MM. Lehninger Principles of Biochemistry (8th ed.). Macmillan.", "https://www.macmillanlearning.com"),
        "carroll" to RefSource("carroll", "Carroll KC et al. Jawetz, Melnick & Adelberg's Medical Microbiology (28th ed.). McGraw Hill.", "https://accessmedicine.mhmedical.com"),
        "rcsb" to RefSource("rcsb", "Berman HM et al. The Protein Data Bank (RCSB PDB).", "https://www.rcsb.org"),
        "alphafold" to RefSource("alphafold", "Jumper J et al. AlphaFold Protein Structure Database.", "https://alphafold.ebi.ac.uk"),
        "epmc" to RefSource("epmc", "Europe PMC Open Biomedical Literature API.", "https://europepmc.org"),
        "hiv" to RefSource("hiv", "NIH/NIAID HIV Immunology & Cure Research Overview.", "https://www.niaid.nih.gov/diseases-conditions/hivaids")
    )

    val EXPLANATION_LEVELS = listOf("Beginner", "Undergraduate", "Advanced", "Research")

    val DAILY = listOf(
        DailyDiscovery(
            title = "V(D)J Recombination Generates Antigen Receptor Diversity",
            status = "Established",
            body = "RAG1/RAG2 recombinases introduce DNA double-strand breaks at recombination signal sequences flanking V, D, and J gene segments, creating >10^11 potential TCR and BCR specificities before thymic and marrow selection.",
            refId = "janeway"
        ),
        DailyDiscovery(
            title = "Cross-Presentation via MHC Class I in cDC1",
            status = "Established",
            body = "Conventional type 1 dendritic cells (XCR1+ CLEC9A+) route exogenous antigens from endosomal compartments into the cytosolic proteasome–TAP pathway to prime naive CD8+ cytotoxic T lymphocytes against viruses and tumors.",
            refId = "abbas"
        ),
        DailyDiscovery(
            title = "NLRP3 Inflammasome Two-Signal Activation",
            status = "Established",
            body = "Signal 1 (NF-κB priming via TLR/IL-1R) induces NLRP3 and pro-IL-1β expression; Signal 2 (K+ efflux, lysosomal rupture, or mtROS) triggers ASC speck assembly and caspase-1 cleavage of gasdermin D and IL-1β.",
            refId = "abbas"
        ),
        DailyDiscovery(
            title = "cGAS–STING Cytosolic DNA Sensing",
            status = "Established",
            body = "Cyclic GMP-AMP synthase binds cytosolic double-stranded DNA to synthesize 2'3'-cGAMP, activating ER-resident STING, TBK1, and IRF3 to drive type I interferon gene programs.",
            refId = "janeway"
        ),
        DailyDiscovery(
            title = "Germinal Center Affinity Maturation",
            status = "Established",
            body = "Cyclic migration between dark-zone centroblast proliferation/AID somatic hypermutation and light-zone follicular dendritic cell/Tfh competition selects high-affinity B-cell clones.",
            refId = "kuby"
        )
    )

    val ORGANS = listOf(
        OrganInfo("marrow", "Bone Marrow", "Primary Lymphoid", -0.2f, -0.35f, 0.1f, 0.22f, 0xFF2EE6C5, "Hematopoietic stem cell niche & B-cell maturation.", "Produces all blood lineages from HSCs; completes early B-cell central tolerance via receptor editing and clonal deletion.", "Bone marrow failure, leukemia, or myeloablation depletes innate and adaptive precursors."),
        OrganInfo("thymus", "Thymus", "Primary Lymphoid", 0.0f, 0.48f, 0.22f, 0.19f, 0xFF22E5FF, "T-lymphocyte selection and MHC restriction.", "Medullary thymic epithelial cells express AIRE to display tissue-restricted antigens; positive selection ensures MHC binding and negative selection removes autoreactive clones.", "DiGeorge syndrome (22q11.2 deletion) causes thymic hypoplasia and severe T-cell deficiency."),
        OrganInfo("spleen", "Spleen", "Secondary Lymphoid", 0.52f, 0.12f, -0.1f, 0.24f, 0xFF8B6BFF, "Blood-borne antigen surveillance & marginal zone defense.", "White pulp (PALS T zones and B follicles) filters blood-borne pathogens; red pulp clears senescent RBCs and opsonized bacteria.", "Asplenia markedly increases risk of encapsulated bacterial sepsis (S. pneumoniae, H. influenzae, N. meningitidis)."),
        OrganInfo("ln_cervical", "Cervical Lymph Nodes", "Secondary Lymphoid", -0.18f, 0.72f, 0.08f, 0.13f, 0xFF3DDC97, "Drains head, oral, and upper airway lymphatics.", "Captures soluble antigens and migratory CCR7+ dendritic cells in subcapsular sinus and paracortex.", "Reactive cervical lymphadenopathy commonly accompanies upper respiratory viral or streptococcal infections."),
        OrganInfo("ln_axillary", "Axillary Lymph Nodes", "Secondary Lymphoid", -0.58f, 0.28f, 0.05f, 0.13f, 0xFF3DDC97, "Drains upper extremity and thoracic lymphatics.", "Site of primary adaptive priming after intramuscular deltoid vaccination.", "Clinically evaluated during breast and upper-limb staging and post-vaccination germinal center responses."),
        OrganInfo("ln_inguinal", "Inguinal Lymph Nodes", "Secondary Lymphoid", 0.34f, -0.56f, 0.12f, 0.13f, 0xFF3DDC97, "Drains lower extremities and pelvic lymphatics.", "Organized into B-cell cortical follicles, T-cell paracortex (HEVs), and plasma-cell medullary cords.", "Sentinel node evaluation tracks regional infection or metastatic spread."),
        OrganInfo("tonsils", "Tonsils (Waldeyer Ring)", "Mucosal / Secondary", 0.08f, 0.80f, 0.20f, 0.12f, 0xFFFFC857, "Pharyngeal mucosal lymphoid sentinels.", "Specialized tonsillar crypt epithelium samples inhaled and ingested antigens directly into germinal centers.", "Recurrent tonsillitis reflects chronic bacterial biofilm or viral antigenic stimulation."),
        OrganInfo("galt", "GALT (Mucosal Tissue)", "Mucosal", -0.36f, -0.05f, 0.28f, 0.17f, 0xFFE04FB0, "Gut-associated secretory IgA immune network.", "Balances tolerance to commensal microbiota and dietary antigens with rapid defense against enteric pathogens.", "Loss of mucosal Treg balance contributes to inflammatory bowel disease."),
        OrganInfo("peyers", "Peyer's Patches", "Mucosal", 0.05f, -0.18f, 0.32f, 0.14f, 0xFFE04FB0, "Ileal lymphoid follicles with M-cell domes.", "Microfold (M) cells transcytose luminal microbes to subepithelial dendritic cells and IgA-committed B cells.", "Exploited as an entry route by Salmonella enterica and poliovirus.")
    )

    val CELLS = listOf(
        CellInfo(
            id = "cd4", name = "CD4+ T Helper Cell", group = "Adaptive", colorHex = 0xFF22E5FF,
            morphology = "Small round lymphocyte (7–10 µm), high nuclear-to-cytoplasmic ratio, dense heterochromatin.",
            development = "Bone marrow CLP -> Thymus DN -> DP -> CD4+ SP selected on MHC Class II.",
            location = "Blood, lymph node paracortex (T-cell zone), spleen PALS, inflamed tissues.",
            markers = listOf("CD3+", "CD4+", "TCRαβ", "CD28"),
            receptors = listOf("TCR", "CD4 co-receptor", "CD28", "CXCR4/CCR5", "CTLA-4 / PD-1 (induced)"),
            secreted = listOf("IL-2", "IFN-γ (Th1)", "IL-4/IL-5/IL-13 (Th2)", "IL-17 (Th17)", "IL-21 (Tfh)"),
            activation = "Signal 1 (TCR–peptide:MHC II) + Signal 2 (CD28–CD80/86) + Signal 3 (polarizing cytokines).",
            clinical = "Primary target depleted by HIV-1; master orchestrator of B-cell class switching and macrophage activation.",
            levels = mapOf(
                "Beginner" to "The conductor of the adaptive immune system. It reads danger messages and tells B cells and killer cells what to do.",
                "Undergraduate" to "Recognizes exogenous peptide on MHC II via TCR/CD4; differentiates into Th1, Th2, Th17, or Tfh subsets based on cytokine milieu.",
                "Advanced" to "Polarization driven by lineage transcription factors T-bet (Th1), GATA3 (Th2), RORγt (Th17), Bcl6 (Tfh), integrating STAT1/4/6/3 signaling.",
                "Research" to "Epigenetic plasticity across Th17/Treg and Tfh/Th1 axes modulates germinal center kinetics, checkpoint efficacy, and HIV reservoir persistence."
            ),
            refIds = listOf("abbas", "janeway")
        ),
        CellInfo(
            id = "cd8", name = "CD8+ Cytotoxic T Cell", group = "Adaptive", colorHex = 0xFF8B6BFF,
            morphology = "Lymphocyte; activated CTLs enlarge and accumulate electron-dense lytic granules.",
            development = "Thymic positive selection on MHC Class I; exits as naive CD8+ T cell.",
            location = "Lymph nodes, blood, virally infected tissues, tumor microenvironment.",
            markers = listOf("CD3+", "CD8+", "TCRαβ", "CD28"),
            receptors = listOf("TCR", "CD8αβ", "CD28", "FasL (effector)", "PD-1 (exhaustion)"),
            secreted = listOf("Perforin", "Granzymes A/B", "IFN-γ", "TNF-α"),
            activation = "Cross-priming by cDC1 via MHC I + CD80/86 + CD4+ T-cell IL-2/CD40 licensing.",
            clinical = "Clears virus-infected cells and tumor neoantigens; PD-1 blockade reinvigorates exhausted CD8+ TILs.",
            levels = mapOf(
                "Beginner" to "Trained assassin cell that destroys body cells hijacked by viruses or turned cancerous.",
                "Undergraduate" to "Binds endogenous 8–10 aa peptides on MHC I; polarizes MTOC to immunological synapse to release perforin/granzyme B.",
                "Advanced" to "Granzyme B cleaves procaspase-3 and Bid directly, bypassing Bcl-2 blocks alongside FasL–Fas death-inducing signaling complex.",
                "Research" to "TCF1+ stem-like progenitor exhausted (Tpex) CD8+ T cells sustain the proliferative burst following anti-PD-1/PD-L1 therapy."
            ),
            refIds = listOf("abbas", "kuby")
        ),
        CellInfo(
            id = "treg", name = "Regulatory T Cell (Treg)", group = "Adaptive", colorHex = 0xFF2EE6C5,
            morphology = "Small lymphocyte phenotypically similar to helper T cells; high surface CD25.",
            development = "Thymic tTreg selection on high-affinity self-pMHC II, or peripheral TGF-β pTreg induction.",
            location = "Secondary lymphoid organs, mucosa, placenta, sites of resolving inflammation.",
            markers = listOf("CD4+", "CD25hi", "FOXP3+", "CD127lo"),
            receptors = listOf("TCR", "CTLA-4 (constitutive)", "IL-2Rα (CD25)", "TIGIT"),
            secreted = listOf("IL-10", "TGF-β", "IL-35"),
            activation = "TCR recognition of self or commensal peptides sustained by paracrine IL-2.",
            clinical = "FOXP3 loss-of-function causes IPEX syndrome (fatal multiorgan autoimmunity); excess tumor Tregs suppress immunity.",
            levels = mapOf(
                "Beginner" to "Peacekeeper cell that calms immune responses so healthy tissues are not attacked.",
                "Undergraduate" to "Maintains peripheral tolerance via CTLA-4 trans-endocytosis of CD80/86, IL-2 consumption, and IL-10/TGF-β.",
                "Advanced" to "FOXP3 forms complexes with NFAT and Runx1 to repress Il2 and enforce suppressive chromatin architecture.",
                "Research" to "TSDR demethylation stabilizes CNS2 enhancer occupancy in thymic Tregs under inflammatory cytokine stress."
            ),
            refIds = listOf("abbas", "janeway")
        ),
        CellInfo(
            id = "bcell", name = "B Lymphocyte", group = "Adaptive", colorHex = 0xFF3DDC97,
            morphology = "Small resting lymphocyte with abundant MHC II and surface IgM/IgD BCR.",
            development = "Bone marrow pro-B -> pre-B -> immature IgM+ B cell -> spleen transitional -> mature follicular/MZ.",
            location = "Lymph node primary follicles, spleen white pulp, bone marrow, blood.",
            markers = listOf("CD19+", "CD20+", "CD21+", "MHC II+"),
            receptors = listOf("Surface IgM/IgD (BCR)", "CD40", "TLR4/7/9", "BAFF-R"),
            secreted = listOf("IgM (early)", "Antigen-presenting cytokines (IL-6, IL-10)"),
            activation = "BCR crosslinking by native antigen + Tfh CD40L/IL-21 (T-dependent) or multivalent TLR/polysaccharides.",
            clinical = "Anti-CD20 (rituximab) depletes pathogenic B cells in lymphoma, RA, and MS.",
            levels = mapOf(
                "Beginner" to "Antibody-making factory cell in training; recognizes shapes on intact microbes.",
                "Undergraduate" to "Internalizes BCR-bound antigen to present peptides on MHC II to Tfh cells, triggering germinal center entry.",
                "Advanced" to "BCR ITAMs (Igα/Igβ) recruit Lyn and Syk, activating BLNK, BTK, PLCγ2, and NF-κB/NFAT.",
                "Research" to "Ephrin-B1 and S1PR2 constrain germinal center B-cell localization while AID-driven CSR/SHM undergoes affinity selection."
            ),
            refIds = listOf("janeway", "kuby")
        ),
        CellInfo(
            id = "plasma", name = "Plasma Cell", group = "Adaptive", colorHex = 0xFF00C2FF,
            morphology = "Eccentric 'clock-face' nucleus, perinuclear Golgi clear zone (hof), vast concentric rough ER.",
            development = "Terminal differentiation of germinal center or extrafollicular B cells driven by Blimp-1/IRF4/XBP1.",
            location = "Lymph node medullary cords, bone marrow survival niches (long-lived), lamina propria.",
            markers = listOf("CD138+", "CD38hi", "CD27+", "CD19lo/CD20-"),
            receptors = listOf("BCMA", "TACI", "CXCR4 (marrow homing)"),
            secreted = listOf("IgG", "IgA", "IgM", "IgE (thousands of molecules/sec)"),
            activation = "Terminally differentiated; long-lived survival requires APRIL/BAFF/IL-6 in marrow stromal niches.",
            clinical = "Malignant clonal expansion produces multiple myeloma; CAR-T therapies target BCMA.",
            levels = mapOf(
                "Beginner" to "Dedicated antibody cannon secreting thousands of pathogen-neutralizing antibodies per second.",
                "Undergraduate" to "Downregulates BCR and MHC II while expanding rough ER via XBP1 unfolded-protein response.",
                "Advanced" to "BLIMP-1 (PRDM1) represses PAX5 and BCL6, locking the secretory gene expression program.",
                "Research" to "CXCL12-CXCR4 retention in hypoxic bone marrow niches supports decades-long humoral serological memory."
            ),
            refIds = listOf("abbas", "kuby")
        ),
        CellInfo(
            id = "nk", name = "Natural Killer (NK) Cell", group = "Innate", colorHex = 0xFFE04FB0,
            morphology = "Large granular lymphocyte with azurophilic perforin/granzyme granules.",
            development = "Bone marrow common lymphoid progenitor -> IL-15-dependent NK maturation.",
            location = "Peripheral blood (~10% of lymphocytes), liver, spleen, decidua, infected tissues.",
            markers = listOf("CD56+", "CD16+", "CD3-", "NKp46+"),
            receptors = listOf("KIR / NKG2A (inhibitory MHC I)", "NKG2D / NKp30 (activating)", "CD16 (FcγRIIIa)"),
            secreted = listOf("IFN-γ", "TNF-α", "Perforin", "Granzymes"),
            activation = "'Missing-self' (loss of MHC Class I) + stress ligands (MICA/B) or IgG-coated targets (ADCC via CD16).",
            clinical = "Essential early antiviral defense (especially herpesviruses) and mediates therapeutic antibody ADCC.",
            levels = mapOf(
                "Beginner" to "Rapid-response patroller that destroys stressed or infected cells trying to hide their ID badges (MHC I).",
                "Undergraduate" to "Balances ITIM inhibitory signals (MHC I binding) against ITAM/DAP10 activating receptors.",
                "Advanced" to "SHP-1/SHP-2 recruitment by inhibitory KIRs dephosphorylates Vav1, blocking lytic synapse polarization unless overridden.",
                "Research" to "Cytokine-induced memory-like NK cells (IL-12/15/18) and NKG2C+ adaptive NK subsets respond robustly to CMV."
            ),
            refIds = listOf("abbas", "janeway")
        ),
        CellInfo(
            id = "macrophage", name = "Macrophage", group = "Innate", colorHex = 0xFFFFC857,
            morphology = "Large phagocyte (15–25 µm) with ruffled pseudopodia, abundant lysosomes and phagosomes.",
            development = "Embryonic yolk-sac/fetal-liver tissue-resident niches OR bone marrow circulating monocytes.",
            location = "All tissues (Kupffer cells, alveolar macrophages, microglia, splenic macrophages).",
            markers = listOf("CD68+", "CD14+", "MHC II+", "F4/80 (mouse)"),
            receptors = listOf("TLRs", "Scavenger receptors", "Mannose receptor", "Complement CR3", "FcγR"),
            secreted = listOf("IL-1β", "TNF-α", "IL-6", "IL-12", "IL-10", "ROS / NO"),
            activation = "Classical M1 (IFN-γ + LPS) for microbicidal killing; Alternative M2 (IL-4/IL-13) for tissue repair.",
            clinical = "Central to granuloma formation in TB, atherosclerosis foam cells, and cytokine release syndromes.",
            levels = mapOf(
                "Beginner" to "Big eater cell that engulfs germs and debris while calling other immune cells to help.",
                "Undergraduate" to "Generates phagolysosomal ROS (NADPH oxidase) and NO (iNOS) while presenting antigens on MHC II.",
                "Advanced" to "Metabolic reprogramming shifts M1 macrophages toward aerobic glycolysis/broken TCA cycle (succinate-HIF1α), versus M2 fatty-acid oxidation.",
                "Research" to "Ontogeny (self-renewing embryonic macrophages vs CCR2+ monocyte-derived) governs fibrosis and niche homeostasis."
            ),
            refIds = listOf("abbas", "janeway")
        ),
        CellInfo(
            id = "monocyte", name = "Monocyte", group = "Innate", colorHex = 0xFFFF9F43,
            morphology = "Large circulating leukocyte (12–20 µm) with characteristic kidney/horseshoe-shaped nucleus.",
            development = "Bone marrow HSC -> CMP -> GMP -> MDP -> cMoP -> blood.",
            location = "Peripheral blood (~4–8% of WBCs), splenic reservoir; extravasates via CCL2/CCR2.",
            markers = listOf("CD14+", "CD16+/-", "CCR2+"),
            receptors = listOf("CCR2", "CX3CR1", "TLRs", "FcγRs"),
            secreted = listOf("TNF-α", "IL-1β", "IL-6", "CCL2"),
            activation = "Recruited along endothelial selectin/integrin cascades to inflamed tissue where they become macrophages or moDCs.",
            clinical = "Subsets (classical CD14++CD16-, intermediate, non-classical patrolling) track cardiovascular and sepsis states.",
            levels = mapOf(
                "Beginner" to "Circulating blood reserve cell that travels to infected tissues and transforms into a macrophage.",
                "Undergraduate" to "Classical monocytes emigrate via CCR2 toward CCL2 gradients; non-classical patrol endothelium via CX3CR1.",
                "Advanced" to "Trained immunity induces H3K4me3/H3K27ac epigenetic rewiring in myeloid progenitors for heightened secondary cytokine output.",
                "Research" to "Single-cell trajectories reveal distinct inflammatory versus angiogenic monocyte fates in tissue injury."
            ),
            refIds = listOf("kuby", "abbas")
        ),
        CellInfo(
            id = "neutrophil", name = "Neutrophil", group = "Innate", colorHex = 0xFF70A1FF,
            morphology = "Polymorphonuclear granulocyte with 3–5 nuclear lobes and pink/neutral cytoplasmic granules.",
            development = "Bone marrow myeloblast -> promyelocyte -> myelocyte -> band -> segmented neutrophil (G-CSF driven).",
            location = "Most abundant blood WBC (50–70%); first responder to acute inflammation/pus.",
            markers = listOf("CD15+", "CD16+", "CD66b+"),
            receptors = listOf("CXCR1/CXCR2 (IL-8)", "fMLPR", "C5aR", "FcγR", "TLR2/4"),
            secreted = listOf("Myeloperoxidase (HOCl)", "Elastase", "Lactoferrin", "NETs (DNA webs)"),
            activation = "Chemotaxis to CXCL8/C5a -> rolling/integrin arrest -> diapedesis -> phagocytosis, degranulation, NETosis.",
            clinical = "Neutropenia causes severe bacterial/fungal infection risk; NADPH oxidase defect causes CGD.",
            levels = mapOf(
                "Beginner" to "Fastest-arriving infantry cell that swarms bacteria, bursts with antimicrobial enzymes, and casts DNA nets.",
                "Undergraduate" to "Executes respiratory burst (NADPH oxidase + MPO producing HOCl) and short-lived apoptosis cleared by efferocytosis.",
                "Advanced" to "PAD4-mediated histone citrullination decondenses chromatin during vital or suicidal NETosis.",
                "Research" to "Circadian CXCL2/CXCR4 aging rhythms regulate margination and bone marrow clearance."
            ),
            refIds = listOf("abbas", "janeway")
        ),
        CellInfo(
            id = "eosinophil", name = "Eosinophil", group = "Innate", colorHex = 0xFFFF6B81,
            morphology = "Bilobed nucleus with bright eosinophilic (red-orange) crystalloid granules.",
            development = "Bone marrow myeloid progenitor driven by IL-5, IL-3, and GM-CSF.",
            location = "GI lamina propria, respiratory tract, blood (1–4%), helminth/allergic sites.",
            markers = listOf("Siglec-8+", "CCR3+", "IL-5Rα+"),
            receptors = listOf("IL-5R", "CCR3 (eotaxin)", "FcεRI / FcγRII"),
            secreted = listOf("Major Basic Protein (MBP)", "Eosinophil Cationic Protein", "Eosinophil Peroxidase", "LTC4"),
            activation = "Eotaxin (CCL11) recruitment + IL-5 survival + IgE/IgG crosslinking against large helminths.",
            clinical = "Drives tissue damage in eosinophilic asthma and allergic esophagitis; anti-IL-5 (mepolizumab) treats severe asthma.",
            levels = mapOf(
                "Beginner" to "Specialist granulocyte that attacks parasitic worms and participates in allergic airway inflammation.",
                "Undergraduate" to "Releases toxic cationic proteins (MBP, ECP) onto helminth cuticles too large for phagocytosis.",
                "Advanced" to "Siglec-8 engagement triggers caspase-dependent apoptosis as a therapeutic checkpoint in eosinophilic disorders.",
                "Research" to "Homeostatic mucosal eosinophils support IgA class switching and adipose beige-fat remodeling."
            ),
            refIds = listOf("kuby", "abbas")
        ),
        CellInfo(
            id = "basophil", name = "Basophil", group = "Innate", colorHex = 0xFFA29BFE,
            morphology = "Bilobed/obscured nucleus packed with deep purple metachromatic granules (<1% of blood WBCs).",
            development = "Bone marrow granulocyte-basophil lineage supported by IL-3.",
            location = "Circulating blood; recruited into tissues during late-phase allergic reactions.",
            markers = listOf("FcεRI+", "CD123+ (IL-3Rα)", "CD203c+"),
            receptors = listOf("High-affinity FcεRI", "IL-3R", "IL-33R (ST2)", "C3aR/C5aR"),
            secreted = listOf("Histamine", "IL-4", "IL-13", "Leukotriene C4"),
            activation = "Crosslinking of IgE bound to FcεRI or innate alarmins (IL-33, TSLP).",
            clinical = "Basophil activation test (BAT, CD63/CD203c upregulation) evaluates IgE-mediated allergy; amplifies Th2 immunity.",
            levels = mapOf(
                "Beginner" to "Rare blood alarm cell that releases histamine and kickstarts allergic type-2 responses.",
                "Undergraduate" to "Major early source of IL-4 that biases naive CD4+ T cells toward Th2 differentiation.",
                "Advanced" to "FcεRI β/γ ITAM phosphorylation via Lyn/Syk triggers rapid calcium flux and degranulation.",
                "Research" to "Cooperate with ILC2s in protease-allergen sensing and helminth expulsion."
            ),
            refIds = listOf("janeway", "kuby")
        ),
        CellInfo(
            id = "dc", name = "Dendritic Cell", group = "Innate / Bridge", colorHex = 0xFF00E5A3,
            morphology = "Stellate morphology with long motile dendrites/veils maximizing surface area for T-cell scanning.",
            development = "Bone marrow CDP -> pre-DC -> tissue cDC1/cDC2; plus pDCs (type I IFN specialists).",
            location = "Skin (Langerhans), mucosae, blood; migrates via lymphatics to lymph node T-cell zones.",
            markers = listOf("CD11c+", "MHC IIhi", "CD80/86+", "CCR7+ (mature)"),
            receptors = listOf("TLRs", "C-type lectins (DC-SIGN)", "NODs", "cGAS/STING", "CCR7"),
            secreted = listOf("IL-12", "IFN-α/β (pDC)", "IL-23", "IL-6", "IL-10"),
            activation = "PRR sensing halts endocytosis, upregulates MHC II, CD80/86, and CCR7 to home to lymph nodes.",
            clinical = "Most potent activator of naive T cells; central to mRNA/adjuvant vaccines and DC cancer vaccines.",
            levels = mapOf(
                "Beginner" to "Master scout that captures pathogen fragments in tissues and carries the alert to lymph node T cells.",
                "Undergraduate" to "Bridges innate and adaptive immunity by delivering Signal 1 (pMHC), Signal 2 (B7), and Signal 3 (cytokines).",
                "Advanced" to "cDC1 (Batf3/IRF8) specialize in cross-presentation to CD8+ T cells; cDC2 (IRF4) bias CD4+ helper priming.",
                "Research" to "mregDC states upregulate PD-L1 and CCR7 in tumors, gating checkpoint-blockade responsiveness."
            ),
            refIds = listOf("abbas", "janeway")
        ),
        CellInfo(
            id = "mast", name = "Mast Cell", group = "Innate", colorHex = 0xFFFF5D73,
            morphology = "Tissue-resident mononuclear cell densely packed with heparin/histamine granules.",
            development = "Bone marrow CD34+ mast cell progenitors mature locally in tissues via SCF–KIT (CD117).",
            location = "Skin, mucosal subepithelia, perivascular spaces (not mature in peripheral blood).",
            markers = listOf("CD117+ (c-Kit)", "FcεRI+", "Tryptase+"),
            receptors = listOf("FcεRI", "c-Kit", "MRGPRX2", "C3aR/C5aR", "TLRs"),
            secreted = listOf("Histamine", "Tryptase", "Chymase", "PGD2", "LTC4", "TNF-α"),
            activation = "Multivalent allergen bridging FcεRI-bound IgE, anaphylatoxins, or neuropeptides.",
            clinical = "Mediates anaphylaxis, urticaria, and allergic rhinitis; stabilized by omalizumab (anti-IgE) and epinephrine rescue.",
            levels = mapOf(
                "Beginner" to "Tissue sentinel stationed near blood vessels and airways that triggers fast swelling and mucus defense.",
                "Undergraduate" to "Immediate degranulation releases preformed histamine/tryptase within seconds, followed by lipid mediators and cytokines.",
                "Advanced" to "Sustained KIT signaling maintains tissue survival; MRGPRX2 mediates non-IgE pseudo-allergic degranulation.",
                "Research" to "Neuro-immune mast cell crosstalk modulates itch, visceral pain, and barrier remodeling."
            ),
            refIds = listOf("abbas", "kuby")
        ),
        CellInfo(
            id = "memb", name = "Memory B Cell", group = "Adaptive", colorHex = 0xFF48DBFB,
            morphology = "Quiescent class-switched (IgG/IgA/IgE) or IgM+ lymphocyte with somatically hypermutated BCR.",
            development = "Exits germinal center light zone under lower-affinity/Bach2 transcriptional programming.",
            location = "Secondary lymphoid marginal/subcapsular zones, bone marrow, mucosal tissues, blood.",
            markers = listOf("CD19+", "CD20+", "CD27+", "Class-switched Ig"),
            receptors = listOf("High-affinity BCR", "CD40", "TLR7/9"),
            secreted = listOf("Rapidly differentiates into high-affinity IgG/IgA plasma cells upon re-exposure"),
            activation = "Lower activation threshold than naive B cells; Tfh-assisted or accelerated TLR/BCR recall.",
            clinical = "Provides durable humoral recall even when serum antibody titers wane; adapts to viral variants.",
            levels = mapOf(
                "Beginner" to "Veteran antibody cell that remembers a past germ or vaccine and multiplies at top speed if it returns.",
                "Undergraduate" to "Carries affinity-matured IgG/IgA BCRs and generates faster, higher-titer secondary antibody responses.",
                "Advanced" to "IgG cytoplasmic tail ITT tyrosine motif amplifies Syk/Grb2 signaling versus naive IgM tails.",
                "Research" to "Broadly cross-reactive memory B clones expand germinal center breadth upon heterologous booster immunization."
            ),
            refIds = listOf("janeway", "abbas")
        ),
        CellInfo(
            id = "memt", name = "Memory T Cell (Tcm/Tem/Trm)", group = "Adaptive", colorHex = 0xFF00D2D3,
            morphology = "Antigen-experienced lymphocyte persisting long-term in survival niches.",
            development = "Survives post-effector contraction phase (~5–10% of peak T-cell burst) via IL-7 and IL-15.",
            location = "Tcm (lymph nodes, CCR7+), Tem (blood/spleen), Trm (skin, lung, gut epithelia, CD69+CD103+).",
            markers = listOf("CD45RO+", "CD127+ (IL-7Rα)", "CD62L/CCR7 (Tcm)", "CD69/CD103 (Trm)"),
            receptors = listOf("TCR", "IL-7R", "IL-15Rβ", "CD28"),
            secreted = listOf("Rapid IFN-γ, TNF-α, IL-2 within hours of cognate restimulation"),
            activation = "Less dependent on CD28 costimulation; poised enhancers at effector cytokine loci.",
            clinical = "Foundation of long-lived cellular immunity after vaccination and CAR-T persistence.",
            levels = mapOf(
                "Beginner" to "Long-lived patrol veteran stationed either in lymph nodes or directly inside tissues to stop repeat infections fast.",
                "Undergraduate" to "Subdivided into CCR7+ Central Memory (proliferative reserve) and Tissue-Resident Memory (instant frontline defense).",
                "Advanced" to "Relies on mitochondrial oxidative phosphorylation and spare respiratory capacity rather than Warburg glycolysis.",
                "Research" to "Hobit and Blimp-1 coordinate tissue retention programs in non-recirculating CD8+ Trm populations."
            ),
            refIds = listOf("abbas", "janeway")
        )
    )

    val INTERACTIONS = listOf(
        InteractionInfo("dc_cd4", "Dendritic Cell", "CD4+ T Helper", 0xFF00E5A3, 0xFF22E5FF, "MHC II–Peptide + CD80/86 + IL-12", "Primes naive T cells into polarized Th1/Tfh effectors."),
        InteractionInfo("tfh_b", "Tfh (CD4+)", "B Lymphocyte", 0xFF22E5FF, 0xFF3DDC97, "CD40L–CD40 + IL-21 / IL-4", "Drives germinal center reaction, somatic hypermutation, and class switching."),
        InteractionInfo("cd8_target", "CD8+ CTL", "Infected Host Cell", 0xFF8B6BFF, 0xFFFF5D73, "TCR–MHC I + Perforin / Granzyme B", "Induces selective caspase-mediated apoptosis in virus-infected or tumor cells."),
        InteractionInfo("th1_mac", "Th1 Cell", "Macrophage", 0xFF22E5FF, 0xFFFFC857, "IFN-γ + CD40L", "Classical M1 activation boosting ROS, iNOS, and phagolysosomal killing."),
        InteractionInfo("nk_target", "NK Cell", "MHC-I-Low Target", 0xFFE04FB0, 0xFFFF5D73, "Missing-self loss + NKG2D", "Eliminates escaped virus-infected or malignant cells lacking MHC Class I."),
        InteractionInfo("treg_teff", "Treg (FOXP3+)", "Effector T Cell", 0xFF2EE6C5, 0xFF22E5FF, "CTLA-4 + IL-10 + TGF-β", "Restrains excessive inflammation and enforces peripheral self-tolerance.")
    )

    val ORGANELLES = listOf(
        OrganelleInfo("membrane", "Plasma Membrane", "Selective phospholipid bilayer & receptors", 0f, 0f, 0f, 0.92f, 0xFF22E5FF, "Fluid mosaic lipid bilayer housing TCR/BCR, MHC, cytokine receptors, and lipid rafts for immunological synapse formation."),
        OrganelleInfo("cytoplasm", "Cytoplasm / Cytosol", "Signal transduction & metabolic matrix", 0.45f, -0.35f, 0.2f, 0.24f, 0xFF48DBFB, "Site of glycolysis, proteasomal antigen processing, cGAS-STING sensing, and kinase cascades."),
        OrganelleInfo("nucleus", "Nucleus", "Genome, V(D)J & transcription control", 0f, 0.05f, 0f, 0.32f, 0xFF8B6BFF, "Houses chromatin, NF-κB/NFAT/AP-1/STAT transcriptional hubs, and AID-driven somatic hypermutation."),
        OrganelleInfo("mito", "Mitochondria", "ATP bioenergetics, mtROS & apoptosis", -0.45f, 0.22f, 0.15f, 0.16f, 0xFFFFC857, "Drives OXPHOS in memory/Treg cells, glycolysis-linked ROS in effector cells, and cytochrome c release in apoptosis."),
        OrganelleInfo("ribosome", "Ribosomes", "mRNA translation into immune proteins", 0.28f, 0.35f, 0.22f, 0.11f, 0xFF3DDC97, "80S complexes translating cytokine, chemokine, perforin, and immunoglobulin transcripts."),
        OrganelleInfo("er", "Rough Endoplasmic Reticulum", "Protein folding, MHC assembly & Ig synthesis", -0.36f, -0.25f, -0.15f, 0.20f, 0xFF2EE6C5, "Loads peptides onto MHC Class I via TAP/tapasin; vastly expanded in antibody-secreting plasma cells."),
        OrganelleInfo("golgi", "Golgi Apparatus", "Glycosylation & vesicular sorting", 0.36f, -0.12f, -0.20f, 0.17f, 0xFFE04FB0, "Modifies N-glycans on Fc domains and sorts lytic granzymes/cytokines into secretory vesicles."),
        OrganelleInfo("lysosome", "Lysosomes & Phagolysosomes", "Acid hydrolase digestion & MHC II loading", -0.18f, -0.52f, 0.28f, 0.13f, 0xFFFF5D73, "Fuses with phagosomes (pH ~4.5) to degrade microbes and process peptides onto MHC Class II."),
        OrganelleInfo("peroxisome", "Peroxisomes & Cytoskeleton", "ROS detoxification & synapse polarization", 0.52f, 0.32f, -0.15f, 0.13f, 0xFF70A1FF, "Microtubule MTOC reorients Golgi and lytic granules directly toward the target cell synapse; peroxisomes balance H2O2.")
    )

    val PROCESSES = listOf(
        CellProcessInfo("endocytosis", "Endocytosis", listOf("Receptor/clathrin coat pits form", "Membrane invaginates & dynamin pinches", "Early endosome acidifies & sorts cargo"), "Internalizes soluble antigens, BCR-captured proteins, and surface receptors."),
        CellProcessInfo("exocytosis", "Exocytosis", listOf("Cytokine/granzyme vesicles bud from trans-Golgi", "Kinesin/MTOC transports vesicles to synapse", "SNARE-mediated membrane fusion releases cargo"), "Secretes antibodies, cytokines, histamine, and cytotoxic perforin/granzymes."),
        CellProcessInfo("phagocytosis", "Phagocytosis", listOf("Opsonin/PRR recognition triggers actin cup", "Pseudopodia engulf microbe into phagosome", "Phagosome fuses with lysosome + ROS burst"), "Core innate clearance of bacteria, fungi, and apoptotic debris."),
        CellProcessInfo("apoptosis", "Apoptosis (Programmed Cell Death)", listOf("Intrinsic cytochrome c / Extrinsic FasL activation", "Caspase-9/8 cleave executioner Caspase-3", "Chromatin condenses, blebbing, clean efferocytosis"), "Non-inflammatory programmed suicide preventing autoimmunity and viral spread."),
        CellProcessInfo("necrosis", "Necrosis / Pyroptosis", listOf("Acute osmotic or gasdermin-D pore stress", "Organelle swelling & plasma membrane rupture", "DAMPs (HMGB1, ATP, IL-1β) trigger inflammation"), "Lytic cell death alerting neighboring immune sentinels via DAMPs."),
        CellProcessInfo("autophagy", "Autophagy", listOf("ULK1/Beclin-1 nucleates double-membrane phagophore", "LC3-II captures damaged organelles or cytosol", "Autophagosome fuses with lysosome for recycling"), "Supports T-cell survival, cytosolic pathogen clearance, and MHC II presentation of endogenous antigens."),
        CellProcessInfo("division", "Cell Division (Mitosis / Clonal Expansion)", listOf("G1/S DNA replication & centrosome duplication", "Mitotic spindle aligns & separates sister chromatids", "Cytokinesis yields exponential antigen-specific clones"), "Expands a single reactive lymphocyte clone >1,000-fold within days."),
        CellProcessInfo("synthesis", "Protein Synthesis", listOf("Transcription factor activation yields spliced mRNA", "Ribosomes translate nascent polypeptide into ER", "Folding, disulfide bridging & Golgi glycosylation"), "Powers massive antibody secretion in plasma cells and cytokine bursts."),
        CellProcessInfo("mito_fn", "Mitochondrial Bioenergetics", listOf("Substrate oxidation drives ETC proton gradient", "ATP synthase powers motility & ion pumps", "Controlled mtROS/cytochrome c regulates fate"), "Metabolic switch distinguishes short-lived effectors from long-lived memory cells."),
        CellProcessInfo("diff", "Cellular Differentiation", listOf("Lineage cytokines activate STAT/Notch signaling", "Master transcription factors remodel chromatin", "Stable effector or memory identity emerges"), "Directs HSCs and naive lymphocytes into specialized functional lineages.")
    )

    val MOLS = listOf(
        MoleculeInfo("dna", "DNA Double Helix", "Nucleic Acid", "1BNA", null, "Established", "B-form antiparallel right-handed double helix storing heritable V(D)J gene segments and immune loci.", "Major/minor grooves, sugar-phosphate backbone, Watson–Crick A=T and G≡C pairs.", "Sequence-specific transcription factor recognition (NF-κB, STAT, AP-1)."),
        MoleculeInfo("mrna", "Messenger RNA (mRNA)", "Nucleic Acid", "1FIR", null, "Established", "Single-stranded transcript with 5' cap, UTRs, coding sequence, and poly(A) tail; basis of mRNA vaccines.", "Hairpin stems, pseudouridine-stabilized codons.", "Ribosome decoding center & TLR7/8 sensing (unmodified ssRNA)."),
        MoleculeInfo("trna", "Transfer RNA (tRNA)", "Nucleic Acid", "1EHZ", null, "Established", "L-shaped cloverleaf adaptor matching mRNA codons to amino acids during translation.", "Acceptor stem (CCA-3'), D/TΨC loops, anticodon triplet.", "Aminoacyl-tRNA synthetase & ribosomal A/P/E sites."),
        MoleculeInfo("rrna", "Ribosomal RNA (rRNA)", "Nucleic Acid", "4V6X", null, "Established", "Catalytic ribozyme core of the 80S/70S ribosome driving peptide bond formation.", "Peptidyl transferase center & decoding helix 44.", "Target of bacterial macrolides and aminoglycosides (70S selectivity)."),
        MoleculeInfo("igg", "Immunoglobulin G (IgG)", "Antibody", "1IGT", "P01857", "Established", "Y-shaped ~150 kDa heterotetramer (2 heavy γ, 2 light κ/λ chains) dominating serum humoral defense.", "2 Fab arms (VH/VL + CH1/CL), flexible hinge, Fc (CH2/CH3).", "Complementarity-determining regions (CDR1-3) bind antigen; Fc binds FcγR & C1q."),
        MoleculeInfo("igm", "Immunoglobulin M (IgM) Fab/Fc Core", "Antibody", "2RCJ", "P01871", "Established", "First antibody class secreted in primary responses; pentameric assembly maximizes avidity and C1q activation.", "Cµ1–Cµ4 domains, J-chain polymerization.", "High-avidity polyvalent microbe agglutination."),
        MoleculeInfo("tcr_pmhc", "TCR–pMHC Complex", "Receptor Complex", "1AO7", "P01850", "Established", "Immunological synapse recognition unit coupling αβ TCR variable loops to peptide-loaded MHC.", "TCR α/β V+C domains docking diagonally over MHC α1/α2 helices.", "Peptide anchor residues + CDR3 specificity pockets."),
        MoleculeInfo("mhci", "MHC Class I (HLA-A2)", "Antigen Presentation", "3HLA", "P04439", "Established", "Ubiquitous nucleated-cell display platform presenting 8–10 aa endogenous peptides to CD8+ T cells.", "Heavy α1/α2 peptide cleft + α3 Ig-fold + non-covalent β2-microglobulin.", "Closed-ends peptide groove (P2 & P9 anchors); CD8 binds α3."),
        MoleculeInfo("mhcii", "MHC Class II (HLA-DR1)", "Antigen Presentation", "1DLH", "P01903", "Established", "Professional APC heterodimer (α + β) presenting 13–25 aa exogenous endosomal peptides to CD4+ T cells.", "Open-ended α1/β1 cleft + α2/β2 stalks.", "Invariant chain (Ii/CLIP) displaced by HLA-DM."),
        MoleculeInfo("il2", "Interleukin-2 (IL-2)", "Cytokine", "1M47", "P60568", "Established", "4-α-helix bundle T-cell growth cytokine driving clonal expansion and Treg homeostasis.", "Four-helix bundle (A–D).", "Binds trimeric IL-2Rα/β/γc (CD25/CD122/CD132) triggering JAK1/JAK3–STAT5."),
        MoleculeInfo("ifng", "Interferon-gamma (IFN-γ)", "Cytokine", "1FG9", "P01579", "Established", "Antiparallel homodimeric Type II interferon secreted by Th1, NK, and CD8+ T cells.", "Interlocked 6-helix homodimer.", "Engages IFNGR1/IFNGR2 dimers -> JAK1/JAK2–STAT1 macrophage activation."),
        MoleculeInfo("tlr4", "TLR4–MD-2–LPS Complex", "Innate Receptor", "3FXI", "O00206", "Established", "Pattern recognition dimer detecting bacterial lipopolysaccharide via MD-2 hydrophobic pocket.", "Horseshoe leucine-rich repeat (LRR) ectodomain + MD-2 co-receptor.", "Acyl chains of lipid A wedge into MD-2, inducing TLR4 M-shaped signaling dimer."),
        MoleculeInfo("c3", "Complement Component C3", "Complement", "2A73", "P01024", "Established", "Central 185 kDa complement hub cleaved into anaphylatoxin C3a and covalent opsonin C3b.", "13 domains including MG1–8, ANA (C3a), and reactive thioester TED.", "Internal thioester bonds covalently to microbial hydroxyl/amine surfaces."),
        MoleculeInfo("gp120", "HIV-1 Envelope gp120 + CD4 Complex", "Viral Protein", "1GC1", "P04578", "Established", "Surface viral glycoprotein engaging host CD4 and CCR5/CXCR4 coreceptors; shielded by dense N-glycans.", "Inner/outer domains, bridging sheet, V1–V5 variable loops.", "CD4 Phe43 cavity + CD4bs broadly neutralizing antibody target."),
        MoleculeInfo("cd4_mol", "Human CD4 Ectodomain", "Co-receptor", "1CDJ", "P01730", "Established", "Monomeric 4-Ig-like domain T-helper co-receptor stabilizing TCR–MHC II contact and Lck delivery.", "D1–D4 extracellular Ig-superfamily domains.", "D1 binds HIV gp120 and MHC Class II β2 domain; cytoplasmic CXCP recruits Lck.")
    )

    val PATHWAYS = listOf(
        PathwayInfo("jak_stat", "JAK–STAT Signalling", "Rapid cytokine receptor to nucleus transcriptional cascade.", listOf(
            PathwayNode("cyt", "Cytokine (IL-2/IFN)", 0.12f, 0.25f, "Ligand"),
            PathwayNode("rec", "Cytokine Receptor", 0.32f, 0.25f, "Receptor"),
            PathwayNode("jak", "JAK1/2/3 Tyrosine Kinase", 0.52f, 0.25f, "Kinase"),
            PathwayNode("stat", "STAT Dimerization", 0.72f, 0.45f, "Transcrip."),
            PathwayNode("socs", "SOCS Feedback Brake", 0.52f, 0.72f, "Inhibitor"),
            PathwayNode("gene", "ISGs / Cytokine Genes", 0.88f, 0.72f, "Nucleus")
        ), listOf(
            PathwayEdge("cyt", "rec"), PathwayEdge("rec", "jak"), PathwayEdge("jak", "stat"),
            PathwayEdge("stat", "gene"), PathwayEdge("stat", "socs"), PathwayEdge("socs", "jak", "inhibits")
        ), "Drives antiviral ISGs, T-cell proliferation, and Th lineage polarization."),
        PathwayInfo("nfkb", "NF-κB Inflammatory Signalling", "Canonical PRR/TNF transcription factor activation.", listOf(
            PathwayNode("tlr", "TLR4 / TNFR", 0.14f, 0.3f, "Receptor"),
            PathwayNode("myd88", "MyD88 / TRAF6", 0.36f, 0.3f, "Adaptor"),
            PathwayNode("ikk", "IKKα/β/γ (NEMO)", 0.58f, 0.3f, "Kinase"),
            PathwayNode("ikb", "IκBα Degradation", 0.58f, 0.65f, "Switch"),
            PathwayNode("rel", "p50/p65 Nuclear Entry", 0.84f, 0.5f, "Nucleus")
        ), listOf(
            PathwayEdge("tlr", "myd88"), PathwayEdge("myd88", "ikk"),
            PathwayEdge("ikk", "ikb"), PathwayEdge("ikb", "rel")
        ), "Transcribes TNF-α, IL-1β, IL-6, chemokines, and pro-survival genes."),
        PathwayInfo("mapk", "MAPK / ERK Signalling", "Three-tier kinase amplifier downstream of Ras.", listOf(
            PathwayNode("rtk", "TCR / RTK", 0.14f, 0.45f, "Receptor"),
            PathwayNode("ras", "Ras-GTP", 0.32f, 0.45f, "GTPase"),
            PathwayNode("raf", "RAF (MAP3K)", 0.50f, 0.28f, "Kinase"),
            PathwayNode("mek", "MEK1/2 (MAP2K)", 0.68f, 0.28f, "Kinase"),
            PathwayNode("erk", "ERK1/2 -> AP-1", 0.86f, 0.55f, "Nucleus")
        ), listOf(
            PathwayEdge("rtk", "ras"), PathwayEdge("ras", "raf"), PathwayEdge("raf", "mek"), PathwayEdge("mek", "erk")
        ), "Activates Fos/Jun (AP-1) for T-cell proliferation and survival."),
        PathwayInfo("pi3k", "PI3K–AKT–mTOR Axis", "Metabolic growth, nutrient sensing, and survival pathway.", listOf(
            PathwayNode("cd28", "CD28 / BCR / InsulinR", 0.14f, 0.45f, "Receptor"),
            PathwayNode("pi3k_n", "PI3K (PIP2 -> PIP3)", 0.34f, 0.45f, "Lipid Kinase"),
            PathwayNode("pten", "PTEN Phosphatase", 0.34f, 0.78f, "Brake"),
            PathwayNode("akt", "AKT (PKB)", 0.58f, 0.45f, "Kinase"),
            PathwayNode("mtor", "mTORC1 -> Anabolism", 0.84f, 0.45f, "Effector")
        ), listOf(
            PathwayEdge("cd28", "pi3k_n"), PathwayEdge("pi3k_n", "akt"),
            PathwayEdge("pten", "pi3k_n", "inhibits"), PathwayEdge("akt", "mtor")
        ), "Shifts activated T cells to aerobic glycolysis and blocks apoptosis."),
        PathwayInfo("tcr_sig", "T-Cell Receptor (TCR) Signalling", "ITAM-ZAP70 cascade coupling antigen recognition to NFAT/NF-κB.", listOf(
            PathwayNode("pmhc", "pMHC + TCR/CD4", 0.12f, 0.4f, "Synapse"),
            PathwayNode("lck", "Lck -> CD3 ITAMs", 0.32f, 0.4f, "Src Kinase"),
            PathwayNode("zap70", "ZAP-70 -> LAT/SLP76", 0.52f, 0.4f, "Scaffold"),
            PathwayNode("plcg", "PLCγ1 -> IP3 / DAG", 0.70f, 0.25f, "2nd Msg"),
            PathwayNode("nfat", "Ca2+ / Calcineurin / NFAT", 0.86f, 0.65f, "Nucleus")
        ), listOf(
            PathwayEdge("pmhc", "lck"), PathwayEdge("lck", "zap70"), PathwayEdge("zap70", "plcg"), PathwayEdge("plcg", "nfat")
        ), "Requires Signal 1 + CD28 Signal 2 to avoid anergy and induce IL-2."),
        PathwayInfo("bcr_sig", "B-Cell Receptor (BCR) Signalling", "Lyn/Syk/BTK activation triggering humoral differentiation.", listOf(
            PathwayNode("ag", "Native Antigen + BCR", 0.14f, 0.45f, "Surface"),
            PathwayNode("syk", "Lyn & Syk -> Igα/Igβ", 0.35f, 0.45f, "Kinase"),
            PathwayNode("btk", "BLNK & BTK", 0.56f, 0.45f, "Kinase"),
            PathwayNode("plcg2", "PLCγ2 -> Ca2+ / PKCβ", 0.74f, 0.30f, "2nd Msg"),
            PathwayNode("b_out", "NF-κB / Survival / MHC II", 0.88f, 0.68f, "Effector")
        ), listOf(
            PathwayEdge("ag", "syk"), PathwayEdge("syk", "btk"), PathwayEdge("btk", "plcg2"), PathwayEdge("plcg2", "b_out")
        ), "Amplified by CD19/CD21 complement co-receptor; inhibited clinically by ibrutinib (BTK)."),
        PathwayInfo("complement", "Complement Activation", "Classical, Lectin & Alternative proteolytic cascades converging on C3.", listOf(
            PathwayNode("c1q", "C1q / MBL / Factor B", 0.14f, 0.35f, "Initiator"),
            PathwayNode("c3conv", "C3 Convertase", 0.38f, 0.35f, "Enzyme"),
            PathwayNode("c3b", "C3b Opsonin + C3a", 0.60f, 0.25f, "Hub"),
            PathwayNode("c5", "C5a + C5b", 0.74f, 0.58f, "Cleavage"),
            PathwayNode("mac", "C5b-9 (MAC Pore)", 0.88f, 0.35f, "Lysis")
        ), listOf(
            PathwayEdge("c1q", "c3conv"), PathwayEdge("c3conv", "c3b"), PathwayEdge("c3b", "c5"), PathwayEdge("c5", "mac")
        ), "Produces anaphylatoxins (C3a/C5a), opsonization (C3b), and Gram-negative lysis (MAC)."),
        PathwayInfo("ag_pres", "Antigen Presentation (MHC I & II)", "Dual intracellular cytosolic vs endolysosomal peptide loading.", listOf(
            PathwayNode("prot", "Proteasome / Endosome", 0.15f, 0.45f, "Cleavage"),
            PathwayNode("tap", "TAP / HLA-DM Loader", 0.40f, 0.45f, "Transport"),
            PathwayNode("mhc", "pMHC I or pMHC II", 0.64f, 0.45f, "Assembly"),
            PathwayNode("cd8_4", "CD8+ or CD4+ T Cell", 0.86f, 0.45f, "Synapse")
        ), listOf(
            PathwayEdge("prot", "tap"), PathwayEdge("tap", "mhc"), PathwayEdge("mhc", "cd8_4")
        ), "Ensures both intracellular viruses (MHC I) and phagocytosed microbes (MHC II) alert T cells."),
        PathwayInfo("apoptosis_p", "Apoptosis Pathways", "Intrinsic mitochondrial & extrinsic death-receptor caspase execution.", listOf(
            PathwayNode("fas", "FasL / BH3 Stress", 0.14f, 0.42f, "Trigger"),
            PathwayNode("bcl2", "BAX/BAK (vs BCL-2)", 0.36f, 0.62f, "Mito Pore"),
            PathwayNode("cytc", "Cytochrome c -> Apaf-1", 0.58f, 0.62f, "Apoptosome"),
            PathwayNode("casp8", "Caspase-8 / Caspase-9", 0.58f, 0.25f, "Initiator"),
            PathwayNode("casp3", "Caspase-3 -> DNA Ladder", 0.86f, 0.42f, "Executioner")
        ), listOf(
            PathwayEdge("fas", "casp8"), PathwayEdge("fas", "bcl2"), PathwayEdge("bcl2", "cytc"),
            PathwayEdge("cytc", "casp3"), PathwayEdge("casp8", "casp3")
        ), "Maintains immune contraction post-infection without necrotic inflammation."),
        PathwayInfo("ifn_sig", "Interferon Antiviral Signalling", "PRR -> IRF3/7 -> IFN-α/β autocrine/paracrine antiviral state.", listOf(
            PathwayNode("prr", "TLR3/7/RIG-I/cGAS", 0.14f, 0.40f, "Sensor"),
            PathwayNode("tbk1", "MAVS/STING -> TBK1", 0.36f, 0.40f, "Kinase"),
            PathwayNode("irf3", "IRF3/IRF7 -> IFN-α/β", 0.56f, 0.40f, "Inducer"),
            PathwayNode("ifnar", "IFNAR1/2 -> ISGF3", 0.74f, 0.65f, "Receptor"),
            PathwayNode("isg", "PKR, OAS1, MxA (ISGs)", 0.88f, 0.35f, "Antiviral")
        ), listOf(
            PathwayEdge("prr", "tbk1"), PathwayEdge("tbk1", "irf3"), PathwayEdge("irf3", "ifnar"), PathwayEdge("ifnar", "isg")
        ), "Halts viral translation, degrades viral RNA, and boosts MHC I expression."),
        PathwayInfo("inflammasome", "Inflammasome Activation (NLRP3)", "Two-signal cytosolic danger sensor driving IL-1β/IL-18 & pyroptosis.", listOf(
            PathwayNode("sig1", "Signal 1: NF-κB Priming", 0.14f, 0.25f, "Priming"),
            PathwayNode("sig2", "Signal 2: K+ Efflux / ROS", 0.14f, 0.68f, "Trigger"),
            PathwayNode("nlrp3", "NLRP3 + ASC Speck", 0.46f, 0.45f, "Platform"),
            PathwayNode("casp1", "Active Caspase-1", 0.68f, 0.45f, "Protease"),
            PathwayNode("gsdmd", "IL-1β/18 + GSDMD Pore", 0.88f, 0.45f, "Pyroptosis")
        ), listOf(
            PathwayEdge("sig1", "nlrp3"), PathwayEdge("sig2", "nlrp3"), PathwayEdge("nlrp3", "casp1"), PathwayEdge("casp1", "gsdmd")
        ), "Recruits neutrophils rapidly via IL-1β; dysregulated in gout and cryopyrinopathies.")
    )
}
