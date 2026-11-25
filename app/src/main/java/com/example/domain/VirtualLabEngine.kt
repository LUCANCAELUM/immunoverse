package com.example.domain

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

data class LabParamDef(
    val key: String,
    val label: String,
    val min: Float,
    val max: Float,
    val defaultVal: Float,
    val unit: String = ""
)

data class LabVisualData(
    val mode: String, // "curve", "flow", "gel", "microscopy", "chrom", "plate", "steps"
    val seriesA: List<Pair<Double, Double>> = emptyList(),
    val seriesB: List<Pair<Double, Double>> = emptyList(),
    val flowPoints: List<Pair<Float, Float>> = emptyList(),
    val gelLanes: List<List<Int>> = emptyList(),
    val extraNotes: List<String> = emptyList()
)

data class VirtualLabOutput(
    val observations: List<String>,
    val metrics: Map<String, String>,
    val interpretation: String,
    val visual: LabVisualData
)

data class LabExperimentDef(
    val id: String,
    val category: String, // Immunology, Molecular Biology, Microbiology
    val title: String,
    val objective: String,
    val theory: String,
    val equipment: List<String>,
    val procedure: List<String>,
    val params: List<LabParamDef>,
    val limitations: String,
    val question: String,
    val answerKey: String
)

private class SeededRng(seed: Long) {
    private var state = seed and 0x7fffffffL
    fun nextDouble(): Double {
        state = (state * 48271L) % 2147483647L
        return state / 2147483647.0
    }
    fun gaussian(): Double {
        val u1 = nextDouble().coerceAtLeast(1e-6)
        val u2 = nextDouble()
        return sqrt(-2.0 * ln(u1)) * kotlin.math.cos(2.0 * Math.PI * u2)
    }
}

