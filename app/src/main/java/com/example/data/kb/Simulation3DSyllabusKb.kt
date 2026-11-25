package com.example.data.kb

data class SyllabusStructureItem(
    val objectId: String,
    val label: String,
    val compartment: String,
    val keyMarkers: String,
    val biologicalFunction: String
)

data class SyllabusQuizItem(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class Simulation3DSyllabusUnit(
    val id: String,
    val unitLabel: String,
    val title: String,
    val screenOrigin: String,
    val difficulty: String,
    val estimatedMinutes: Int,
    val prerequisites: String,
    val overview: String,
    val learningObjectives: List<String>,
    val labeledStructures: List<SyllabusStructureItem>,
    val guidedProtocol: List<String>,
    val quantitativeBenchmarks: List<String>,
    val selfCheck: List<SyllabusQuizItem>
)

object Simulation3DSyllabusKb {
    val UNITS: List<Simulation3DSyllabusUnit> = listOf(
        Simulation3DSyllabusUnit(
            id = "organs_3d",
            unitLabel = "Unit 1 of 6 • Anatomy 3D",
            title = "Primary & Secondary Lymphoid Organ Network (3D)",
            screenOrigin = "Atlas -> Organs 3D",
            difficulty = "Foundational",
            estimatedMinutes = 12,
            prerequisites = "Basic hematopoiesis & blood vs lymphatic drainage concepts",
            overview = "Explore the spatial organization connecting generative primary lymphoid niches (bone marrow, thymus) with peripheral secondary surveillance organs (spleen, regional lymph nodes) and mucosal barriers (tonsils, GALT, Peyer's patches).",
            learningObjectives = listOf(
                "Distinguish primary tolerance-education organs (bone marrow, thymus) from secondary antigen-capture organs.",
                "Trace lymphatic drainage corridors linking peripheral tissues to cervical, axillary, and inguinal nodes.",
                "Contrast blood-borne splenic clearance (white vs red pulp) with mucosal M-cell transcytosis in Peyer's patches.",
                "Relate organ-level defects (e.g., thymic aplasia, asplenia) to specific infectious vulnerabilities."
            ),
            labeledStructures = listOf(
                SyllabusStructureItem("marrow", "Bone Marrow", "Primary Lymphoid", "CD34+ HSCs, CXCL12+ stroma, IL-7", "Hematopoiesis of all leukocytes and B-cell central tolerance / receptor editing."),
                SyllabusStructureItem("thymus", "Thymus", "Primary Lymphoid", "AIRE+ mTECs, Notch1, CCL19/21", "MHC restriction (positive selection) and self-antigen deletion (negative selection) of T cells."),
                SyllabusStructureItem("spleen", "Spleen", "Secondary Lymphoid", "Marginal zone B cells, PALS T zones", "Filters blood-borne encapsulated bacteria and senescent erythrocytes."),
                SyllabusStructureItem("ln_cervical", "Cervical Lymph Nodes", "Secondary Lymphoid", "CCR7+ DCs, HEVs (PNAd+)", "Captures upper respiratory & oral antigens via afferent lymphatics."),
                SyllabusStructureItem("ln_axillary", "Axillary Lymph Nodes", "Secondary Lymphoid", "Follicular DCs, Tfh, GC B cells", "Drains upper limb/thorax; prime germinal center site after deltoid vaccination."),
                SyllabusStructureItem("peyers", "Peyer's Patches & GALT", "Mucosal Lymphoid", "GP2+ M cells, IgA+ plasmablasts", "Samples luminal gut microbiota to calibrate oral tolerance and secretory IgA.")
            ),
            guidedProtocol = listOf(
                "Rotate the 3D network laterally to inspect the anterior-posterior depth separation between the retroperitoneal/splenic axis and superficial lymph node chains.",
                "Tap 'Primary' filter to isolate Bone Marrow and Thymus, confirming both serve as antigen-independent lymphocyte schools.",
                "Switch filter back to 'All', tap Bone Marrow and Thymus in sequence, and compare their B-cell vs T-cell developmental checkpoints.",
                "Slide the Cross-Section Z slider to 0.15 to slice away anterior nodes and reveal deeper splenic vascular relationships."
            ),
            quantitativeBenchmarks = listOf(
                "Naive lymphocyte transit dwell time per lymph node: ~12–24 hours before S1PR1-driven efferent exit.",
                "Thymic selection attrition: >95% of developing thymocytes undergo apoptosis (death by neglect or negative selection).",
                "Spleen blood throughput: ~5% of total cardiac output (~250–300 mL/min) filtered continuously."
            ),
            selfCheck = listOf(
                SyllabusQuizItem(
                    question = "Which secondary organ filters blood-borne pathogens directly without afferent lymphatic vessels?",
                    options = listOf("Axillary Lymph Node", "Spleen", "Thymus", "Tonsils"),
                    correctIndex = 1,
                    explanation = "The spleen samples systemic blood directly via the splenic artery and marginal zone sinuses, lacking afferent lymphatics."
                ),
                SyllabusQuizItem(
                    question = "Which primary organ relies on AIRE transcription factor in medullary epithelial cells to purge autoreactive T clones?",
                    options = listOf("Bone Marrow", "Spleen", "Thymus", "Peyer's Patches"),
                    correctIndex = 2,
                    explanation = "AIRE enables medullary thymic epithelial cells (mTECs) to display tissue-restricted self-peptides during negative selection."
                )
            )
        ),
        Simulation3DSyllabusUnit(
            id = "cells_3d",
            unitLabel = "Unit 2 of 6 • Cellular Architecture 3D",
            title = "Immune Cell Ultrastructure & Surface Receptors (3D)",
            screenOrigin = "Atlas -> Cells 3D",
            difficulty = "Undergraduate",
            estimatedMinutes = 15,
            prerequisites = "Unit 1 Lymphoid Anatomy & basic receptor-ligand affinity",
            overview = "Examine how plasma membrane receptors, cytoplasmic bioenergetic organelles, and nuclear polarity align to enable antigen sensing, co-stimulation, and targeted cytolysis or secretion.",
            learningObjectives = listOf(
                "Map extracellular sensing receptors (TCR, BCR, TLRs, FcRs) radially across the 3D plasma membrane sphere.",
                "Compare activation Signal 1 (antigen specificity), Signal 2 (CD28/CD40 co-stimulation), and Signal 3 (cytokines).",
                "Interpret multi-tier phenotype markers across Beginner, Undergraduate, Advanced, and Research depths."
            ),
            labeledStructures = listOf(
                SyllabusStructureItem("membrane", "Plasma Membrane", "Cell Boundary", "Lipid rafts, Integrins (LFA-1)", "Fluid mosaic surface organizing immune synapses and receptor microclusters."),
                SyllabusStructureItem("nucleus", "Nucleus", "Genetic Hub", "T-bet / GATA3 / FOXP3 / BLIMP-1", "Integrates phosphorylated STAT, NF-κB, and NFAT transcription factors."),
                SyllabusStructureItem("mito1", "Mitochondria", "Bioenergetics", "OXPHOS vs Warburg glycolysis", "Sets effector proliferation rate vs spare respiratory capacity in memory cells."),
                SyllabusStructureItem("golgi", "Golgi Apparatus", "Secretory Sorting", "COPII / Clathrin / MTOC", "Orients toward target cells to focus cytokine or perforin-granzyme release.")
            ),
            guidedProtocol = listOf(
                "Select 'CD4+ T Helper Cell' and inspect the radial receptor nodes distributed around the plasma membrane.",
                "Slide Cross-Section Z inward to 0.0 to look inside the translucent membrane and observe Nucleus, Mitochondrion, and Golgi.",
                "Toggle 'Surface Receptors Overlay' OFF and ON to contrast the bare intracellular chassis with full receptor display.",
                "Switch cell selection to 'Plasma Cell' and then 'NK Cell' to compare receptor repertoires and secreted payloads."
            ),
            quantitativeBenchmarks = listOf(
                "TCR affinity for agonist pMHC: modest Kd ~1–50 µM compensated by serial triggering and coreceptor avidity.",
                "Antibody secretion rate of mature bone-marrow plasma cell: ~2,000–10,000 immunoglobulin molecules/second.",
                "Immunological synapse diameter: ~8–12 µm organized into concentric cSMAC, pSMAC, and dSMAC rings."
            ),
            selfCheck = listOf(
                SyllabusQuizItem(
                    question = "During cytotoxic T-cell killing, which internal structure polarizes toward the target contact zone to aim lytic granules?",
                    options = listOf("Smooth ER", "MTOC / Golgi Apparatus", "Peroxisome", "Nuclear Pore"),
                    correctIndex = 1,
                    explanation = "Dynein motors pull the microtubule-organizing center (MTOC) and closely associated Golgi toward the immunological synapse."
                )
            )
        ),
        Simulation3DSyllabusUnit(
            id = "organelles_3d",
            unitLabel = "Unit 3 of 6 • Subcellular Trafficking 3D",
            title = "Eukaryotic Immune Organelles & Antigen Processing (3D)",
            screenOrigin = "Cells -> Organelles 3D",
            difficulty = "Undergraduate",
            estimatedMinutes = 14,
            prerequisites = "Eukaryotic cell compartmentalization & protein secretion",
            overview = "Trace intracellular antigen routing and secretory biosynthesis in three dimensions—connecting cytosolic proteasome/ER MHC Class I loading with endolysosomal MHC Class II pathways.",
            learningObjectives = listOf(
                "Locate all 9 canonical eukaryotic compartments and link each to a dedicated immunological mechanism.",
                "Contrast MHC Class I peptide loading (Proteasome -> TAP -> Rough ER) with MHC Class II loading (Phagolysosome -> HLA-DM).",
                "Explain how unfolded protein response (XBP1) expands the rough ER during plasma-cell differentiation."
            ),
            labeledStructures = listOf(
                SyllabusStructureItem("er", "Rough Endoplasmic Reticulum", "Biosynthetic Core", "TAP1/2, Tapasin, Calreticulin, XBP1", "Translates secretory Ig chains and loads 8–10 aa peptides onto MHC Class I."),
                SyllabusStructureItem("lysosome", "Lysosomes & Phagolysosomes", "Digestive Compartment", "V-ATPase (pH ~4.5), Cathepsins, NOX2", "Destroys ingested microbes with ROS/HOCl and generates MHC Class II peptides."),
                SyllabusStructureItem("mito", "Mitochondria", "Metabolic & Apoptotic", "Cytochrome c, MAVS, mtROS", "Houses antiviral MAVS scaffolds and regulates intrinsic Bax/Bak apoptosis."),
                SyllabusStructureItem("peroxisome", "Peroxisomes & Cytoskeleton", "Polarity & Redox", "Catalase, Microtubule MTOC", "Detoxifies H2O2 and provides directional tracks for polarized degranulation.")
            ),
            guidedProtocol = listOf(
                "Tap 'Rough Endoplasmic Reticulum' directly in the 3D canvas (or pill list) and note its spatial proximity to the Nucleus.",
                "Tap 'Lysosomes & Phagolysosomes' to inspect the acidic endocytic limb positioned near the periphery.",
                "Adjust the Cross-Section Z slider to -0.15 to isolate posterior organelle depth ordering.",
                "Pair this 3D inspection with the 'Cellular Processes' tab ('Phagocytosis' & 'Protein Synthesis') to animate step-by-step cargo flow."
            ),
            quantitativeBenchmarks = listOf(
                "Phagolysosomal lumen pH: drops rapidly from ~7.2 to ~4.5–5.0 via V-ATPase proton pumping.",
                "MHC Class I bound peptide length: tightly constrained to 8–10 amino acids; MHC Class II accommodates 13–25 aa.",
                "Plasma cell rough ER volume fraction: expands >5-fold relative to resting naive B cells."
            ),
            selfCheck = listOf(
                SyllabusQuizItem(
                    question = "Which organelle houses MAVS antiviral signaling complexes and releases cytochrome c during intrinsic apoptosis?",
                    options = listOf("Golgi Apparatus", "Mitochondria", "Ribosome", "Lysosome"),
                    correctIndex = 1,
                    explanation = "Outer mitochondrial membranes anchor MAVS downstream of RIG-I and release cytochrome c to trigger Apaf-1/caspase-9."
                )
            )
        ),
        Simulation3DSyllabusUnit(
            id = "hiv_virion_3d",
            unitLabel = "Unit 4 of 6 • Virology Structural 3D",
            title = "HIV-1 Virion Cutaway Architecture & Latency (3D)",
            screenOrigin = "Diseases -> HIV Immunology Research",
            difficulty = "Advanced",
            estimatedMinutes = 16,
            prerequisites = "Retroviral reverse transcription & CD4+ T-cell biology",
            overview = "Inspect the concentric structural shells of an HIV-1 virion in 3D cutaway—from host-derived lipid envelope and trimeric gp120/gp41 spikes through the p17 matrix shell to the conical p24 capsid core housing diploid (+)ssRNA.",
            learningObjectives = listOf(
                "Identify each radial virion layer (Env gp120/gp41, lipid bilayer, p17 matrix, p24 conical capsid) via 3D slice planes.",
                "Map all 6 replication stages to clinically validated antiretroviral (cART) pharmacologic classes.",
                "Explain why transcriptionally silent proviral reservoirs in memory CD4+ T cells resist cART alone."
            ),
            labeledStructures = listOf(
                SyllabusStructureItem("gp120_0", "Env gp120/gp41 Trimer", "Viral Surface Spike", "CD4bs, V1/V2/V3 loops, N-glycan shield", "Binds host CD4 then CCR5/CXCR4 coreceptor to trigger gp41 six-helix membrane fusion."),
                SyllabusStructureItem("env", "Host-Derived Lipid Envelope", "Outer Bilayer", "Cholesterol-rich raft, MHC incorporations", "Acquired during ESCRT budding from host plasma membrane; targeted by detergent/neutralizing antibodies."),
                SyllabusStructureItem("p17", "p17 Matrix Shell (MA)", "Sub-Membrane Scaffold", "Myristoylated Gag N-terminus", "Lines inner leaflet and recruits Env trimers into budding virions."),
                SyllabusStructureItem("p24_0", "Conical p24 Capsid (CA) + RNA", "Core Shell", "~1,500 CA hexamers/pentamers, RT, IN", "Shields diploid (+)ssRNA and Reverse Transcriptase/Integrase during cytoplasmic transport to nuclear pore.")
            ),
            guidedProtocol = listOf(
                "Drag the HIV-1 Virion 3D model slowly to view the surface distribution of pink gp120/gp41 trimer spikes.",
                "Slide Cross-Section Z down from 1.1 to 0.15 to slice open the translucent envelope and expose the conical gold p24 capsid.",
                "Tap individual components ('Lipid Envelope', 'p17 Matrix', 'p24 Capsid', 'Env gp120/gp41') to highlight their exact concentric shells.",
                "Scroll to the Latent Reservoir Simulator below and sweep LRA Activation + CTL/bNAb kill rates to compare decay curves against ART alone."
            ),
            quantitativeBenchmarks = listOf(
                "Virion diameter: ~110–130 nm; sparse ~7–14 Env trimer spikes per virion minimize antibody avidity.",
                "Latent reservoir half-life on suppressive ART: ~44 months (~3.7 years) requiring >70 years for natural exhaustion.",
                "CD4-tropic reservoir decay requires both latency reversal ('shock') and effector clearance ('kill')."
            ),
            selfCheck = listOf(
                SyllabusQuizItem(
                    question = "Which HIV-1 structural protein forms the distinctive conical core surrounding diploid RNA, RT, and Integrase?",
                    options = listOf("gp120", "p17 Matrix", "p24 Capsid", "Nef"),
                    correctIndex = 2,
                    explanation = "Following Gag proteolytic maturation by viral protease, ~1,500 p24 (CA) monomers assemble into a fullerene cone capsid."
                )
            )
        ),
        Simulation3DSyllabusUnit(
            id = "pdb_structure_3d",
            unitLabel = "Unit 5 of 6 • Structural Bioinformatics 3D",
            title = "Experimental RCSB PDB & AlphaFold Macromolecules (3D)",
            screenOrigin = "Molecules -> Molecules & PDB",
            difficulty = "Research",
            estimatedMinutes = 18,
            prerequisites = "Protein folding (domains, Ig-fold) & coordinate formats",
            overview = "Inspect live atomic C-α backbones and heteroatom ligands fetched directly from RCSB PDB alongside AlphaFold DB metadata—coloring multi-chain complexes (IgG, TCR–pMHC, TLR4–MD-2) by subunit or N→C sequence polarity.",
            learningObjectives = listOf(
                "Trace multi-chain quaternary folds (e.g., 1IGT intact IgG heavy/light chains, 1AO7 TCR–pMHC interface).",
                "Toggle between Chain Identity coloring and N-to-C sequence gradient to orient polypeptide domains.",
                "Locate bound HETATM ligands, peptides, or cofactors within active sites and tap coordinates for residue inspection."
            ),
            labeledStructures = listOf(
                SyllabusStructureItem("chain_trace", "C-α Polypeptide Backbone", "Secondary/Quaternary", "α-helices, β-barrels, Ig-folds", "Traces folded Fab/Fc, MHC α1/α2 peptide cleft, or LRR horseshoe ectodomains."),
                SyllabusStructureItem("het_ligands", "HETATM Ligands & Cofactors", "Binding Pocket", "Glycans, lipids (LPS), metal ions", "Highlight red spheres indicating non-polymer cofactors, drugs, or bound epitopes.")
            ),
            guidedProtocol = listOf(
                "Select 'Immunoglobulin G (IgG)' (PDB 1IGT) and tap 'Load 3D PDB' to render real C-α coordinates.",
                "Toggle 'Color by Chain' ON to distinguish heavy and light chains, then OFF to inspect N-terminus-to-C-terminus domain progression.",
                "Tap any backbone sphere or ligand pocket to inspect the exact chain, residue name, and sequence number.",
                "Load 'TCR–pMHC Complex' (1AO7) or 'TLR4–MD-2–LPS' (3FXI) and use Cross-Section Z to inspect the central binding groove."
            ),
            quantitativeBenchmarks = listOf(
                "IgG molecular mass: ~150 kDa (two ~50 kDa heavy chains + two ~25 kDa light chains).",
                "High-resolution X-ray crystal structures resolve side chains cleanly below ~2.0 Å; Cryo-EM routinely achieves 2.5–3.5 Å.",
                "AlphaFold pLDDT > 90 denotes high atomic confidence; pLDDT < 50 correlates with intrinsically disordered linkers."
            ),
            selfCheck = listOf(
                SyllabusQuizItem(
                    question = "In a PDB file, which record type represents non-polymer small molecules such as bound LPS, glycans, or enzyme cofactors?",
                    options = listOf("ATOM C-α", "HETATM", "HEADER", "SEQRES"),
                    correctIndex = 1,
                    explanation = "HETATM records store coordinates of ligands, cofactors, ions, and post-translational modifiers distinct from standard polymer residues."
                )
            )
        ),
        Simulation3DSyllabusUnit(
            id = "immune_kinetics_3d",
            unitLabel = "Unit 6 of 6 • Systems Immunology 3D + ODE",
            title = "Coupled 9-Variable Immune Kinetics & 3D Cellular Arena",
            screenOrigin = "Simulator -> Live 3D Arena & Charts",
            difficulty = "Advanced",
            estimatedMinutes = 20,
            prerequisites = "Units 1–3 & basic differential rate thinking",
            overview = "Integrate numerical ODE trajectories with a live 3D cellular arena across Day 0–60. Track how pathogen load, innate sentinels, APC priming, CD4/CD8 expansion, germinal center affinity maturation, antibody opsonization, and tissue tolerance co-evolve.",
            learningObjectives = listOf(
                "Identify all 5 physiological kinetic phases from Innate Sensing through Contraction & Memory.",
                "Quantify clearance efficiency using t50 clearance time, sterile elimination day, Antigen AUC, and Cytokine Storm Risk.",
                "Contrast naive Primary kinetics against Anamnestic Secondary recall, Vaccination + Challenge, Immunodeficiency, and Autoimmune Tolerance Loss."
            ),
            labeledStructures = listOf(
                SyllabusStructureItem("pathogen_cluster", "Pathogen / Antigen Swarm", "Infection Site", "Replicating load A(t)", "Drives innate PRR sensing and collateral damage unless contained by phagocytes and antibodies."),
                SyllabusStructureItem("innate_sentinel", "Innate Sentinels & APCs", "Peripheral & Conduit", "Neutrophils, MΦ, cDCs", "Early containment arm; activates T cells proportional to APC Priming index."),
                SyllabusStructureItem("t_effector", "CD4+ / CD8+ Effector T Pool", "Paracortex -> Tissue", "Th1/Tfh helpers & CTLs", "Drives cell-mediated cytolysis and licenses B-cell germinal center affinity maturation."),
                SyllabusStructureItem("b_gc_plasma", "B Cells, Germinal Center & IgG", "Follicle & Serum", "Affinity Index (1x–5x), IgG", "Differentiates into plasma cells secreting opsonizing antibodies for sterile clearance."),
                SyllabusStructureItem("treg_shield", "Treg / Self-Tolerance Halo", "Systemic Brake", "τ_tol threshold, IL-10/TGF-β", "Constrains cytokine amplification and prevents autoreactive tissue injury."),
                SyllabusStructureItem("memory_pool", "Memory B & T Niche", "Marrow & Tcm/Trm", "Long-lived survival", "Persists post-clearance to ignite rapid secondary recall upon Day-21 or future re-exposure.")
            ),
            guidedProtocol = listOf(
                "Switch the Simulator Arena view to '3D Arena' and press Play to watch Pathogen, APCs, T Cells, B/Ab, Treg, and Memory nodes resize dynamically across time.",
                "Compare 'Primary Response' vs 'Secondary Memory Recall': note the jump in Peak Antibody, faster t50 clearance, and higher Affinity Maturation index.",
                "Select 'Vaccination + Challenge' and scrub the replay slider specifically around Day 21 to watch pre-formed antibodies blunt the rechallenge spike.",
                "Lower 'Self-Tolerance Threshold' below 0.40 and switch charts to 'Derived Compartments & GC' to observe Cytokine Storm Risk and Damage AUC climb."
            ),
            quantitativeBenchmarks = listOf(
                "Primary naive IgG peak typically requires 10–14 days vs 3–5 days during secondary anamnestic recall.",
                "Germinal center somatic hypermutation boosts functional BCR/antibody affinity >10–100 fold (normalized 1x–5x index here).",
                "Healthy effector contraction eliminates ~90–95% of peak T effectors while preserving ~5–10% in IL-7/IL-15 memory niches."
            ),
            selfCheck = listOf(
                SyllabusQuizItem(
                    question = "In the 'Vaccination + Challenge' preset, why does the Day 21 live challenge produce far lower tissue damage than an unprimed infection?",
                    options = listOf(
                        "Vaccine antigen permanently disables innate neutrophils",
                        "Pre-existing memory B/T cells and baseline antibodies rapidly neutralize the Day-21 challenge",
                        "Antibodies increase antigen growth rate",
                        "Tolerance threshold drops to zero"
                    ),
                    correctIndex = 1,
                    explanation = "Anamnestic recall activates memory B and T pools immediately, capping pathogen AUC before severe cytokine damage develops."
                )
            )
        )
    )

    fun byId(id: String): Simulation3DSyllabusUnit =
        UNITS.firstOrNull { it.id == id } ?: UNITS.first()
}
