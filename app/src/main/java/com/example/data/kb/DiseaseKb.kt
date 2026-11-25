package com.example.data.kb

data class DiseaseProfile(
    val id: String,
    val name: String,
    val category: String,
    val agent: String,
    val transmission: String,
    val overview: String,
    val pathophysiology: String,
    val epidemiology: String,
    val clinicalManifestations: String,
    val immunoMechanism: String,
    val diagnostics: String,
    val prevention: String,
    val treatment: String,
    val research: String,
    val refIds: List<String>
)

data class HivStage(
    val step: Int,
    val title: String,
    val detail: String,
    val targetClass: String,
    val status: String
)

data class HivTopic(
    val id: String,
    val title: String,
    val status: String,
    val summary: String,
    val refId: String
)

object DiseaseKb {
    val DISEASES = listOf(
        DiseaseProfile(
            "hiv", "HIV / AIDS", "Viral Retrovirus", "HIV-1 / HIV-2 (Lentivirus)",
            "Blood, sexual contact, perinatal / vertical.",
            "Lentivirus targeting CD4+ T cells, macrophages, and dendritic cells, causing progressive cellular immunodeficiency if untreated.",
            "Env gp120 binds CD4 and CCR5/CXCR4; RNA genome reverse-transcribes into proviral DNA and integrates into host chromosomes.",
            "~39M people living with HIV globally; chronic manageable condition with uninterrupted ART.",
            "Acute retroviral syndrome -> prolonged clinical latency -> opportunistic infections/neoplasms if CD4 <200/µL.",
            "Direct viral cytopathy, pyroptosis of abortively infected bystander CD4+ cells, gut GALT depletion, chronic immune activation, and latent proviral reservoirs.",
            "4th-gen Ag/Ab combo (p24 + IgG/IgM), confirmatory HIV-1/2 differentiation, plasma HIV RNA viral load & CD4 count.",
            "Condoms, TasP (U=U), PrEP (oral TDF/FTC or long-acting cabotegravir), blood screening, PEP.",
            "Combination Antiretroviral Therapy (cART: NRTIs, INSTIs, NNRTIs, PIs, capsid inhibitors) suppresses viremia to undetectable.",
            "Broadly neutralizing antibodies (bNAbs), latency-reversing agents ('shock and kill'), block-and-lock, stem-cell CCR5Δ32 insights.",
            listOf("abbas", "hiv")
        ),
        DiseaseProfile(
            "tb", "Tuberculosis", "Mycobacterial", "Mycobacterium tuberculosis",
            "Airborne respiratory droplet nuclei.",
            "Chronic intracellular bacterial infection primarily of lungs capable of latent dormancy or active pulmonary/extrapulmonary disease.",
            "Alveolar macrophages phagocytose bacilli; ESAT-6/mycolic-acid lipids inhibit phagosome-lysosome fusion.",
            "Leading infectious cause of morbidity worldwide; ~1/4 global population estimates for latent TB sensitization.",
            "Productive cough, night sweats, weight loss, hemoptysis, apical cavitary lesions or miliary spread.",
            "IL-12/IFN-γ Th1–macrophage axis walls bacilli inside organized granulomas; TNF-α sustains granuloma integrity.",
            "Sputum AFB smear, Xpert MTB/RIF molecular NAAT, mycobacterial culture, IGRA / Mantoux TST.",
            "BCG vaccine (protects infants from disseminated/meningeal TB), airborne isolation, LTBI therapy.",
            "Standard RIPE regimen (Rifampin, Isoniazid, Pyrazinamide, Ethambutol); bedaquiline/pretomanid for MDR-TB.",
            "Host-directed therapies, next-gen mucosal/subunit TB vaccines (M72/AS01E).",
            listOf("carroll", "abbas")
        ),
        DiseaseProfile(
            "malaria", "Malaria", "Protozoan Parasite", "Plasmodium falciparum, vivax, ovale, malariae, knowlesi",
            "Female Anopheles mosquito bite.",
            "Erythrocytic and hepatic protozoan infection causing cyclical fever, hemolysis, and microvascular sequestration.",
            "Sporozoites infect hepatocytes -> merozoites invade RBCs -> PfEMP1 mediates endothelial cytoadherence causing cerebral/placental ischemia.",
            "Endemic across sub-Saharan Africa, South Asia, and parts of Latin America; children <5 face highest severe risk.",
            "Periodic paroxysms (chills, high fever, sweats), hemolytic anemia, splenomegaly, cerebral malaria.",
            "Var-gene antigenic variation evades antibodies; splenic clearance + IFN-γ/cytophilic IgG1/IgG3 slowly build partial premunition.",
            "Thick/thin Giemsa blood smear (gold standard), HRP2/pLDH rapid diagnostic tests, PCR.",
            "Insecticide-treated bed nets, chemoprevention, RTS,S and R21/Matrix-M vaccines.",
            "Artemisinin-based combination therapies (ACTs); IV artesunate for severe malaria; primaquine/tafenoquine for hypnozoites.",
            "Transmission-blocking mAbs targeting Pfs25/Pfs230 and blood-stage RH5 vaccines.",
            listOf("carroll", "janeway")
        ),
        DiseaseProfile(
            "hep", "Viral Hepatitis (A–E)", "Viral Hepatic", "HAV, HBV, HCV, HDV, HEV",
            "Fecal-oral (HAV/HEV); parenteral/sexual/perinatal (HBV/HCV/HDV).",
            "Hepatotropic viruses causing acute self-limited inflammation (HAV/HEV) or chronic cirrhosis/HCC (HBV/HCV).",
            "Non-cytopathic HBV/HCV injury results largely from host CD8+ CTL destruction of infected hepatocytes.",
            "Hundreds of millions globally carry chronic HBV (cccDNA persistence) or HCV.",
            "Jaundice, elevated ALT/AST, dark urine, fatigue; chronic fibrosis progressing to portal hypertension.",
            "CD8+ T-cell exhaustion (PD-1/LAG-3) and Treg expansion allow persistent HBV cccDNA; HCV mutates hypervariable E2 epitopes.",
            "Serology (HBsAg, anti-HBs, anti-HBc, anti-HCV) + quantitative HBV DNA / HCV RNA PCR.",
            "Recombinant HBsAg vaccine, HAV vaccine, universal birth-dose HBV prophylaxis, safe blood/needle practices.",
            "Direct-acting antivirals (DAAs) cure >95% of HCV; tenofovir/entecavir suppress HBV replication.",
            "HBV cccDNA silencing (CRISPR/siRNA) and therapeutic T-cell vaccines for functional HBV cure.",
            listOf("carroll", "abbas")
        ),
        DiseaseProfile(
            "flu", "Influenza", "Viral Respiratory", "Influenza A & B (Orthomyxovirus)",
            "Respiratory droplets, aerosols, fomites.",
            "Segmented negative-sense ssRNA respiratory virus undergoing rapid antigenic drift and occasional pandemic shift.",
            "Hemagglutinin (HA) binds sialic acid on airway epithelium; neuraminidase (NA) cleaves sialic acid for virion release.",
            "Seasonal winter epidemics globally with periodic zoonotic reassortment pandemics.",
            "Abrupt fever, myalgia, dry cough, malaise; secondary bacterial pneumonia (S. aureus, S. pneumoniae).",
            "Type I/III interferons limit early spread; neutralizing anti-HA stem/head IgG and CD8+ Trm mediate clearance.",
            "Multiplex RT-PCR respiratory panel; rapid antigen assays.",
            "Annual multivalent seasonal vaccines updated for HA/NA drift; hand/respiratory hygiene.",
            "Neuraminidase inhibitors (oseltamivir), cap-dependent endonuclease inhibitor (baloxavir).",
            "Universal HA-stalk and mRNA multivalent conserved-epitope influenza vaccines.",
            listOf("janeway", "carroll")
        ),
        DiseaseProfile(
            "covid", "COVID-19", "Viral Respiratory", "SARS-CoV-2 (Betacoronavirus)",
            "Respiratory aerosols and droplets.",
            "Enveloped positive-sense ssRNA virus entering via Spike–ACE2 engagement with TMPRSS2 cleavage.",
            "Alveolar injury coupled with delayed early type I IFN evasion and hyperinflammatory IL-6/macrophage activation in severe cases.",
            "Endemic global respiratory circulation with evolving Spike RBD Omicron sublineages.",
            "Upper respiratory symptoms, anosmia, dyspnea, hypoxemia, microvascular thrombosis, post-acute sequelae.",
            "Broad spike-neutralizing IgG/IgA plus conserved nucleocapsid/non-structural CD4+/CD8+ T-cell memory.",
            "RT-PCR, lateral-flow nucleocapsid rapid antigen tests.",
            "mRNA, protein-subunit, and viral-vector vaccines; ventilation and respirators.",
            "Nirmatrelvir/ritonavir (Mpro inhibitor), remdesivir; dexamethasone and anti-IL-6 (tocilizumab) in severe hypoxemic inflammation.",
            "Pan-sarbecovirus mucosal IgA vaccines and long-COVID immune-signature endotyping.",
            listOf("abbas", "epmc")
        ),
        DiseaseProfile(
            "dengue", "Dengue Fever", "Arboviral", "DENV-1 to DENV-4 (Flavivirus)",
            "Aedes aegypti and Aedes albopictus mosquitoes.",
            "Tropical flavivirus where secondary heterotypic infection risks severe plasma leakage via antibody-dependent enhancement.",
            "Sub-neutralizing cross-reactive IgG binds heterotypic DENV and facilitates FcγR-mediated entry into monocytes/macrophages (ADE).",
            "~100–400M infections annually in tropical/subtropical regions.",
            "'Breakbone' fever, retro-orbital pain, rash, thrombocytopenia; warning signs: plasma leakage, shock.",
            "Heterotypic original antigenic sin + FcγR internalization amplifies cytokine storm and endothelial glycocalyx disruption.",
            "NS1 antigen ELISA (days 1–5), RT-PCR, IgM/IgG seroconversion.",
            "Vector control, TAK-003 (Qdenga) and Dengvaxia (serostatus-restricted) vaccines.",
            "Supportive cautious isotonic fluid resuscitation; avoid NSAIDs/aspirin due to bleeding risk.",
            "NS4B protease inhibitors and serotype-balanced tetravalent mRNA candidates.",
            listOf("janeway", "carroll")
        ),
        DiseaseProfile(
            "ebola", "Ebola Virus Disease", "Viral Filovirus", "Orthoebolavirus zairense / sudanense",
            "Direct contact with infected bodily fluids (high biosafety).",
            "Severe hemorrhagic filovirus blocking innate interferon signaling and triggering disseminated microvascular failure.",
            "VP35 blocks RIG-I/MDA5 sensing; VP24 blocks STAT1 nuclear import; soluble GP acts as an antibody decoy.",
            "Sporadic Central/West African outbreaks from wildlife spillover amplified in unprotected care settings.",
            "Abrupt fever, severe GI fluid loss, coagulopathy, shock, multiorgan dysfunction.",
            "Rapid macrophage/DC infection sparks massive TNF-α/IL-6/nitric oxide release alongside massive lymphocyte apoptosis.",
            "BSL-4 / specialized RT-PCR and antigen capture assays.",
            "rVSV-ZEBOV (Ervebo) vaccine, strict PPE, ring vaccination, safe burials.",
            "Aggressive IV electrolyte support plus anti-GP monoclonal antibodies (Inmazeb, Ebanga).",
            "Pan-filovirus small-molecule polymerase inhibitors and cross-reactive GP mAb cocktails.",
            listOf("carroll", "abbas")
        ),
        DiseaseProfile(
            "bacterial", "Bacterial Infections & Sepsis", "Bacterial", "Gram+ (S. aureus, S. pyogenes) & Gram- (E. coli, P. aeruginosa)",
            "Respiratory, fecal-oral, skin breach, vector, nosocomial.",
            "Diverse extracellular and intracellular bacteria cleared by complement, neutrophils, and opsonizing antibodies.",
            "Endotoxin (LPS via TLR4) or superantigens (TSST-1 crosslinking TCRβ-MHC II) can trigger life-threatening septic shock.",
            "Sepsis accounts for ~20% of global mortality; AMR increasingly complicates hospital care.",
            "Localized abscess/pneumonia/UTI up to hypotension, hyperlactatemia, and organ dysfunction.",
            "Complement C3b + IgG opsonization -> neutrophil oxidative burst; IgA guards mucosal barriers.",
            "Gram stain, aerobic/anaerobic cultures, procalcitonin, MALDI-TOF/NAAT.",
            "Conjugate vaccines (PCV, Hib, MenACWY), sterile asepsis, antibiotic stewardship.",
            "Targeted beta-lactams, glycopeptides, or carbapenems guided by susceptibility testing + source control.",
            "Phage therapy, anti-virulence monoclonals, and rapid phenotypic microfluidics.",
            listOf("carroll", "abbas")
        ),
        DiseaseProfile(
            "autoimmune", "Autoimmune Diseases", "Immune Dysregulation", "Breakdown of Central or Peripheral Self-Tolerance",
            "Non-communicable; polygenic HLA susceptibility + environmental triggers.",
            "Pathogenic T/B cells or autoantibodies injure self tissues (e.g., Type 1 Diabetes, SLE, Rheumatoid Arthritis, MS).",
            "Failure of AIRE thymic deletion, Treg suppression, or efferocytosis leads to Type II/III/IV hypersensitivity tissue destruction.",
            "Affects ~5–8% of populations globally, frequently showing female predominance.",
            "Joint synovitis (RA), malar rash/nephritis (SLE), demyelination (MS), insulitis (T1D).",
            "HLA-DR/DQ alleles shape autoreactive peptide repertoire; epitope spreading amplifies chronic tissue destruction.",
            "Autoantibody panels (ANA, anti-dsDNA, anti-CCP, RF), ESR/CRP, complement C3/C4 consumption.",
            "No universal prevention; smoking cessation lowers citrullinated RA risk.",
            "DMARDs, biologics (anti-TNF, anti-IL-6, anti-CD20, CTLA4-Ig), JAK inhibitors, targeted CAR-T (investigational in SLE).",
            "Antigen-specific tolerogenic nanoparticles and CD19 CAR-T resetting autoreactive B-cell compartments.",
            listOf("abbas", "kuby")
        ),
        DiseaseProfile(
            "pid", "Primary Immunodeficiencies (IEIs)", "Genetic Immune Defect", "Monogenic Inborn Errors of Immunity (>480 genes)",
            "Hereditary (X-linked, autosomal recessive/dominant).",
            "Genetic blocks in lymphocyte development, phagocyte burst, or complement causing severe recurrent infections.",
            "Examples: SCID (IL2RG/ADA -> absent T/NK cells), XLA (BTK -> no mature B cells), CGD (CYBB -> defective NADPH oxidase).",
            "Individually rare (~1:1,200–1:50,000 combined); newborn TREC screening catches SCID early.",
            "Recurrent sinopulmonary bacterial infections (XLA), catalase+ abscesses (CGD), failure to thrive/opportunists (SCID).",
            "Specific defect predicts pathogen spectrum: B-cell -> encapsulated bacteria; T-cell -> fungi/viruses; terminal MAC -> Neisseria.",
            "TREC newborn screening, quantitative IgG/A/M, flow cytometry T/B/NK panels, DHR-123 oxidative burst, WES.",
            "Genetic counseling, live-vaccine avoidance in severe T-cell defects, newborn screening.",
            "IVIG/SCIG replacement (XLA), antimicrobial prophylaxis, HSCT, and lentiviral/CRISPR autologous gene therapy.",
            "Base/prime editing for SCID and CGD hematopoietic stem cells.",
            listOf("janeway", "abbas")
        ),
        DiseaseProfile(
            "allergy", "Allergic Conditions (Type I)", "Hypersensitivity", "IgE-Mediated Mast Cell & Basophil Activation",
            "Non-communicable; atopic genetic predisposition + environmental allergen exposure.",
            "Inappropriate Th2/IL-4/IL-13 class switching to allergen-specific IgE causing rapid mucosal and vascular responses.",
            "Sensitization loads FcεRI on mast cells; re-exposure crosslinks IgE triggering immediate histamine/leukotriene release and late eosinophil influx.",
            "Affects up to 20–30% globally (allergic rhinitis, asthma, food allergy, atopic dermatitis).",
            "Sneezing, wheezing/bronchospasm, urticaria, angioedema, or systemic anaphylactic shock.",
            "Epithelial alarmins (TSLP, IL-25, IL-33) prime ILC2 and Th2 responses upon barrier disruption.",
            "Skin prick testing, serum allergen-specific IgE (ImmunoCAP), oral food challenge.",
            "Allergen avoidance, early oral tolerance introduction (e.g., peanut), barrier emollients.",
            "Antihistamines, inhaled corticosteroids, epinephrine auto-injector (anaphylaxis), omalizumab, dupilumab (anti-IL-4Rα).",
            "Sublingual/epicutaneous immunotherapy and anti-TSLP biologics.",
            listOf("kuby", "abbas")
        ),
        DiseaseProfile(
            "cancer", "Cancer Immune Dysfunction", "Oncology / Immuno-Oncology", "Tumor Immune Evasion & Editing",
            "Non-communicable somatic malignancy.",
            "Malignant clones undergo elimination, equilibrium, and escape by co-opting immune checkpoints and suppressive stroma.",
            "Tumors downregulate MHC I/β2M, upregulate PD-L1/IDO, and recruit Tregs, MDSCs, and M2 TAMs.",
            "Universal hallmark across solid tumors and hematologic malignancies.",
            "Variable by primary organ; immunotherapy responses correlate with TIL density and neoantigen burden.",
            "CD8+ TIL exhaustion (PD-1, TIM-3, LAG-3) driven by chronic antigen stimulation and hostile hypoxic/lactate TME.",
            "Immunohistochemistry (PD-L1 CPS), TMB/MSI-high sequencing, flow cytometry, ctDNA.",
            "HPV & HBV vaccination prevent virally driven cervical/hepatic malignancies.",
            "Checkpoint inhibitors (anti-PD-1/PD-L1, anti-CTLA-4, anti-LAG-3), bispecific T-cell engagers, autologous CAR-T.",
            "Personalized mRNA neoantigen vaccines, TCR-engineered TILs, and armored CAR-NK/macrophage therapies.",
            listOf("abbas", "janeway")
        )
    )