object VirtualLabEngine {
    val EXPERIMENTS = listOf(
        // IMMUNOLOGY (8)
        LabExperimentDef("elisa", "Immunology", "Sandwich ELISA (4PL Standard Curve)", "Quantify unknown cytokine concentration using a 4-parameter logistic enzyme immunoassay.", "Capture mAb immobilizes antigen; HRP-conjugated detection mAb oxidizes TMB substrate proportional to bound analyte.", listOf("96-well high-binding plate", "Capture/Detection antibodies", "TMB substrate & H2SO4 stop", "450 nm plate reader"), listOf("Coat plate with capture antibody & block with BSA.", "Add serial standards and unknown sample.", "Incubate biotinylated detection mAb + streptavidin-HRP.", "Develop TMB and read OD450."), listOf(LabParamDef("conc", "True Sample Conc (Simulated)", 5f, 400f, 110f, "pg/mL"), LabParamDef("noise", "Pipetting CV Noise", 0f, 0.15f, 0.04f)), "Simulated 4PL fits ignore matrix hook effects and plate edge drift.", "Why does high-dose 'hook effect' suppress apparent signal?", "Excess free antigen saturates capture and detection antibodies separately before sandwich formation."),
        LabExperimentDef("western", "Immunology", "Western Blot Immunodetection", "Verify molecular weight and relative phosphorylation of target immune kinase.", "SDS-PAGE denatures proteins by mass; electrotransfer to PVDF allows specific primary/HRP antibody probing.", listOf("SDS-PAGE gel", "PVDF membrane", "Anti-phospho-STAT1 & Actin loading control", "Chemiluminescence imager"), listOf("Boil lysates in SDS + β-ME.", "Separate on 10% polyacrylamide gel and transfer.", "Block membrane and probe primary + HRP secondary."), listOf(LabParamDef("stim", "IFN-γ Stimulation Dose", 0f, 100f, 65f, "ng/mL"), LabParamDef("exposure", "Imager Exposure", 1f, 10f, 5f, "s")), "Normalized simulated band intensities do not replace biological replicates.", "Why include β-actin or GAPDH as a control?", "To normalize for equal total protein loading and transfer efficiency across lanes."),
        LabExperimentDef("immunofluorescence", "Immunology", "Immunofluorescence Microscopy", "Localize NF-κB p65 nuclear translocation upon LPS stimulation.", "Fluorophore-tagged antibodies emit wavelength-shifted photons revealing subcellular spatial localization.", listOf("Fixed macrophages", "Anti-p65 Alexa488 + DAPI nuclear stain", "Fluorescence microscope"), listOf("Fix and permeabilize cells.", "Stain p65 (green) and nuclei with DAPI (blue).", "Compare resting vs TLR4-stimulated nuclear overlap."), listOf(LabParamDef("lps", "LPS Dose", 0f, 100f, 75f, "ng/mL"), LabParamDef("time", "Incubation Time", 5f, 60f, 30f, "min")), "Illustrative spatial intensities; ignores photobleaching and autofluorescence.", "What confirms NF-κB activation visually?", "Shift of green p65 signal from cytoplasm into DAPI-positive blue nuclei."),
        LabExperimentDef("ihc", "Immunology", "Immunohistochemistry (CD3/PD-L1)", "Score simulated tumor-infiltrating T-cell density and PD-L1 DAB chromogen staining.", "Enzyme-linked polymer converts DAB into brown insoluble precipitate at antigen sites within FFPE tissue sections.", listOf("FFPE tissue slides", "Anti-CD3 / Anti-PD-L1 primary Ab", "DAB chromogen + Hematoxylin counterstain"), listOf("Deparaffinize and perform heat-induced epitope retrieval.", "Block endogenous peroxidase.", "Apply primary antibody, HRP polymer, DAB, and hematoxylin."), listOf(LabParamDef("infil", "CD3+ TIL Density", 5f, 95f, 58f, "%"), LabParamDef("pdl1", "Tumor PD-L1 Expression", 0f, 100f, 45f, "%")), "Educational TPS score simulator; clinical scoring requires pathologist validation.", "Why does heat-induced epitope retrieval improve staining?", "It breaks formalin methylene crosslinks that mask antigenic epitopes."),
        LabExperimentDef("flow", "Immunology", "Flow Cytometry (CD4 / CD8 Gating)", "Quantify CD4+ helper and CD8+ cytotoxic T-cell subsets in peripheral lymphocytes.", "Hydrodynamic focusing passes labeled single cells through lasers; fluorophore scatter resolves lineages.", listOf("Flow cytometer (488/640 nm lasers)", "Anti-CD4-FITC, Anti-CD8-APC", "Lymphocyte suspension"), listOf("Stain PBMCs with fluorescent antibody cocktail.", "Gate lymphocytes on FSC/SSC.", "Plot CD4 vs CD8 quadrants and inspect CD4:CD8 ratio."), listOf(LabParamDef("cd4pct", "Target CD4% of T cells", 10f, 75f, 56f, "%"), LabParamDef("events", "Event Count", 150f, 500f, 300f)), "Simulated Gaussian clusters represent gated lymphocytes only.", "What does a falling CD4:CD8 ratio (<1.0) indicate?", "Selective CD4+ T-cell depletion or relative CD8+ cytotoxic expansion."),
        LabExperimentDef("agglutination", "Immunology", "ABO / Rh Blood Group Agglutination", "Interpret visible hemagglutination patterns against anti-A, anti-B, and anti-D reagents.", "Pentameric IgM anti-A/B crosslinks multivalent RBC surface oligosaccharides into visible clumps.", listOf("Anti-A, Anti-B, Anti-D antisera", "Glass typing slides", "2% RBC suspension"), listOf("Place 3 drops of RBC suspension on slide.", "Add anti-A, anti-B, anti-D reagents separately.", "Mix gently and inspect clumping at 2 minutes."), listOf(LabParamDef("abo", "Blood Phenotype (0=O,1=A,2=B,3=AB)", 0f, 3f, 1f), LabParamDef("rh", "Rh(D) (0=Neg, 1=Pos)", 0f, 1f, 1f)), "Educational rule-based typing simulator; never for pre-transfusion crossmatching.", "Why does IgM agglutinate RBCs more efficiently than IgG?", "Pentameric IgM spans ~30 nm distances between negatively charged RBCs."),
        LabExperimentDef("complement", "Immunology", "Complement Fixation Assay", "Detect patient serum antibodies by measuring whether classical complement is consumed.", "If patient antibody binds test antigen, C1q–C9 is fixed and unavailable to lyse indicator antibody-coated sheep RBCs.", listOf("Test antigen & heat-inactivated serum", "Guinea pig complement", "Sensitized sheep RBC indicator"), listOf("Heat-inactivate serum at 56°C to destroy endogenous complement.", "Incubate serum + antigen + measured complement.", "Add sensitized sheep RBCs and score hemolysis."), listOf(LabParamDef("titer", "Antigen-Specific Ab Titer", 0f, 100f, 72f)), "Simulated lysis percentage; modern diagnostics prefer ELISA/chemiluminescence.", "In a positive test, why do indicator sheep RBCs remain unlysed?", "The test antigen–antibody complex consumed all complement during step 1."),
        LabExperimentDef("precipitin", "Immunology", "Heidelberger–Kendall Precipitin Curve", "Map lattice precipitation across antibody excess (prozone), equivalence, and antigen excess.", "Optimal crosslinking occurs at equimolar valence (equivalence zone); excess of either partner shrinks complexes.", listOf("Serial antigen dilutions", "Fixed polyclonal antiserum", "Nephelometer / turbidimeter"), listOf("Prepare serial twofold antigen dilutions.", "Add constant polyclonal antiserum.", "Measure precipitate turbidity across zones."), listOf(LabParamDef("abVal", "Antibody Equivalence Peak", 20f, 80f, 50f)), "Conceptual equilibrium curve.", "Why is there minimal precipitate in extreme antigen excess?", "Free antigens saturate every Fab arm independently, preventing multivalent lattice formation."),

        // MOLECULAR BIOLOGY (8)
        LabExperimentDef("pcr", "Molecular Biology", "End-Point PCR Amplification", "Simulate exponential dsDNA amplification across denaturation, annealing, and extension cycles.", "Thermostable Taq polymerase extends primers every cycle, approaching plateau as dNTPs and primers deplete.", listOf("Thermal cycler", "Taq polymerase + dNTPs + MgCl2", "Forward/Reverse primers"), listOf("Denature at 95°C, anneal at primer Tm-3°C, extend at 72°C.", "Repeat for 25–40 cycles.", "Resolve product size on agarose gel."), listOf(LabParamDef("cycles", "Cycle Count", 15f, 40f, 30f), LabParamDef("eff", "Cycle Efficiency", 0.65f, 1.0f, 0.94f)), "Deterministic logistic saturation model.", "Why does amplification plateau in late cycles?", "Primer depletion, pyrophosphate buildup, and product strand re-annealing."),
        LabExperimentDef("qpcr", "Molecular Biology", "Quantitative RT-qPCR (ΔΔCt)", "Calculate fold-change cytokine mRNA expression relative to reference housekeeping gene.", "Fluorescent SYBR/TaqMan signal crosses threshold at cycle Ct inversely proportional to log2 initial copies.", listOf("Real-time qPCR cycler", "SYBR Green master mix", "Target & GAPDH primers"), listOf("Normalize target Ct to housekeeping Ct (ΔCt).", "Subtract control ΔCt from stimulated ΔCt (ΔΔCt).", "Compute 2^(-ΔΔCt) fold expression."), listOf(LabParamDef("fold", "True Transcript Induction", 1f, 32f, 8f, "x"), LabParamDef("eff", "Reaction Efficiency", 0.80f, 1.05f, 0.98f)), "Assumes 100% primer specificity and single melt peak.", "What does a ΔΔCt of -3 represent?", "A 2^3 = 8-fold increase in target transcript abundance."),
        LabExperimentDef("gel", "Molecular Biology", "Agarose Gel Electrophoresis", "Separate DNA restriction/PCR fragments inversely proportional to log10(base pairs).", "Phosphodiester negative charge drives migration toward anode; agarose pores sieve smaller fragments faster.", listOf("1.5% Agarose gel + SYBR Safe", "100 bp / 1 kb DNA Ladder", "Electrophoresis chamber (100 V)"), listOf("Load DNA ladder and samples into wells.", "Run at 100 V in TAE buffer.", "Visualize bands under blue/UV transilluminator."), listOf(LabParamDef("agarose", "Agarose %", 0.7f, 2.5f, 1.5f, "%"), LabParamDef("fragBp", "Target Amplicon Size", 150f, 2500f, 650f, "bp")), "Simulated log-linear mobility; supercoiled plasmids deviate from linear ladders.", "How does raising agarose % affect small fragments?", "Tighter gel pores improve resolution of small fragments (<500 bp) while slowing large DNA."),
        LabExperimentDef("dna_ext", "Molecular Biology", "Genomic DNA Silica Extraction", "Model lysis, RNase digestion, chaotropic silica binding, wash, and TE elution yield.", "Guanidinium salts disrupt hydration shells so DNA adsorbs selectively to silica membranes in ethanol.", listOf("Silica spin column", "Proteinase K + RNase A", "Lysis, wash, and elution buffers"), listOf("Lyse cells with detergent + Proteinase K.", "Bind DNA to silica column under chaotropic ethanol.", "Wash salts away and elute pure dsDNA (A260/A280 ~1.8)."), listOf(LabParamDef("cells", "Input Cell Mass", 1f, 10f, 5f, "x10^6"), LabParamDef("wash", "Ethanol Wash Thoroughness", 0.5f, 1.0f, 0.95f)), "Simulated spectrophotometer yield and A260/A280 purity ratios.", "What does an A260/A230 ratio below 1.5 suggest?", "Carryover of guanidine salts, ethanol, or phenol contaminants."),
        LabExperimentDef("rna_ext", "Molecular Biology", "Total RNA Extraction & RIN Quality", "Simulate acid guanidinium extraction and 28S/18S rRNA integrity evaluation.", "RNase-free cold handling prevents hydrolysis of labile single-stranded transcripts (A260/A280 ~2.0).", listOf("TRIzol / spin column", "RNase-free water & tips", "Bioanalyzer micro-capillary"), listOf("Homogenize sample in RNase-inactivating reagent.", "Phase-separate / bind RNA and treat with DNase I.", "Verify 28S:18S rRNA ratio and RIN score."), listOf(LabParamDef("rnase", "RNase Contamination Level", 0f, 1f, 0.1f)), "Educational RIN simulator.", "Why is DNase treatment essential before RT-qPCR?", "Residual genomic DNA causes false amplification if primers do not span exon junctions."),
        LabExperimentDef("sanger", "Molecular Biology", "Sanger Dideoxy Sequencing", "Inspect dye-terminator chromatogram peaks and base-call quality scores.", "2',3'-dideoxynucleotides terminate chain elongation; capillary electrophoresis reads fluorescently labeled fragment lengths.", listOf("Capillary sequencer", "BigDye ddNTP mix", "Template + sequencing primer"), listOf("Run cycle sequencing with fluorescent ddNTPs.", "Separate fragments at 1-nt resolution.", "Call peaks A/C/G/T and inspect heterozygous overlap."), listOf(LabParamDef("het", "Heterozygous Peak Mix", 0f, 0.5f, 0.2f), LabParamDef("noise", "Baseline Noise", 0.01f, 0.25f, 0.05f)), "Simulated trace peaks; read lengths degrade past ~800 bp.", "How is a heterozygous SNV recognized in Sanger traces?", "Two overlapping colored dye peaks of ~50% height at the same nucleotide position."),
        LabExperimentDef("restriction", "Molecular Biology", "Restriction Mapping Analysis", "Digest a circular 4.8 kb plasmid with EcoRI and HindIII to deduce fragment sizes.", "Type II endonucleases cleave palindromic dsDNA sites cleanly, yielding diagnostic gel patterns.", listOf("Plasmid DNA (4,800 bp)", "EcoRI & HindIII enzymes", "1% Agarose gel"), listOf("Set up single and double enzyme digests at 37°C.", "Heat-inactivate enzymes.", "Compare fragment bands against 1 kb ladder."), listOf(LabParamDef("site2", "HindIII Cut Position", 600f, 4000f, 1750f, "bp")), "Idealized 100% digestion efficiency (no star activity).", "How many bands appear from a circular plasmid with 2 cut sites?", "Two linear bands whose lengths sum to total plasmid size."),
        LabExperimentDef("crispr", "Molecular Biology", "CRISPR-Cas9 Conceptual PAM Scan", "Identify candidate 5'-NGG-3' SpCas9 target sites and illustrate NHEJ vs HDR repair outcomes.", "Cas9 HNH/RuvC domains generate a blunt double-strand break 3 bp upstream of 5'-NGG-3' PAM.", listOf("Target exon DNA", "sgRNA design logic", "Conceptual repair readout"), listOf("Scan coding strand and reverse complement for NGG motifs.", "Check GC% (40–60%) of 20-nt protospacer.", "Compare NHEJ indels vs donor-templated HDR."), listOf(LabParamDef("nhejBias", "NHEJ Repair Probability", 0.4f, 0.95f, 0.80f)), "Strictly conceptual educational model; not an experimental gRNA clinical design tool.", "Why must the PAM (NGG) not be included inside a cloned HDR repair template?", "Mutating the PAM prevents Cas9 from re-cutting the successfully edited allele."),

        // MICROBIOLOGY (6)
        LabExperimentDef("microscopy", "Microbiology", "Brightfield Resolution & NA Limit", "Explore Abbe diffraction limit d = 0.61λ / NA across 10x, 40x, and 100x oil objectives.", "Shorter wavelength and higher numerical aperture (oil immersion n=1.51) sharply improve spatial resolving power.", listOf("Compound light microscope", "400–700 nm illumination", "Immersion oil (n=1.51)"), listOf("Select wavelength λ and objective NA.", "Compute Abbe minimum resolvable distance d.", "Inspect simulated resolution of cocci vs bacilli."), listOf(LabParamDef("na", "Objective NA", 0.25f, 1.40f, 1.25f), LabParamDef("lambda", "Wavelength (nm)", 400f, 680f, 520f, "nm")), "Theoretical diffraction calculation excluding spherical aberrations.", "Why does 100x oil immersion dramatically sharpen 1 µm bacteria?", "Oil matches glass refractive index, raising NA from ~0.95 to 1.25–1.40."),
        LabExperimentDef("micro_id", "Microbiology", "Biochemical Identification Key", "Differentiate Gram+ cocci and Gram- rods using catalase, coagulase, oxidase, and lactose.", "Diagnostic enzymatic reactions isolate metabolic signatures of medically relevant bacteria.", listOf("H2O2 catalase reagent", "Rabbit plasma coagulase", "Oxidase strips & TSI/MacConkey"), listOf("Determine Gram stain & morphology.", "Run catalase/coagulase (Gram+) or oxidase/lactose (Gram-).", "Match metabolic profile."), listOf(LabParamDef("speciesIdx", "Mystery Isolate Profile (0–3)", 0f, 3f, 0f)), "Simplified 4-isolate pedagogical key; real clinical ID uses MALDI-TOF/API panels.", "Which two tests rapidly separate S. aureus from S. pyogenes?", "Catalase (+) and coagulase (+) confirm S. aureus vs catalase (-) S. pyogenes."),
        LabExperimentDef("gram_stain", "Microbiology", "Gram Staining Simulator", "Simulate crystal violet, iodine mordant, decolorizer timing, and safranin counterstain.", "Over-decolorizing strips crystal violet from Gram-positives; under-decolorizing leaves Gram-negatives false purple.", listOf("Crystal violet & Gram's iodine", "95% Ethanol-acetone decolorizer", "Safranin counterstain"), listOf("Flood heat-fixed smear with crystal violet + iodine.", "Carefully decolorize for optimal 8–12 seconds.", "Counterstain with safranin and view under 100x oil."), listOf(LabParamDef("decolor", "Decolorizer Duration", 2f, 35f, 10f, "s"), LabParamDef("gramPosPct", "Gram+ Fraction in Mix", 0f, 100f, 50f, "%")), "Educational timing demonstration.", "What happens if alcohol decolorizer is left on too long (30+ s)?", "Gram-positive bacteria lose crystal violet and appear falsely pink (Gram-negative)."),
        LabExperimentDef("culture", "Microbiology", "Agar Culture Interpretation", "Interpret hemolysis on 5% Sheep Blood Agar and lactose fermentation on MacConkey Agar.", "α-hemolysis shows green partial clearing, β-hemolysis clear zones; MacConkey selects Gram-negatives and turns lactose+ pink.", listOf("5% Sheep Blood Agar", "MacConkey Agar", "37°C incubator"), listOf("Streak quadrants for isolated colonies.", "Inspect hemolytic halo on Blood Agar.", "Check pink/colorless colonies on MacConkey."), listOf(LabParamDef("profile", "Culture Sample (0=E.coli,1=S.pyogenes,2=P.aerug)", 0f, 2f, 0f)), "Simulated colony descriptions only.", "Why do Gram-positive cocci fail to grow on standard MacConkey agar?", "Bile salts and crystal violet selectively inhibit Gram-positive cell envelopes."),
        LabExperimentDef("antibiogram", "Microbiology", "Antibiotic Susceptibility (Disc Diffusion)", "Measure Kirby–Bauer inhibition halos across bacterial inoculum density and drug potency.", "Antimicrobial diffuses radially from paper disc; growth stops where concentration exceeds apparent MIC.", listOf("Mueller–Hinton agar plate", "Antibiotic discs (Amox, Cipro, Vanc)", "0.5 McFarland inoculum"), listOf("Lawn-streak standardized inoculum.", "Place discs and incubate 18 hours.", "Measure zone diameter in mm (illustrative only!)."), listOf(LabParamDef("resist", "Resistance Mechanism Strength", 0f, 1f, 0.25f)), "CRITICAL: Illustrative breakpoints only — NEVER use simulated zones for clinical prescribing.", "Why is 0.5 McFarland turbidity standardization mandatory?", "Overly dense lawns artificially shrink inhibition zones, mimicking resistance."),
        LabExperimentDef("growth_curve", "Microbiology", "Microbial Growth Kinetics (OD600)", "Model lag, exponential log, stationary, and death phases under varying nutrient and temp settings.", "Doubling time g governs exponential slope μ = ln(2)/g before nutrient limitation caps carrying capacity K.", listOf("LB broth flasks", "Spectrophotometer (600 nm)", "Shaking incubator"), listOf("Inoculate fresh broth from overnight culture.", "Sample OD600 every 30 min.", "Calculate generation time during log phase."), listOf(LabParamDef("temp", "Incubation Temp (°C)", 20f, 45f, 37f, "°C"), LabParamDef("nutrients", "Carbon/Nitrogen Supply", 0.3f, 2.0f, 1.0f)), "Monod-style educational OD curve.", "During which phase are β-lactam antibiotics most bactericidal?", "Exponential (log) phase when peptidoglycan cross-linking peaks.")
    )

