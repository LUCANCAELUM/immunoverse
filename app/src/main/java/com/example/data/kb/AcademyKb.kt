package com.example.data.kb

data class AcademyLesson(
    val id: String,
    val title: String,
    val level: String,
    val summary: String,
    val keyPoints: List<String>
)

data class AcademyTrack(
    val id: String,
    val title: String,
    val category: String,
    val lessons: List<AcademyLesson>
)

data class QuizQuestion(
    val id: String,
    val trackId: String,
    val difficulty: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class ClinicalCase(
    val id: String,
    val title: String,
    val difficulty: String,
    val vignette: String,
    val labFindings: List<String>,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val synthesis: String
)

object AcademyKb {
    val TRACKS = listOf(
        AcademyTrack("intro_imm", "Introduction to Immunology", "Core", listOf(
            AcademyLesson("i1", "Innate vs Adaptive Barriers", "Beginner", "Immediate germline-encoded defense vs antigen-specific clonal memory.", listOf("Physical skin/mucosa & defensins act immediately.", "PRRs (TLRs, NODs, cGAS) recognize conserved PAMPs/DAMPs.", "Adaptive T and B lymphocytes rearrange RAG1/2 receptor genes.")),
            AcademyLesson("i2", "Primary & Secondary Lymphoid Organs", "Beginner", "Where lymphocytes originate, educate, and encounter antigen.", listOf("Bone marrow & thymus educate self-tolerant B and T cells.", "Spleen filters blood; lymph nodes sample tissue lymph.", "MALT/Peyer's patches coordinate mucosal secretory IgA."))
        )),
        AcademyTrack("adv_imm", "Advanced Immunology", "Immunology", listOf(
            AcademyLesson("a1", "V(D)J Recombination & Selection", "Advanced", "Somatic diversification and central tolerance checkpoints.", listOf("RAG1/2 cut RSS flanking V, D, J segments; TdT adds N-nucleotides.", "Positive selection ensures self-MHC restriction in thymic cortex.", "Negative selection (AIRE) deletes high-affinity autoreactive clones.")),
            AcademyLesson("a2", "Germinal Center Affinity Maturation", "Advanced", "AID-driven somatic hypermutation and Tfh competition.", listOf("Centroblasts mutate IgV regions in the dark zone.", "Light-zone FDCs display intact antigen for BCR uptake.", "Tfh CD40L + IL-21 rescue the highest-affinity B clones."))
        )),
        AcademyTrack("mol_bio", "Molecular Biology", "Molecular", listOf(
            AcademyLesson("m1", "Central Dogma & mRNA Regulation", "Undergraduate", "Transcription, splicing, ribosome decoding, and epigenetics.", listOf("Promoters and enhancers integrate NF-κB, STAT, and NFAT.", "Alternative splicing generates membrane vs secreted IgM.", "miRNAs and histone acetylation fine-tune cytokine half-life.")),
            AcademyLesson("m2", "CRISPR-Cas9 & Nucleic Acid Tools", "Undergraduate", "RNA-guided nucleases and PCR amplification principles.", listOf("SpCas9 recognizes 5'-NGG-3' PAM adjacent to 20-nt protospacer.", "RT-qPCR quantifies relative transcripts via 2^-ΔΔCt.", "Sanger dideoxy chain termination resolves base calls."))
        )),
        AcademyTrack("virology", "Virology", "Infectious", listOf(
            AcademyLesson("v1", "Viral Entry & Replication Strategies", "Undergraduate", "Baltimore classification and host receptor tropism.", listOf("Class I–VII genomes dictate polymerase requirements.", "Retroviruses reverse-transcribe RNA into integrated provirus.", "Interferons induce ISGs (PKR, OAS1, MxA) blocking translation.")),
            AcademyLesson("v2", "Immune Evasion & Latency", "Advanced", "How herpesviruses, HIV, and respiratory RNA viruses persist.", listOf("MHC I downregulation triggers 'missing-self' NK sensing.", "Antigenic drift alters HA/RBD surface epitopes.", "Latent proviral reservoirs silence viral gene transcription."))
        )),
        AcademyTrack("microbiology", "Microbiology", "Infectious", listOf(
            AcademyLesson("mc1", "Bacterial Envelope & Gram Staining", "Beginner", "Peptidoglycan architecture dictates stain retention.", listOf("Gram-positive thick peptidoglycan traps crystal violet–iodine.", "Gram-negative outer membrane houses LPS (endotoxin) and porins.", "MacConkey agar differentiates lactose fermenters.")),
            AcademyLesson("mc2", "Antimicrobial Mechanisms & Stewardship", "Undergraduate", "Selective targets and resistance mechanisms.", listOf("Beta-lactams inhibit PBP transpeptidase cell-wall crosslinking.", "Beta-lactamases, efflux pumps, and target mutations drive resistance.", "Kirby–Bauer disc diffusion illustrates growth inhibition zones."))
        )),
        AcademyTrack("hematology", "Hematology", "Clinical", listOf(
            AcademyLesson("h1", "Hematopoiesis & Leukocyte Lineage", "Undergraduate", "Multipotent HSC branching into myeloid and lymphoid arms.", listOf("Neutrophils, eosinophils, basophils, monocytes arise from GMP.", "B, T, and NK lymphocytes arise from CLP.", "CBC differentials track left-shift neutrophilia vs lymphocytosis.")),
            AcademyLesson("h2", "Blood Groups & Transfusion Reactions", "Undergraduate", "ABO carbohydrate antigens and IgM complement hemolysis.", listOf("Natural anti-A/anti-B IgM antibodies activate classical complement.", "Rh(D) IgG crosses the placenta; RhoGAM prevents sensitization.", "Type II cytotoxic crossmatching prevents hemolytic crisis."))
        )),
        AcademyTrack("pathology", "Pathology & Hypersensitivity", "Clinical", listOf(
            AcademyLesson("p1", "Gell & Coombs Hypersensitivity I–IV", "Undergraduate", "Four mechanistic classes of immune-mediated tissue injury.", listOf("Type I: IgE + mast cell/basophil histamine (anaphylaxis).", "Type II: IgG/IgM vs cell-surface antigens (AIHA, Goodpasture).", "Type III: Circulating immune complexes (SLE, serum sickness).", "Type IV: T-cell delayed hypersensitivity (TB skin test, contact dermatitis).")),
            AcademyLesson("p2", "Granulomatous & Septic Inflammation", "Advanced", "Persistent antigen containment vs dysregulated shock.", listOf("Epithelioid macrophages fuse into Langhans giant cells.", "Caseating granulomas reflect intense mycobacterial DTH.", "Sepsis balances hyperinflammatory shock and late immunoparalysis."))
        )),
        AcademyTrack("cell_bio", "Cellular Biology", "Cellular", listOf(
            AcademyLesson("cb1", "Vesicular Trafficking & Organelles", "Beginner", "Coordinated ER–Golgi–lysosome secretory pipelines.", listOf("COPII moves ER cargo to Golgi; clathrin coats endosomes.", "Mannose-6-phosphate tags hydrolases for lysosomes.", "Plasma cells expand rough ER exponentially for Ig output.")),
            AcademyLesson("cb2", "Immunometabolism & Cell Death", "Advanced", "Bioenergetic wiring of effector vs memory lymphocytes.", listOf("mTORC1 drives Warburg glycolysis in effector T cells.", "AMPK and FAO sustain long-lived Trm and Tregs.", "Apoptosis preserves membrane integrity; pyroptosis releases IL-1β."))
        )),
        AcademyTrack("clin_imm", "Clinical Immunology", "Clinical", listOf(
            AcademyLesson("ci1", "Vaccinology & Correlates of Protection", "Undergraduate", "Live attenuated, subunit, conjugate, and mRNA platforms.", listOf("Adjuvants engage PRRs to supply DC Signal 2/3.", "T-dependent polysaccharide–protein conjugates trigger memory B cells.", "Herd immunity threshold depends on R0 and transmission block.")),
            AcademyLesson("ci2", "Monoclonals, Checkpoint & CAR-T", "Advanced", "Targeted immunotherapy engineering.", listOf("Anti-PD-1/CTLA-4 release T-cell inhibitory brakes.", "Rituximab depletes CD20+ B cells via ADCC and CDC.", "CAR-T links scFv antigen binding directly to CD3ζ + CD28/4-1BB."))
        )),
        AcademyTrack("bioinfo", "Bioinformatics & Genomics", "Computational", listOf(
            AcademyLesson("bi1", "Sequence Alignment & Phylogenetics", "Undergraduate", "Scoring matrices, Needleman–Wunsch, and evolutionary trees.", listOf("Global alignment (NW) aligns end-to-end; local (SW) finds conserved motifs.", "Jukes–Cantor corrects multiple substitutions at the same site.", "UPGMA builds ultrametric clustering trees from pairwise distances.")),
            AcademyLesson("bi2", "Structural Biology & Protein Fold", "Undergraduate", "Reading PDB coordinates and AlphaFold pLDDT confidence.", listOf("C-α coordinates trace secondary helices and β-sheets.", "B-factor / pLDDT reflects local structural order or flexibility.", "HETATM records capture bound ligands, cofactors, and glycans."))
        ))
    )

    val QUIZ_BANK = listOf(
        // Intro Immunology
        QuizQuestion("q1", "intro_imm", "Beginner", "Which organ is the primary site of T-lymphocyte positive and negative selection?", listOf("Spleen", "Thymus", "Lymph node", "Tonsil"), 1, "Pre-T cells migrate from bone marrow to the thymus for MHC restriction and central tolerance."),
        QuizQuestion("q2", "intro_imm", "Beginner", "Which cell type serves as the fastest first-responder phagocyte in acute bacterial pus?", listOf("Neutrophil", "Plasma cell", "Basophil", "Memory B cell"), 0, "Neutrophils constitute 50–70% of blood leukocytes and arrive within hours via CXCL8."),
        QuizQuestion("q3", "intro_imm", "Beginner", "Which antibody class is transported across mucosal epithelia as a secretory dimer?", listOf("IgE", "IgD", "IgA", "IgM"), 2, "Dimeric IgA binds pIgR to protect respiratory and intestinal mucosal surfaces."),
        QuizQuestion("q4", "intro_imm", "Undergraduate", "Which receptor allows dendritic cells to recognize bacterial LPS?", listOf("TLR4–MD-2", "TLR9", "CD16", "FcεRI"), 0, "TLR4 paired with MD-2 senses Gram-negative lipopolysaccharide."),
        QuizQuestion("q5", "intro_imm", "Undergraduate", "Which secondary lymphoid structure samples luminal gut antigens via M cells?", listOf("Thymic cortex", "Peyer's patches", "Splenic red pulp", "Bone marrow"), 1, "M cells in Peyer's patch follicle-associated epithelium transcytose microbes to dendritic cells."),
        QuizQuestion("q6", "intro_imm", "Undergraduate", "What are the three classic signals required to fully activate a naive T cell?", listOf("MHC-TCR, CD28-B7, and polarizing cytokines", "Complement, IgE, and histamine", "Perforin, granzyme, and FasL", "BCR, BAFF, and APRIL"), 0, "Signal 1 (antigen specificity), Signal 2 (costimulation), and Signal 3 (cytokine lineage instruction)."),
        // Advanced Immunology
        QuizQuestion("q7", "adv_imm", "Advanced", "Mutations in AIRE cause APS-1 autoimmunity because medullary thymic epithelial cells fail to:", listOf("Express tissue-restricted self-antigens for negative selection", "Produce IL-7 for pro-B survival", "Secrete C1q", "Activate RAG2 in bone marrow"), 0, "AIRE drives ectopic transcription of peripheral self-proteins to delete autoreactive thymocytes."),
        QuizQuestion("q8", "adv_imm", "Advanced", "Which enzyme initiates both somatic hypermutation and class-switch recombination in germinal center B cells?", listOf("RAG1", "AID (Activation-Induced Cytidine Deaminase)", "TdT", "BLIMP-1"), 1, "AID deaminates cytosines to uracils in Ig variable and switch regions."),
        QuizQuestion("q9", "adv_imm", "Advanced", "Which transcription factor acts as the master regulator of CD4+CD25+ regulatory T cells?", listOf("T-bet", "GATA3", "FOXP3", "RORγt"), 2, "Loss of FOXP3 causes severe multiorgan autoimmunity (IPEX syndrome)."),
        QuizQuestion("q10", "adv_imm", "Advanced", "Cross-presentation of exogenous viral antigens onto MHC Class I is most specialized in:", listOf("cDC1 (XCR1+ CLEC9A+)", "Neutrophils", "Basophils", "Plasma cells"), 0, "cDC1 export endocytosed antigen into the proteasome–TAP pathway to prime CD8+ T cells."),
        QuizQuestion("q11", "adv_imm", "Undergraduate", "NK cells release inhibitory brakes when target cells display normal levels of:", listOf("MHC Class I", "FasL", "CD80", "C3b"), 0, "KIR and NKG2A/CD94 receptors sense healthy self MHC Class I to restrain killing."),
        QuizQuestion("q12", "adv_imm", "Advanced", "In the TCR signaling cascade, which kinase directly phosphorylates LAT and SLP-76?", listOf("JAK3", "ZAP-70", "BTK", "IRAK4"), 1, "Lck phosphorylates CD3 ITAMs, recruiting and activating ZAP-70."),
        // Molecular Biology
        QuizQuestion("q13", "mol_bio", "Beginner", "During transcription, RNA polymerase synthesizes RNA in which direction?", listOf("3' to 5'", "5' to 3'", "N-terminus to C-terminus", "Both directions simultaneously"), 1, "Incoming nucleoside triphosphates add onto the free 3'-OH group."),
        QuizQuestion("q14", "mol_bio", "Undergraduate", "What PAM sequence is required by wild-type Streptococcus pyogenes Cas9?", listOf("5'-NGG-3'", "5'-TTTN-3'", "5'-AUG-3'", "5'-GAATTC-3'"), 0, "SpCas9 binds 5'-NGG-3' immediately downstream of the 20-nt spacer target."),
        QuizQuestion("q15", "mol_bio", "Undergraduate", "In RT-qPCR ΔΔCt analysis, lowering Ct by 3 cycles at 100% efficiency corresponds to:", listOf("3-fold higher template", "8-fold higher template", "3-fold lower template", "No change"), 1, "2^3 = 8-fold higher starting cDNA abundance."),
        QuizQuestion("q16", "mol_bio", "Beginner", "Which nucleic acid base is found in RNA but replaced by thymine in DNA?", listOf("Cytosine", "Guanine", "Uracil", "Adenine"), 2, "RNA incorporates uracil pairing with adenine."),
        QuizQuestion("q17", "mol_bio", "Advanced", "Which molecular chaperone pair loads endogenous peptides onto MHC Class I in the ER lumen?", listOf("HLA-DM and invariant chain", "TAP1/2 and tapasin", "Calcineurin and NFAT", "Sec61 and clathrin"), 1, "TAP imports cytosolic proteasomal peptides and tapasin bridges MHC I to the peptide-loading complex."),
        QuizQuestion("q18", "mol_bio", "Undergraduate", "Restriction enzyme EcoRI cuts 5'-G^AATTC-3' leaving:", listOf("Blunt ends", "5' sticky overhangs", "3' sticky overhangs", "Single-stranded RNA"), 1, "Staggered cleavage after G yields 4-nt 5'-AATT overhangs."),
        // Virology
        QuizQuestion("q19", "virology", "Undergraduate", "HIV-1 gp120 engages host CD4 first and then undergoes a conformational shift to bind:", listOf("TLR4 or MD-2", "CCR5 or CXCR4", "CD21 or CD19", "ACE2 or TMPRSS2"), 1, "CCR5 (macrophage/M-tropic R5) or CXCR4 (T-tropic X4) mediates gp41 fusion triggering."),
        QuizQuestion("q20", "virology", "Undergraduate", "Which viral enzyme converts diploid HIV-1 ssRNA into integration-competent dsDNA?", listOf("Integrase", "Protease", "Reverse Transcriptase", "Neuraminidase"), 2, "Reverse transcriptase catalyzes RNA-dependent DNA polymerization, RNase H digestion, and DNA-dependent second-strand synthesis."),
        QuizQuestion("q21", "virology", "Advanced", "Why do latent HIV-1 proviruses persist despite long-term suppressive cART?", listOf("ART cannot kill non-replicating integrated DNA in quiescent memory CD4+ T cells", "Integrase inhibitors stimulate viral budding", "Plasma cells harbor free HIV virions", "CCR5 is expressed on erythrocytes"), 0, "ART blocks new rounds of infection but does not excise transcriptionally silent integrated proviruses."),
        QuizQuestion("q22", "virology", "Undergraduate", "Oseltamivir inhibits influenza spread by blocking:", listOf("Hemagglutinin binding", "Neuraminidase sialic acid cleavage", "M2 proton channel", "RNA cap snatching"), 1, "Blocking neuraminidase traps budding virions clumped to host sialic acid."),
        QuizQuestion("q23", "virology", "Advanced", "In secondary heterotypic dengue infection, sub-neutralizing IgG accelerates viral entry via:", listOf("FcγR-mediated antibody-dependent enhancement (ADE)", "Complement MAC pore formation", "CD4 receptor upregulation", "Perforin endocytosis"), 0, "FcγRs on monocytes internalize opsonized but unneutralized virions."),
        QuizQuestion("q24", "virology", "Beginner", "Antigenic shift in Influenza A results from:", listOf("Point mutations in HA", "Reassortment of segmented RNA genomes between strains", "Host ribosome frameshifting", "Antibody class switching"), 1, "Eight RNA segments permit sudden hybrid reassortment across co-infecting influenza A viruses."),
        // Microbiology
        QuizQuestion("q25", "microbiology", "Beginner", "Why do Gram-positive bacteria retain crystal violet stain after alcohol decolorization?", listOf("Thick cross-linked peptidoglycan dehydrates and traps dye-iodine complexes", "Outer LPS membrane binds safranin", "They lack cell walls", "Mycolic acid repels water"), 0, "Thick peptidoglycan shrinks under ethanol, trapping crystal violet–iodine."),
        QuizQuestion("q26", "microbiology", "Undergraduate", "Which culture plate inhibits Gram-positive bacteria and differentiates lactose fermenters as pink colonies?", listOf("Sheep blood agar", "MacConkey agar", "Sabouraud agar", "Chocolate agar"), 1, "Bile salts/crystal violet inhibit Gram-positives; lactose acid turns neutral red pink."),
        QuizQuestion("q27", "microbiology", "Undergraduate", "A Gram-positive cluster-forming coccus that is catalase-positive and coagulase-positive is classically:", listOf("Streptococcus pyogenes", "Staphylococcus aureus", "Enterococcus faecalis", "Neisseria meningitidis"), 1, "S. aureus produces both catalase and coagulase."),
        QuizQuestion("q28", "microbiology", "Advanced", "Macrolide resistance often arises via erm methylation of:", listOf("23S rRNA (50S subunit)", "16S rRNA (30S subunit)", "Penicillin-binding protein 2a", "DNA gyrase subunit A"), 0, "erm methylases methylate 23S rRNA A2058, blocking macrolide binding."),
        QuizQuestion("q29", "microbiology", "Beginner", "Which bacterial phase exhibits exponential doubling and peak susceptibility to cell-wall antibiotics?", listOf("Lag phase", "Log (exponential) phase", "Stationary phase", "Death phase"), 1, "Actively dividing log-phase bacteria rapidly synthesize peptidoglycan."),
        QuizQuestion("q30", "microbiology", "Undergraduate", "In Kirby–Bauer disc diffusion, a wider clear zone around an antibiotic disc indicates:", listOf("Higher bacterial resistance", "Greater apparent growth inhibition at the tested diffusion gradient", "Contamination", "Anaerobic metabolism"), 1, "Larger zone diameters correlate with lower MIC susceptibility (clinical interpretation requires validated breakpoints)."),
        // Hematology
        QuizQuestion("q31", "hematology", "Beginner", "Elevated circulating neutrophils with immature band forms ('left shift') classically points toward:", listOf("Acute bacterial infection", "Helminthic eosinophilia", "Vitamin B12 deficiency", "Pure viral suppression"), 0, "Bone marrow accelerates granulopoiesis and releases band neutrophils during acute bacterial challenge."),
        QuizQuestion("q32", "hematology", "Undergraduate", "Why can Type O RBCs usually be given emergently as packed red cells?", listOf("They lack surface A and B carbohydrate antigens", "They lack Rh(D) regardless of genetics", "They secrete anti-A/B IgG", "They suppress complement"), 0, "Packed O RBCs avoid immediate recipient anti-A/anti-B IgM agglutination."),
        QuizQuestion("q33", "hematology", "Undergraduate", "Which plasma cell malignancy produces a monoclonal Ig 'M-spike' and lytic bone lesions?", listOf("Chronic myeloid leukemia", "Multiple myeloma", "Hodgkin lymphoma", "Aplastic anemia"), 1, "Clonal bone marrow plasma cells secrete monoclonal immunoglobulin and activate osteoclasts."),
        QuizQuestion("q34", "hematology", "Advanced", "A positive Direct Coombs (DAT) test detects:", listOf("Free circulating anti-RBC antibodies in serum", "IgG or C3d already bound directly onto patient RBCs in vivo", "Platelet aggregation", "Fibrin degradation products"), 1, "DAT agglutinates patient RBCs directly with anti-human globulin reagent."),
        QuizQuestion("q35", "hematology", "Beginner", "Which granulocyte normally accounts for fewer than 1% of circulating white blood cells?", listOf("Neutrophil", "Lymphocyte", "Basophil", "Monocyte"), 2, "Basophils are the rarest circulating granulocytes (<1%)."),
        // Pathology
        QuizQuestion("q36", "pathology", "Undergraduate", "Systemic lupus erythematosus (SLE) glomerulonephritis is a classic prototype of:", listOf("Type I immediate IgE allergy", "Type III immune-complex deposition", "Type IV granuloma", "Complement C1-inhibitor excess"), 1, "dsDNA–anti-dsDNA complexes deposit in glomerular capillaries and fix complement."),
        QuizQuestion("q37", "pathology", "Undergraduate", "A positive tuberculin (PPD) skin induration at 48–72 hours is mediated primarily by:", listOf("IgE and mast cells", "Th1 memory cells and macrophages (Type IV DTH)", "Immune complexes and neutrophils", "NK cell ADCC"), 1, "Type IV delayed-type hypersensitivity peaks at 48–72 hours."),
        QuizQuestion("q38", "pathology", "Advanced", "Caseating necrosis inside a pulmonary granuloma reflects coordinated activation of macrophages by:", listOf("IL-4 and IL-13", "IFN-γ and TNF-α", "IL-10 and TGF-β", "IgE and tryptase"), 1, "Th1 IFN-γ and TNF-α maintain epithelioid macrophage containment."),
        QuizQuestion("q39", "pathology", "Undergraduate", "Which endogenous danger molecule released by necrotic cells activates TLR4/RAGE?", listOf("HMGB1", "Cytochrome c inside intact mitochondria", "Albumin", "Insulin"), 0, "Nuclear HMGB1 released during necrosis serves as a canonical DAMP."),
        QuizQuestion("q40", "pathology", "Advanced", "Why does anti-TNF therapy risk reactivation of latent Mycobacterium tuberculosis?", listOf("TNF-α is required to maintain chemokine signaling and granuloma cellular architecture", "TNF-α kills M. tuberculosis directly like penicillin", "Anti-TNF converts Gram-negative LPS", "TNF-α stimulates IgE synthesis"), 0, "Neutralizing TNF-α destabilizes granulomas, releasing sequestered bacilli."),
        // Cellular Biology
        QuizQuestion("q41", "cell_bio", "Beginner", "Which organelle undergoes dramatic expansion in antibody-secreting plasma cells?", listOf("Smooth ER", "Rough endoplasmic reticulum", "Peroxisome", "Centriole"), 1, "Dense rough ER synthesizes and folds thousands of immunoglobulin chains per second."),
        QuizQuestion("q42", "cell_bio", "Undergraduate", "Extrinsic apoptosis is triggered when FasL binds Fas, recruiting FADD and activating:", listOf("Caspase-1", "Caspase-8", "Caspase-9", "Cathepsin B"), 1, "DISC assembly cleaves initiator Caspase-8."),
        QuizQuestion("q43", "cell_bio", "Advanced", "Which metabolic pathway dominates quiescent memory CD8+ T cells compared with glycolytic effectors?", listOf("Fatty-acid oxidation & oxidative phosphorylation", "Anaerobic lactate fermentation", "Pentose phosphate shunt only", "Urea cycle"), 0, "Spare respiratory capacity in fused mitochondria sustains long-term memory survival."),
        QuizQuestion("q44", "cell_bio", "Undergraduate", "Pyroptosis is distinguished from apoptosis by plasma membrane pore formation mediated by cleaved:", listOf("BAX", "Gasdermin D (GSDMD)", "Clathrin", "Lamin A"), 1, "Caspase-1/4/5/11 cleaves GSDMD to release N-terminal pore-forming domains and IL-1β."),
        QuizQuestion("q45", "cell_bio", "Beginner", "Which cytoskeletal structure reorients toward an immunological synapse to aim cytotoxic granules?", listOf("Microtubule-organizing center (MTOC)", "Nuclear lamina", "Intermediate keratin", "Gap junction"), 0, "Dynein-driven MTOC polarization focuses secretion onto the target contact zone."),
        // Clinical Immunology
        QuizQuestion("q46", "clin_imm", "Undergraduate", "Why are bacterial capsular polysaccharides conjugated to carrier proteins in infant vaccines?", listOf("To recruit CD4+ Tfh help for class switching and memory B-cell generation", "To avoid MHC II processing", "To induce IgE", "To activate C1q directly"), 0, "Pure polysaccharides are T-independent; protein conjugation enables linked T-cell help."),
        QuizQuestion("q47", "clin_imm", "Undergraduate", "Checkpoint inhibitors such as nivolumab enhance anti-tumor CD8+ T-cell function by blocking:", listOf("PD-1 / PD-L1 inhibitory signaling", "CD28 costimulation", "IL-2 receptor α", "MHC Class I expression"), 0, "PD-1 recruits SHP-2 phosphatase to blunt TCR and CD28 signaling."),
        QuizQuestion("q48", "clin_imm", "Advanced", "In Chronic Granulomatous Disease (CGD), recurrent catalase-positive bacterial infections occur because neutrophils lack:", listOf("NADPH oxidase superoxide burst", "Chemotactic CXCR2", "Adhesion integrins (CD18)", "Perforin"), 0, "Without NADPH oxidase ROS, phagocytes fail to kill catalase+ organisms that neutralize their own H2O2."),
        QuizQuestion("q49", "clin_imm", "Undergraduate", "Which drug is first-line life-saving treatment for systemic Type I anaphylaxis?", listOf("Oral loratadine", "Intramuscular epinephrine", "Inhaled salmeterol", "Methotrexate"), 1, "Epinephrine reverses vasodilation, edema, and bronchospasm via α/β-adrenergic agonism."),
        QuizQuestion("q50", "clin_imm", "Advanced", "A defect in Bruton tyrosine kinase (BTK) causes X-linked agammaglobulinemia characterized by:", listOf("Absent mature CD19+ B cells and pan-hypogammaglobulinemia", "Absent CD3+ T cells", "Isolated C9 deficiency", "Elevated serum IgE >2000 IU/mL"), 0, "Pre-BCR signaling fails without BTK, arresting B-cell maturation in bone marrow."),
        // Bioinformatics
        QuizQuestion("q51", "bioinfo", "Beginner", "High GC content in a PCR primer raises its melting temperature (Tm) primarily because:", listOf("G≡C pairs form three hydrogen bonds plus stronger base-stacking than A=T", "Guanine binds Mg2+ covalently", "Cytosine prevents polymerase extension", "GC regions repel water"), 0, "Three H-bonds and favorable π-stacking stabilize duplex DNA."),
        QuizQuestion("q52", "bioinfo", "Undergraduate", "When searching for a short conserved catalytic motif inside two divergent multi-domain proteins, which alignment is preferred?", listOf("Needleman–Wunsch global alignment", "Smith–Waterman local alignment", "UPGMA clustering", "4-parameter logistic curve"), 1, "Smith–Waterman zeroes negative scores to isolate the highest-scoring local homologous region."),
        QuizQuestion("q53", "bioinfo", "Undergraduate", "In coding DNA, a single-nucleotide substitution converting UAC (Tyr) to UAA (Stop) is classified as a:", listOf("Synonymous variant", "Missense variant", "Nonsense variant", "Frameshift insertion"), 2, "Introducing a premature termination codon produces a truncated nonsense mutation."),
        QuizQuestion("q54", "bioinfo", "Advanced", "Why does Jukes–Cantor distance transform raw p-distance in molecular phylogenetics?", listOf("To correct for unseen multiple/back nucleotide substitutions over evolutionary time", "To translate codons to amino acids", "To normalize RNA-seq TPM", "To estimate protein pI"), 0, "d = -(3/4) ln(1 - 4p/3) accounts for saturation at diverged sites."),
        QuizQuestion("q55", "bioinfo", "Undergraduate", "In AlphaFold DB structures, high residue confidence is reflected by pLDDT scores above:", listOf("90 (very high confidence)", "20", "0.5", "-10"), 0, "pLDDT spans 0–100; >90 indicates high backbone and side-chain accuracy, <50 often indicates intrinsic disorder."),
        QuizQuestion("q56", "clin_imm", "Beginner", "Which cells secrete histamine within seconds when allergen crosslinks surface IgE?", listOf("Mast cells & basophils", "Neutrophils", "CD8+ T cells", "Memory B cells"), 0, "Preformed histamine granules are released immediately upon FcεRI crosslinking."),
        QuizQuestion("q57", "adv_imm", "Advanced", "Which complement regulator accelerates decay of C3 convertases and serves as a Factor I cofactor on host cells?", listOf("Factor H / CD55 (DAF)", "C5b", "Properdin", "Mannose-binding lectin"), 0, "Host surfaces display sialic acid recruiting Factor H and membrane CD55/CD46 to prevent bystander lysis."),
        QuizQuestion("q58", "microbiology", "Advanced", "Endotoxin shock from Gram-negative bacteremia is triggered primarily by lipid A engaging:", listOf("TLR4–MD-2", "TLR3", "NOD1 only", "RIG-I"), 0, "Lipid A inserts into the MD-2 hydrophobic pocket to dimerize TLR4."),
        QuizQuestion("q59", "mol_bio", "Advanced", "In a 4-parameter logistic (4PL) ELISA standard curve, parameter C represents:", listOf("The inflection point / EC50 concentration", "Background noise floor", "Maximum optical density", "Hill slope"), 0, "y = D + (A - D) / (1 + (x/C)^B), where C is the midpoint inflection concentration."),
        QuizQuestion("q60", "hematology", "Advanced", "Which cytokine stimulates megakaryopoiesis and platelet production vs erythropoiesis?", listOf("Thrombopoietin (TPO) vs Erythropoietin (EPO)", "G-CSF vs IL-5", "IL-2 vs IFN-γ", "CCL2 vs CXCL8"), 0, "TPO drives megakaryocyte maturation while EPO supports erythroid progenitors.")
    )

    val CLINICAL_CASES = listOf(
        ClinicalCase(
            id = "case_xla",
            title = "Case 1 (Fictional): Recurrent Bacterial Sinopneumonia in Infancy",
            difficulty = "Undergraduate",
            vignette = "A fictional 9-month-old boy presents with his third episode of Streptococcus pneumoniae otitis media and pneumonia after maternal transplacental antibodies waned around 6 months of age. Physical examination reveals absent palatine tonsils.",
            labFindings = listOf(
                "Serum IgG: 45 mg/dL (markedly low)",
                "Serum IgA & IgM: Undetectable",
                "Flow cytometry: CD3+ T cells normal (72%), CD19+ B cells <0.5%",
                "TREC newborn screen: Normal"
            ),
            question = "Which molecular defect most closely explains this immunophenotype?",
            options = listOf(
                "X-linked Bruton tyrosine kinase (BTK) deficiency arresting bone marrow pre-B maturation",
                "AIRE transcription factor mutation affecting thymic medulla",
                "NADPH oxidase CYBB deficiency",
                "Terminal complement C9 deficiency"
            ),
            correctIndex = 0,
            synthesis = "Loss of BTK blocks pre-BCR signaling in the bone marrow, resulting in absent circulating B cells, tiny/absent lymphoid follicles (no tonsils), pan-hypogammaglobulinemia, and recurrent encapsulated bacterial infections once maternal IgG clears."
        ),
        ClinicalCase(
            id = "case_dth",
            title = "Case 2 (Fictional): Persistent Cough & Necrotizing Apical Granuloma",
            difficulty = "Advanced",
            vignette = "A fictional 34-year-old healthcare worker develops chronic productive cough, evening fevers, night sweats, and a right upper lobe cavitary infiltrate. Before initiating an anti-TNF biologic for an unrelated condition, an IGRA blood assay returned strongly positive.",
            labFindings = listOf(
                "Interferon-Gamma Release Assay (IGRA): Markedly elevated IFN-γ to ESAT-6/CFP-10",
                "Sputum Xpert MTB/RIF NAAT: Mycobacterium tuberculosis detected",
                "Biopsy morphology: Caseating granulomas with multinucleated Langhans giant cells"
            ),
            question = "Which immune axis is central both to granuloma containment and the risk of reactivation under TNF blockade?",
            options = listOf(
                "IL-12 / IFN-γ Th1–macrophage activation sustained by TNF-α chemokine signaling",
                "IgE-mediated mast cell degranulation",
                "NK missing-self killing of erythrocytes",
                "BCR hypermutation in Peyer's patches"
            ),
            correctIndex = 0,
            synthesis = "Th1 CD4+ T cells secrete IFN-γ to activate macrophage nitric oxide/phagolysosomal killing while TNF-α maintains chemokine recruitment and granuloma integrity."
        ),
        ClinicalCase(
            id = "case_sle",
            title = "Case 3 (Fictional): Photosensitive Rash, Arthralgia & Low Complement",
            difficulty = "Undergraduate",
            vignette = "A fictional 26-year-old graduate student reports symmetric small-joint arthralgias, fatigue, a malar facial erythema sparing the nasolabial folds, and frothy urine.",
            labFindings = listOf(
                "ANA: High titer speckled/homogeneous (1:640)",
                "Anti-dsDNA & anti-Smith antibodies: Positive",
                "Serum C3 and C4: Markedly decreased",
                "Urinalysis: Proteinuria + dysmorphic RBC casts"
            ),
            question = "What drives the simultaneous elevation of anti-dsDNA and drop in serum C3/C4?",
            options = listOf(
                "Type III immune-complex deposition in glomeruli consuming classical complement proteins",
                "Type I mast cell histamine release",
                "Genetic absence of bone marrow stem cells",
                "HIV gp120 binding to CD4 on podocytes"
            ),
            correctIndex = 0,
            synthesis = "Defective clearance of apoptotic nuclear debris promotes autoantibodies to dsDNA/histones; circulating immune complexes deposit in glomerular capillaries and fix C1q -> C4 -> C3."
        )
    )
}