    val HIV_STAGES = listOf(
        HivStage(1, "Attachment & Coreceptor Switch", "gp120 binds host CD4 (D1 domain), inducing conformational shift to engage CCR5 (R5-tropic) or CXCR4 (X4-tropic).", "Entry inhibitors (Maraviroc, Fostemsavir)", "Established"),
        HivStage(2, "gp41 Membrane Fusion", "gp41 HR1/HR2 six-helix bundle snaps viral and host membranes together, releasing the conical p24 capsid core.", "Fusion inhibitors (Enfuvirtide)", "Established"),
        HivStage(3, "Reverse Transcription & Nuclear Import", "Error-prone viral RT converts ssRNA into linear dsDNA inside the intact capsid entering nuclear pores.", "NRTIs (Tenofovir/FTC) & NNRTIs (Doravirine)", "Established"),
        HivStage(4, "Proviral Integration", "Integrase inserts HIV dsDNA into transcriptionally active host chromatin, establishing lifelong cellular lineage persistence.", "INSTIs (Dolutegravir, Bictegravir)", "Established"),
        HivStage(5, "Transcription, Translation & Assembly", "Host RNA Pol II + viral Tat/Rev express Gag/Gag-Pol/Env polyproteins that assemble at plasma membrane lipid rafts.", "Capsid inhibitor (Lenacapavir)", "Established"),
        HivStage(6, "Budding & Protease Maturation", "ESCRT machinery pinches immature virions; HIV Protease cleaves Gag-Pol into mature infectious capsid cones.", "Protease inhibitors (Darunavir)", "Established")
    )