    fun runExperiment(expId: String, paramValues: Map<String, Float>): VirtualLabOutput {
        val def = EXPERIMENTS.first { it.id == expId }
        fun p(key: String): Float = paramValues[key] ?: def.params.firstOrNull { it.key == key }?.defaultVal ?: 1f
        val rng = SeededRng((expId.hashCode().toLong() and 0xFFFF) + (paramValues.values.sum() * 100).toLong())

        return when (expId) {
            "elisa" -> {
                val trueConc = p("conc").toDouble()
                val noise = p("noise").toDouble()
                val stdConcs = listOf(0.0, 15.6, 31.25, 62.5, 125.0, 250.0, 500.0)
                fun fourPl(x: Double): Double = 0.08 + (2.65 - 0.08) / (1.0 + (95.0 / max(0.1, x)).pow(1.15))
                val curve = (1..25).map { i ->
                    val x = i * 20.0
                    Pair(x, fourPl(x))
                }
                val measOd = (fourPl(trueConc) + rng.gaussian() * noise).coerceIn(0.05, 3.2)
                val estConc = 95.0 * ((2.65 - measOd).coerceAtLeast(0.02) / (measOd - 0.08).coerceAtLeast(0.02)).pow(-1.0 / 1.15)
                VirtualLabOutput(
                    observations = listOf(
                        "Standard wells formed clean yellow TMB-acid gradient (OD450 0.08 → 2.41).",
                        "Unknown duplicate mean OD450 = ${"%.3f".format(measOd)}."
                    ),
                    metrics = mapOf(
                        "Measured OD450" to "%.3f".format(measOd),
                        "Back-calculated Conc" to "${"%.1f".format(estConc)} pg/mL",
                        "4PL EC50" to "95.0 pg/mL"
                    ),
                    interpretation = "Sample falls in the linear dynamic range of the 4PL curve (${stdConcs.first()}–500 pg/mL). Simulated educational output.",
                    visual = LabVisualData(mode = "curve", seriesA = curve, seriesB = listOf(Pair(estConc, measOd)))
                )
            }
            "flow" -> {
                val cd4Target = p("cd4pct").toInt().coerceIn(5, 85)
                val cd8Target = (90 - cd4Target).coerceIn(5, 80)
                val totalPts = p("events").toInt().coerceIn(100, 450)
                val pts = mutableListOf<Pair<Float, Float>>()
                repeat(totalPts) { idx ->
                    val frac = idx.toFloat() / totalPts * 100f
                    val (cx, cy) = when {
                        frac < cd4Target -> Pair(0.76f, 0.16f) // CD4+ CD8-
                        frac < cd4Target + cd8Target -> Pair(0.16f, 0.78f) // CD4- CD8+
                        else -> Pair(0.18f, 0.18f) // Double negative
                    }
                    val x = (cx + rng.gaussian().toFloat() * 0.07f).coerceIn(0.03f, 0.97f)
                    val y = (cy + rng.gaussian().toFloat() * 0.07f).coerceIn(0.03f, 0.97f)
                    pts += Pair(x, y)
                }
                val ratio = cd4Target.toDouble() / max(1, cd8Target)
                VirtualLabOutput(
                    observations = listOf(
                        "Gated $totalPts lymphocyte events on FSC-A vs SSC-A.",
                        "Q3 (CD4+CD8-): $cd4Target% | Q1 (CD4-CD8+): $cd8Target% | DN: ${100 - cd4Target - cd8Target}%."
                    ),
                    metrics = mapOf(
                        "CD4+ Helper %" to "$cd4Target%",
                        "CD8+ CTL %" to "$cd8Target%",
                        "CD4:CD8 Ratio" to "%.2f".format(ratio)
                    ),
                    interpretation = if (ratio < 1.0) "Inverted CD4:CD8 ratio (<1.0) observed in simulated chronic viral depletion or CTL expansion." else "Physiological CD4:CD8 ratio (~1.5–2.5) maintained.",
                    visual = LabVisualData(mode = "flow", flowPoints = pts)
                )
            }
            "gel" -> {
                val agar = p("agarose")
                val targetBp = p("fragBp").toInt()
                val ladder = listOf(100, 250, 500, 750, 1000, 1500, 2000, 3000)
                VirtualLabOutput(
                    observations = listOf(
                        "1 kb Ladder resolved cleanly across ${"%.1f".format(agar)}% agarose matrix.",
                        "Sample lane displays sharp single amplicon at ~$targetBp bp."
                    ),
                    metrics = mapOf(
                        "Agarose Matrix" to "${"%.1f".format(agar)}%",
                        "Target Band" to "$targetBp bp",
                        "Primer Dimer" to "Absent"
                    ),
                    interpretation = "Higher agarose concentration retards migration of $targetBp bp bands cleanly away from the bromophenol dye front.",
                    visual = LabVisualData(mode = "gel", gelLanes = listOf(ladder, listOf(targetBp), listOf(targetBp, targetBp / 2)))
                )
            }
            "restriction" -> {
                val cut = p("site2").toInt().coerceIn(400, 4400)
                val f1 = cut
                val f2 = 4800 - cut
                VirtualLabOutput(
                    observations = listOf(
                        "Lane 1: Ladder. Lane 2 (Uncut): supercoiled single band.",
                        "Lane 3 (Single cut): linearized 4,800 bp band.",
                        "Lane 4 (Double digest): fragments at $f1 bp and $f2 bp."
                    ),
                    metrics = mapOf("Plasmid Total" to "4,800 bp", "Fragment 1" to "$f1 bp", "Fragment 2" to "$f2 bp"),
                    interpretation = "Double digest fragments sum exactly to 4,800 bp, confirming EcoRI at 0 bp and HindIII at $cut bp.",
                    visual = LabVisualData(mode = "gel", gelLanes = listOf(listOf(500, 1000, 2000, 3000, 5000), listOf(4800), listOf(f1, f2)))
                )
            }
            "pcr" -> {
                val cycles = p("cycles").toInt()
                val eff = p("eff").toDouble()
                val curve = (1..cycles).map { c ->
                    val raw = 100.0 * (1.0 + eff).pow(c)
                    val sat = 1e9 * raw / (1e9 + raw)
                    Pair(c.toDouble(), ln(sat) / ln(10.0))
                }
                VirtualLabOutput(
                    observations = listOf("Exponential fluorescence amplification observed until reagent plateau near cycle ${ (cycles * 0.78).toInt() }."),
                    metrics = mapOf("Cycles" to "$cycles", "Efficiency" to "${(eff * 100).toInt()}%", "Final Log10 Copies" to "%.2f".format(curve.last().second)),
                    interpretation = "Log-linear amplification slope confirms efficient primer annealing; late plateau reflects dNTP depletion.",
                    visual = LabVisualData(mode = "curve", seriesA = curve)
                )
            }
            "qpcr" -> {
                val fold = p("fold").toDouble()
                val eff = p("eff").toDouble()
                val ctrlCt = 24.0
                val deltaCycles = ln(fold) / ln(1.0 + eff)
                val treatCt = (ctrlCt - deltaCycles).coerceAtLeast(10.0)
                val curveCtrl = (1..36).map { c -> Pair(c.toDouble(), 1.0 / (1.0 + exp(-(c - ctrlCt) * 1.1))) }
                val curveTreat = (1..36).map { c -> Pair(c.toDouble(), 1.0 / (1.0 + exp(-(c - treatCt) * 1.1))) }
                VirtualLabOutput(
                    observations = listOf(
                        "Reference GAPDH stable at Ct 18.2 ± 0.1 across conditions.",
                        "Stimulated target crossed threshold at Ct ${"%.2f".format(treatCt)} vs control Ct ${"%.2f".format(ctrlCt)}."
                    ),
                    metrics = mapOf("Control Ct" to "%.2f".format(ctrlCt), "Treated Ct" to "%.2f".format(treatCt), "Relative Fold (2^-ΔΔCt)" to "${"%.2f".format(fold)}x"),
                    interpretation = "Earlier Ct indicates higher baseline transcript abundance after stimulation.",
                    visual = LabVisualData(mode = "curve", seriesA = curveCtrl, seriesB = curveTreat)
                )
            }
            "precipitin" -> {
                val peak = p("abVal").toDouble()
                val pts = (1..30).map { i ->
                    val ag = i * 3.5
                    val diff = (ag - peak) / 22.0
                    Pair(ag, exp(-diff * diff) * 1.8)
                }
                VirtualLabOutput(
                    observations = listOf("Turbidity peaked sharply near antigen index ${peak.toInt()} (equivalence zone), dropping in prozone and postzone."),
                    metrics = mapOf("Equivalence Peak" to "${peak.toInt()} µg", "Peak Turbidity" to "1.80 OD"),
                    interpretation = "Optimal multivalent Ab–Ag crosslinking occurs exclusively near stoichiometric equivalence.",
                    visual = LabVisualData(mode = "curve", seriesA = pts)
                )
            }
            "growth_curve" -> {
                val temp = p("temp").toDouble()
                val nut = p("nutrients").toDouble()
                val tempFactor = exp(-((temp - 37.0) / 11.0).pow(2))
                val rate = 0.95 * tempFactor * nut
                val cap = 1.6 * nut.coerceIn(0.4, 1.8)
                val pts = (0..24).map { i ->
                    val hr = i * 0.5
                    val od = cap / (1.0 + ((cap - 0.05) / 0.05) * exp(-rate * hr))
                    Pair(hr, od)
                }
                val dblMin = 60.0 * ln(2.0) / max(0.05, rate)
                VirtualLabOutput(
                    observations = listOf("Lag phase resolved by ~1.5 h; exponential growth reached doubling time ${dblMin.toInt()} min before stationary plateau."),
                    metrics = mapOf("Doubling Time" to "${dblMin.toInt()} min", "Max OD600" to "%.2f".format(pts.last().second), "Temp Opt Factor" to "%.2f".format(tempFactor)),
                    interpretation = "Deviations from 37°C or carbon limitation reduce growth rate μ and terminal biomass.",
                    visual = LabVisualData(mode = "curve", seriesA = pts)
                )
            }
            "gram_stain" -> {
                val dec = p("decolor")
                val gPosPct = p("gramPosPct").toInt()
                val overDecolorized = dec > 20f
                val underDecolorized = dec < 5f
                val status = when {
                    overDecolorized -> "Over-decolorized (>20 s): Gram+ cocci stripped and stained falsely pink!"
                    underDecolorized -> "Under-decolorized (<5 s): Gram- rods retained purple falsely!"
                    else -> "Optimal decolorization: Gram+ cocci violet/purple; Gram- bacilli pink/red."
                }
                VirtualLabOutput(
                    observations = listOf(status, "Simulated field mix: $gPosPct% Gram+ cocci, ${100 - gPosPct}% Gram- bacilli."),
                    metrics = mapOf("Decolorizer" to "${dec.toInt()} s", "Stain Validity" to if (overDecolorized || underDecolorized) "Artifactual" else "Valid"),
                    interpretation = "Alcohol-acetone timing is the critical technical determinant of Gram stain accuracy.",
                    visual = LabVisualData(mode = "microscopy", extraNotes = listOf(if (overDecolorized) "ALL_PINK" else if (underDecolorized) "ALL_PURPLE" else "MIXED"))
                )
            }
            "agglutination" -> {
                val abo = p("abo").toInt()
                val rh = p("rh").toInt() == 1
                val hasA = abo == 1 || abo == 3
                val hasB = abo == 2 || abo == 3
                val label = listOf("O", "A", "B", "AB")[abo.coerceIn(0, 3)] + if (rh) "+" else "-"
                VirtualLabOutput(
                    observations = listOf(
                        "Anti-A well: ${if (hasA) "AGGLUTINATION (+)" else "Smooth (-)"}",
                        "Anti-B well: ${if (hasB) "AGGLUTINATION (+)" else "Smooth (-)"}",
                        "Anti-D well: ${if (rh) "AGGLUTINATION (+)" else "Smooth (-)"}"
                    ),
                    metrics = mapOf("Interpreted Blood Type" to label, "Anti-A" to if (hasA) "+" else "-", "Anti-B" to if (hasB) "+" else "-"),
                    interpretation = "Visible RBC lattice formation reflects IgM/IgD crosslinking of membrane A, B, or RhD epitopes.",
                    visual = LabVisualData(mode = "steps", extraNotes = listOf("Anti-A: ${if (hasA) "Clumped" else "Clear"}", "Anti-B: ${if (hasB) "Clumped" else "Clear"}", "Anti-D: ${if (rh) "Clumped" else "Clear"}"))
                )
            }
            "antibiogram" -> {
                val res = p("resist")
                val amoxZone = (26 - res * 18).toInt().coerceAtLeast(6)
                val ciproZone = (31 - res * 12).toInt().coerceAtLeast(6)
                val vancZone = (21 - res * 4).toInt().coerceAtLeast(6)
                VirtualLabOutput(
                    observations = listOf(
                        "Illustrative inhibition diameters: Drug A $amoxZone mm | Drug B $ciproZone mm | Drug C $vancZone mm.",
                        "Educational resistance stress set to ${(res * 100).toInt()}%."
                    ),
                    metrics = mapOf("Drug A Halo" to "$amoxZone mm (Illustrative)", "Drug B Halo" to "$ciproZone mm", "Clinical Use" to "Prohibited (Demo)"),
                    interpretation = "Higher enzymatic β-lactamase / efflux expression shrinks diffusion zones. Never apply to clinical prescribing.",
                    visual = LabVisualData(
                        mode = "curve",
                        seriesA = listOf(1.0 to amoxZone.toDouble(), 2.0 to ciproZone.toDouble(), 3.0 to vancZone.toDouble())
                    )
                )
            }
            "microscopy" -> {
                val na = p("na").toDouble()
                val lam = p("lambda").toDouble()
                val dNm = (0.61 * lam) / na
                VirtualLabOutput(
                    observations = listOf("Abbe diffraction resolution limit = ${"%.0f".format(dNm)} nm (0.${"%02d".format((dNm / 10).toInt())} µm)."),
                    metrics = mapOf("Resolution d" to "${"%.0f".format(dNm)} nm", "Numerical Aperture" to "%.2f".format(na), "1 µm Cocci Clear?" to if (dNm < 350) "Crisp" else "Blurred"),
                    interpretation = "Increasing NA with oil immersion (1.25–1.40) and shorter blue-green light resolves sub-micron bacteria.",
                    visual = LabVisualData(mode = "microscopy", extraNotes = listOf(if (dNm < 350) "CRISP" else "BLUR"))
                )
            }
            else -> {
                val paramSummary = def.params.joinToString(", ") { "${it.label}=${"%.2f".format(p(it.key))}${it.unit}" }
                val stepCurve = def.procedure.mapIndexed { idx, _ -> Pair((idx + 1).toDouble(), (idx + 1) * 28.0 + p(def.params.first().key) * 0.1) }
                val observations = when (expId) {
                    "western" -> listOf(
                        "Phospho-STAT1 (91 kDa) band intensity scaled to ${(p("stim") * 1.1f).toInt()}% vs unstimulated baseline.",
                        "β-Actin (42 kDa) loading control remained uniform across all lanes."
                    )
                    "immunofluorescence" -> listOf(
                        "Nuclear-to-cytoplasmic p65 colocalization rose to ${((p("lps") * p("time") / 60f)).coerceIn(8f, 96f).toInt()}% overlapping DAPI."
                    )
                    "ihc" -> listOf(
                        "Brown DAB membrane staining observed in ${p("pdl1").toInt()}% tumor cells; ${p("infil").toInt()}% stromal CD3+ lymphocytes."
                    )
                    "complement" -> {
                        val titer = p("titer").toInt()
                        listOf(
                            if (titer > 35) "Positive complement fixation: indicator sheep RBCs remained intact (0–15% lysis)." else "Negative fixation: free complement lysed indicator RBCs (>80% hemolysis)."
                        )
                    }
                    "dna_ext" -> {
                        val cells = p("cells")
                        val wash = p("wash")
                        listOf("Yield ~${"%.1f".format(cells * 6.4 * wash)} µg dsDNA | A260/A280 = ${"%.2f".format(1.65 + 0.2 * wash)}.")
                    }
                    "rna_ext" -> {
                        val r = p("rnase")
                        val rin = (9.6f - r * 6.5f).coerceIn(2.0f, 10.0f)
                        listOf("28S/18S rRNA ratio ${"%.2f".format(2.1f - r * 1.5f)} | Simulated RIN = ${"%.1f".format(rin)}/10.")
                    }
                    "sanger" -> listOf("Clean 4-color capillary peaks resolved; heterozygous secondary peak at ${(p("het") * 100).toInt()}% amplitude.")
                    "crispr" -> listOf("Identified 4 candidate 5'-NGG-3' PAM sites; predicted NHEJ indel rate ${(p("nhejBias") * 100).toInt()}% vs HDR ${(100 - (p("nhejBias") * 100).toInt())}%.")
                    "micro_id" -> {
                        val profiles = listOf(
                            "Gram+ cluster cocci, Catalase+, Coagulase+ -> Simulated S. aureus profile.",
                            "Gram+ chain cocci, Catalase-, Beta-hemolytic -> Simulated S. pyogenes profile.",
                            "Gram- rod, Lactose+ pink on MacConkey, Oxidase- -> Simulated E. coli profile.",
                            "Gram- rod, Lactose-, Oxidase+, blue-green pigment -> Simulated P. aeruginosa profile."
                        )
                        listOf(profiles[p("speciesIdx").toInt().coerceIn(0, 3)])
                    }
                    "culture" -> {
                        val plates = listOf(
                            "MacConkey: bright pink lactose-fermenting colonies; Blood agar: non-hemolytic grey.",
                            "Blood agar: pinpoint colonies with sharp clear β-hemolytic halos; MacConkey: no growth.",
                            "Blood agar: metallic sheen / β-hemolysis; MacConkey: colorless non-lactose fermenter."
                        )
                        listOf(plates[p("profile").toInt().coerceIn(0, 2)])
                    }
                    else -> listOf("Completed simulated protocol with parameters: $paramSummary")
                }
                VirtualLabOutput(
                    observations = observations,
                    metrics = def.params.associate { it.label to "${"%.2f".format(p(it.key))} ${it.unit}".trim() } + mapOf("Status" to "Simulated Assay Complete"),
                    interpretation = def.theory + " (" + def.limitations + ")",
                    visual = LabVisualData(mode = "curve", seriesA = stepCurve, extraNotes = observations)
                )
            }
        }
    }
}