    val HIV_TOPICS = listOf(
        HivTopic("hiv_struct", "Virion Architecture & Glycan Shield", "Established", "Enveloped diploid (+)ssRNA lentivirus (~120 nm) displaying trimeric gp120/gp41 spikes shielded by dense N-linked glycans over p17 matrix and p24 conical capsid.", "hiv"),
        HivTopic("hiv_tropism", "CD4, CCR5 & CXCR4 Coreceptors", "Established", "Transmitted/founder strains predominantly use CCR5; ~1% homozygous CCR5Δ32 Europeans resist R5 infection. X4 emergence correlates with faster CD4 decline.", "abbas"),
        HivTopic("hiv_latency", "Latent Proviral Reservoirs", "Established", "Resting memory CD4+ T cells harbor intact, transcriptionally silent proviruses (~1 per 10^6 cells) invisible to ART and CTLs, driving rebound if ART stops.", "hiv"),
        HivTopic("hiv_exhaustion", "Chronic Immune Activation & Exhaustion", "Established", "Early gut GALT depletion permits microbial translocation (LPS), driving persistent activation and PD-1/TIGIT CD8+ T-cell exhaustion even during ART.", "abbas"),
        HivTopic("hiv_reconst", "Immune Reconstitution & IRIS", "Established", "cART halts replication, allowing naive/memory CD4+ recovery; rapid restoration can rarely unmask latent TB or cryptococcus (IRIS).", "hiv"),
        HivTopic("hiv_bnabs", "Broadly Neutralizing Antibodies (bNAbs)", "Experimental", "Rare antibodies targeting conserved CD4bs, V3-glycan, or MPER epitopes suppress viremia and engage FcγR clearance in clinical trials.", "epmc"),
        HivTopic("hiv_shock", "Latency Reversal ('Shock and Kill')", "Experimental", "HDACi/PKC agonists reactivate silent proviruses under ART cover to expose infected cells to CTLs/bNAbs; clearing deep reservoirs remains challenging.", "hiv"),
        HivTopic("hiv_block", "'Block and Lock' / Proviral Excision", "Hypothesis", "Deep latency silencing (Tat inhibitors) or targeted CRISPR proviral excision explores permanent ART-free viral control without reactivation.", "epmc")
    )
}
