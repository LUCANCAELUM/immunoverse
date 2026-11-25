package com.example

import com.example.data.kb.AcademyKb
import com.example.data.kb.ImmunologyGlossaryKb
import com.example.data.kb.ImmunologyKb
import com.example.data.kb.PathogenKingdom
import com.example.data.kb.PathogenTaxonomyKb
import com.example.data.network.ScientificApiClient
import com.example.domain.AntagonistMechanism
import com.example.domain.BioinformaticsEngine
import com.example.domain.DosingRoute
import com.example.domain.ImmuneCellActivationProfile
import com.example.domain.ImmuneResponseSimulator
import com.example.domain.ImmuneSimulatorEngine
import com.example.domain.LibraryCatalogHelper
import com.example.domain.LibraryCategory
import com.example.domain.MolecularBio3DEngine
import com.example.domain.NodePerturbation
import com.example.domain.PathogenLoadParameters
import com.example.domain.PathogenSimulatorEngine
import com.example.domain.PharmacologyEngine
import com.example.domain.PeptideModType
import com.example.domain.ProteaseEnzyme
import com.example.domain.ProteomicsEngine

import com.example.data.model.BookmarkItem
import com.example.data.model.ResearchProject
import com.example.data.network.LiteraturePaper
import com.example.domain.QuizTimeLimitMode
import com.example.domain.QuizTimerHelper
import com.example.domain.ResearchAlertsEngine
import com.example.domain.SignalingEngine
import com.example.domain.SpikeReceptorComparator
import com.example.domain.TreatmentStrategy
import com.example.domain.VirtualLabEngine
import com.example.ui.components.AppRoute
import com.example.ui.components.GenomicBrowserHelper
import com.example.ui.components.GenomicStrainVizBuilder
import com.example.ui.components.buildCellularProcess3DScene
import com.example.ui.components.buildLabeledMolecule3DScene
import com.example.ui.components.buildPlatformSearchIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BioAndSimTests {

    @Test
    fun simulator_allPresets_produceBoundedPositiveDynamics() {
        assertEquals(9, ImmuneSimulatorEngine.PRESETS.size)
        for (preset in ImmuneSimulatorEngine.PRESETS) {
            val res = ImmuneSimulatorEngine.runSim(preset.params)
            assertTrue("Series empty for ${preset.key}", res.series.size > 50)
            assertTrue(res.peakAntigen >= 0.0)
            assertTrue(res.peakAntibody >= 0.0)
            assertTrue(res.events.isNotEmpty())
        }
    }

    @Test
    fun simulator_secondaryRecallExceedsPrimaryAntibody() {
        val primary = ImmuneSimulatorEngine.runSim(ImmuneSimulatorEngine.PRESETS.first { it.key == "primary" }.params)
        val secondary = ImmuneSimulatorEngine.runSim(ImmuneSimulatorEngine.PRESETS.first { it.key == "secondary" }.params)
        assertTrue(
            "Expected secondary recall Ab (${secondary.peakAntibody}) > primary (${primary.peakAntibody})",
            secondary.peakAntibody > primary.peakAntibody
        )
    }

    @Test
    fun bioinformatics_fastaOrfAlignmentUpgmaDigestAndSnv() {
        val recs = BioinformaticsEngine.parseFasta(BioinformaticsEngine.DEMO_FASTA)
        assertEquals(1, recs.size)
        val seq = recs.first().sequence
        assertEquals("DNA", BioinformaticsEngine.detectSeqType(seq))
        val orfs = BioinformaticsEngine.findOrfs(seq, minAa = 15)
        assertTrue("Expected at least 1 ORF", orfs.isNotEmpty())

        val aln = BioinformaticsEngine.alignSequences("ATGGTGCTG", "ATGGTTCTG", global = true)
        assertTrue(aln.identityPct > 80.0)

        val multi = BioinformaticsEngine.parseFasta(BioinformaticsEngine.DEMO_MULTI_FASTA)
        assertEquals(4, multi.size)
        val tree = BioinformaticsEngine.buildUpgmaTree(multi)
        assertNotNull(tree)
        val newick = BioinformaticsEngine.toNewick(tree)
        assertTrue(newick.contains("SeqA_Human"))

        val frags = BioinformaticsEngine.digestDna("AAAAGAATTCTTTTGAATTCAAAA", "EcoRI (GAATTC)")
        assertEquals(3, frags.size)

        val snv = BioinformaticsEngine.classifySnv("TAC", "TAA")
        assertNotNull(snv)
        assertTrue(snv!!.classification.contains("Nonsense"))
    }

    @Test
    fun bioinformatics_csvStatsAndOls() {
        val tbl = BioinformaticsEngine.parseCsv(BioinformaticsEngine.DEMO_CYTOKINE_CSV)
        assertEquals(5, tbl.headers.size)
        assertEquals(7, tbl.rows.size)
        val days = BioinformaticsEngine.extractNumericColumn(tbl, 0)
        val il6 = BioinformaticsEngine.extractNumericColumn(tbl, 1)
        val desc = BioinformaticsEngine.describeColumn("IL6", il6)
        assertNotNull(desc)
        assertEquals(7, desc!!.n)
        val fit = BioinformaticsEngine.olsFit(days, days.map { 2.0 * it + 3.0 })
        assertNotNull(fit)
        assertEquals(2.0, fit!!.slope, 1e-6)
        assertEquals(3.0, fit.intercept, 1e-6)
        assertEquals(1.0, fit.rSquared, 1e-6)
    }

    @Test
    fun virtualLab_all22ExperimentsExecuteCleanly() {
        assertEquals(22, VirtualLabEngine.EXPERIMENTS.size)
        for (exp in VirtualLabEngine.EXPERIMENTS) {
            val out = VirtualLabEngine.runExperiment(exp.id, emptyMap())
            assertTrue("Observations empty for ${exp.id}", out.observations.isNotEmpty())
            assertTrue("Metrics empty for ${exp.id}", out.metrics.isNotEmpty())
        }
    }

    @Test
    fun signaling_allPathwaysAndCellContextsProduceBoundedSignals() {
        assertEquals(11, ImmunologyKb.PATHWAYS.size)
        val cond = SignalingEngine.CONDITIONS[1]
        for (pathway in ImmunologyKb.PATHWAYS) {
            for (cell in SignalingEngine.CELL_CONTEXTS) {
                val res = SignalingEngine.simulate(
                    pathway = pathway,
                    cellContext = cell,
                    condition = cond,
                    customCsvMap = emptyMap(),
                    stimulusDose = 1.0,
                    feedbackStrength = 1.0,
                    perturbations = emptyMap()
                )
                assertEquals(pathway.nodes.size, res.nodeActivations.size)
                assertTrue(res.nodeActivations.values.all { it in 0.0..1.0 })
                assertEquals(13, res.timeSeries.size)
                assertTrue(res.downstreamOutputs.isNotEmpty())
            }
        }
    }

    @Test
    fun signaling_inhibitorBlockadeAndCsvParsingModulateDownstream() {
        val jakStat = ImmunologyKb.PATHWAYS.first { it.id == "jak_stat" }
        val th1 = SignalingEngine.CELL_CONTEXTS.first { it.id == "th1" }
        val viral = SignalingEngine.CONDITIONS.first { it.id == "viral_ifn" }
        val customCond = SignalingEngine.CONDITIONS.first { it.id == "custom_csv" }

        val baseline = SignalingEngine.simulate(jakStat, th1, viral, emptyMap(), 1.2, 0.8, emptyMap())
        val blocked = SignalingEngine.simulate(
            jakStat, th1, viral, emptyMap(), 1.2, 0.8,
            mapOf("jak" to NodePerturbation.INHIBITED)
        )
        assertTrue(
            "Expected JAK inhibition to reduce downstream gene output (${blocked.nodeActivations["gene"]} < ${baseline.nodeActivations["gene"]})",
            (blocked.nodeActivations["gene"] ?: 1.0) < (baseline.nodeActivations["gene"] ?: 0.0)
        )

        val csvMap = SignalingEngine.parseBiologicalCsv(SignalingEngine.DEMO_BIO_CSV)
        assertTrue(csvMap.containsKey("jak"))
        val csvSim = SignalingEngine.simulate(jakStat, th1, customCond, csvMap, 1.0, 1.0, emptyMap())
        assertTrue("Expected biological CSV features to match pathway nodes", csvSim.matchedCsvNodes >= 3)
    }

    @Test
    fun pathogenSimulator_allStrainsAndEarlyTreatmentReduction() {
        assertEquals(8, PathogenSimulatorEngine.REFERENCE_STRAINS.size)
        assertEquals(5, PathogenSimulatorEngine.HOST_PROFILES.size)
        val healthy = PathogenSimulatorEngine.HOST_PROFILES.first { it.key == "healthy_adult" }
        for (strain in PathogenSimulatorEngine.REFERENCE_STRAINS) {
            val res = PathogenSimulatorEngine.runHostInfectionSim(strain, healthy, TreatmentStrategy())
            assertTrue("Series empty for ${strain.id}", res.series.size > 40)
            assertTrue(res.effectiveR0 > 0.0)
            assertTrue(res.maxOrganStressPct in 0.0..100.0)
        }
        val delta = PathogenSimulatorEngine.REFERENCE_STRAINS.first { it.id == "scov2_delta" }
        val treated = PathogenSimulatorEngine.runHostInfectionSim(
            delta,
            healthy,
            TreatmentStrategy(
                useDirectAntiviralOrAntibiotic = true,
                useMonoclonalAntibody = true,
                interventionDay = 1.5
            )
        )
        assertTrue("Expected therapy to reduce pathogen burden", treated.burdenReductionPct > 10.0)
        assertTrue("Expected therapy to reduce max organ stress", treated.maxOrganStressPct <= treated.untreatedMaxStressPct)
    }

    @Test
    fun molecularBio3D_dnaRnaAminoAcidsAndProteinAnalysis() {
        assertEquals(20, MolecularBio3DEngine.AMINO_ACIDS.size)
        val lys = MolecularBio3DEngine.AMINO_ACIDS.first { it.code1 == 'K' }
        val glu = MolecularBio3DEngine.AMINO_ACIDS.first { it.code1 == 'E' }
        assertTrue("Lysine net charge at pH 7.4 should be positive", MolecularBio3DEngine.chargeAtPh(lys, 7.4) > 0.5)
        assertTrue("Glutamate net charge at pH 7.4 should be negative", MolecularBio3DEngine.chargeAtPh(glu, 7.4) < -0.5)

        val dna = MolecularBio3DEngine.analyzeDna("ATGGCCATTGTAATGGGCCGCTGAAAGGGTGCCCGATAG", "B-DNA")
        assertEquals("TACCGGTAACATTACCCGGCGACTTTCCCACGGGCTATC", dna.complementSeq)
        assertTrue(dna.gcPct in 40.0..70.0)
        assertTrue(dna.tmCelsius > 50.0)

        val rna = MolecularBio3DEngine.analyzeRna("GGUCUCUCUGGUUAGACCAGAUCUGAGCCUGGGAGCUCUCUGGCUAACUAGG")
        assertEquals(rna.lengthNt, rna.dotBracket.length)
        assertEquals(rna.dotBracket.count { it == '(' }, rna.dotBracket.count { it == ')' })
        assertTrue(rna.wcPairs + rna.wobbleGuPairs > 0)

        val prot = MolecularBio3DEngine.analyzeProteinFold("MKVLWAALLVTFLAGCQAKVEQAVETEPEPELRQQTEW")
        assertEquals(100.0, prot.helixPct + prot.sheetPct + prot.coilPct, 0.05)
    }

    @Test
    fun quizTimer_formattingFilteringAndScoring() {
        assertEquals("02:05", QuizTimerHelper.formatSeconds(125))
        assertEquals("00:00", QuizTimerHelper.formatSeconds(-3))
        assertTrue(QuizTimeLimitMode.PER_QUESTION_20S.isTimed)
        assertFalse(QuizTimeLimitMode.UNTIMED.isTimed)
        val qs = AcademyKb.QUIZ_BANK.take(4)
        val answers = mapOf(
            qs[0].id to qs[0].correctIndex,
            qs[1].id to ((qs[1].correctIndex + 1) % qs[1].options.size)
        )
        val summary = QuizTimerHelper.computeSummary(qs, answers, setOf(qs[2].id), 64)
        assertEquals(4, summary.totalQuestions)
        assertEquals(2, summary.answeredCount)
        assertEquals(1, summary.correctCount)
        assertEquals(1, summary.timedOutCount)
        assertEquals(25, summary.scorePercent)
    }

    @Test
    fun d3Genomics_presetsAndMultiFastaSerialization() {
        assertEquals(3, GenomicStrainVizBuilder.PRESET_PANELS.size)
        for (preset in GenomicStrainVizBuilder.PRESET_PANELS) {
            assertTrue(preset.strains.isNotEmpty())
            assertTrue(preset.domains.isNotEmpty())
            assertTrue(preset.mutations.isNotEmpty())
            assertTrue(preset.matrix.isNotEmpty())
        }
        val recs = BioinformaticsEngine.parseFasta(BioinformaticsEngine.DEMO_MULTI_FASTA)
        val panel = GenomicStrainVizBuilder.fromMultiFasta(recs)
        assertEquals(4, panel.strains.size)
        assertEquals(16, panel.matrix.size)
        assertTrue(panel.windows.isNotEmpty())
        assertTrue(panel.domains.isNotEmpty())
    }

    @Test
    fun pharmacology_structureElucidationLabeled3dAndPdPkAssays() {
        assertEquals(8, PharmacologyEngine.MOLECULES.size)
        for (mol in PharmacologyEngine.MOLECULES) {
            assertTrue("Atoms empty for ${mol.id}", mol.atoms.isNotEmpty())
            assertTrue("Bonds empty for ${mol.id}", mol.bonds.isNotEmpty())
            assertTrue("IR peaks empty for ${mol.id}", mol.irPeaks.isNotEmpty())
            assertTrue("1H NMR peaks empty for ${mol.id}", mol.hNmrPeaks.isNotEmpty())
            assertTrue("Wet bench tests empty for ${mol.id}", mol.wetBenchTests.isNotEmpty())
            val scene = buildLabeledMolecule3DScene(mol)
            assertEquals(mol.atoms.size, scene.spheres.size)
            assertTrue(scene.spheres.all { it.label.isNotBlank() })
            assertTrue(PharmacologyEngine.generateFtirCurve(mol).size > 20)
            assertTrue(PharmacologyEngine.generateHNmrCurve(mol).size > 20)
            assertTrue(PharmacologyEngine.generateTitrationCurve(mol).size > 10)
        }

        val acet = PharmacologyEngine.MOLECULES.first { it.id == "acetaminophen" }
        val exactEval = PharmacologyEngine.evaluateElucidationHypothesis(
            target = acet,
            guessedFormula = "c8h9no2",
            guessedIhd = 5,
            selectedGroups = acet.functionalGroupTags.toSet()
        )
        assertEquals(100, exactEval.scorePct)
        assertTrue(exactEval.formulaMatched)
        assertTrue(exactEval.ihdMatched)

        val comp = PharmacologyEngine.runPharmacodynamicsAndPk(
            mol = acet,
            doseMg = 500.0,
            antagonistRatio = 4.0,
            antagonistMechanism = AntagonistMechanism.COMPETITIVE,
            route = DosingRoute.ORAL
        )
        assertEquals(5.0, comp.schildRatio, 1e-6)
        assertTrue(comp.apparentEc50Nm > acet.ec50Nm)
        assertEquals(acet.emaxPct, comp.apparentEmaxPct, 1e-6)
        assertTrue(comp.cMaxMgL > 0.0)
        assertTrue(comp.aucMgHrL > 0.0)

        val nonComp = PharmacologyEngine.runPharmacodynamicsAndPk(
            mol = acet,
            doseMg = 500.0,
            antagonistRatio = 4.0,
            antagonistMechanism = AntagonistMechanism.NON_COMPETITIVE,
            route = DosingRoute.IV_BOLUS
        )
        assertTrue(nonComp.apparentEmaxPct < acet.emaxPct)
        assertEquals(0.0, nonComp.tMaxHr, 1e-6)
    }

    @Test
    fun genomicSequenceBrowser_normalizationMotifsAndCodonLookup() {
        val dna = GenomicBrowserHelper.normalizeDna("atg-gcu-taa")
        assertEquals("ATGGCTTAA", dna)
        assertEquals('T', GenomicBrowserHelper.complementBase('A'))
        assertEquals('C', GenomicBrowserHelper.complementBase('G'))
        assertEquals(3, GenomicBrowserHelper.hBondCount('G'))
        assertEquals(2, GenomicBrowserHelper.hBondCount('A'))

        val hits = GenomicBrowserHelper.findMotifPositions("CCCATGGGGATGTTT", "ATG")
        assertEquals(listOf(3, 9), hits)

        val (codon, aa) = GenomicBrowserHelper.codonAt("ATGGCTTAA", 1, frameOffset = 0)
        assertEquals("ATG", codon)
        assertEquals("M", aa)
        assertEquals(0.5, GenomicBrowserHelper.windowGcFraction("AATTGGCC", 3, halfWin = 4), 1e-6)
    }

    @Test
    fun immuneResponseSimulator_handlesPathogenLoadAndCellActivationLevels() {
        val competentSim = ImmuneResponseSimulator(
            initialPathogenParams = PathogenLoadParameters(initialPathogenLoad = 1.4, replicationRate = 0.52),
            initialCellActivation = ImmuneCellActivationProfile(
                macrophageNeutrophilLevel = 1.2,
                dendriticApcLevel = 1.15,
                cd4HelperActivation = 1.2,
                cd8CytotoxicActivation = 1.25,
                bCellPlasmaActivation = 1.2,
                tregSuppressionLevel = 1.0
            )
        )
        val competentReport = competentSim.runForDuration(24.0)
        assertTrue(competentSim.history.size > 40)
        assertNotNull(competentReport.timeToClearanceDays)
        assertTrue(competentReport.finalPathogenLoad < 0.05)

        val impairedSim = ImmuneResponseSimulator(
            initialPathogenParams = PathogenLoadParameters(initialPathogenLoad = 1.4, replicationRate = 0.52),
            initialCellActivation = ImmuneCellActivationProfile(
                macrophageNeutrophilLevel = 0.45,
                cd4HelperActivation = 0.25,
                cd8CytotoxicActivation = 0.2,
                bCellPlasmaActivation = 0.25
            )
        )
        val impairedReport = impairedSim.runForDuration(24.0)
        assertTrue(
            "Impaired activation (${impairedReport.finalPathogenLoad}) should leave higher pathogen burden than competent (${competentReport.finalPathogenLoad})",
            impairedReport.finalPathogenLoad > competentReport.finalPathogenLoad
        )

        competentSim.boostPathogenLoad(1.8)
        assertTrue(competentSim.currentState.pathogenLoad >= 1.8)
        competentSim.modulateImmuneCellActivation { it.copy(bCellPlasmaActivation = 1.8) }
        val postBoost = competentSim.runForDuration(30.0)
        assertTrue(postBoost.peakAntibodyTiter >= competentReport.peakAntibodyTiter)
        assertEquals(1.4, competentSim.toSimParams().antigenLoad, 1e-6)
    }

    @Test
    fun immunologyGlossary_termsDiagramsCrossLinksAndFiltering() {
        assertEquals(18, ImmunologyGlossaryKb.TERMS.size)
        for (term in ImmunologyGlossaryKb.TERMS) {
            assertTrue("Short definition blank for ${term.id}", term.shortDefinition.isNotBlank())
            assertTrue("Detailed mechanism blank for ${term.id}", term.detailedMechanism.isNotBlank())
            assertTrue("Clinical relevance blank for ${term.id}", term.clinicalRelevance.isNotBlank())
            assertTrue("Default focused part missing for ${term.id}", term.defaultFocusedPart in term.diagramType.parts)
            for (relId in term.relatedTermIds) {
                assertNotNull("Invalid related term $relId on ${term.id}", ImmunologyGlossaryKb.byId(relId))
            }
        }
        val complementHits = ImmunologyGlossaryKb.filterTerms("complement", "All")
        assertTrue(complementHits.any { it.id == "complement_mac" })
        val geneticsOnly = ImmunologyGlossaryKb.filterTerms("", "Genetics & Receptors")
        assertEquals(3, geneticsOnly.size)
    }

    @Test
    fun proteomicsEngine_protParamDigestMsMsPtmAndVolcano() {
        val ubq = ProteomicsEngine.BENCHMARK_PROTEINS.first { it.pdbId == "1UBQ" }
        val pp = ProteomicsEngine.computeProtParam(ubq.sequence)
        assertEquals(76, pp.length)
        assertTrue("Expected Ubiquitin MW near 8565 Da, got ${pp.avgMassDa}", pp.avgMassDa in 8400.0..8700.0)
        assertTrue("Expected pI between 6.0 and 7.3, got ${pp.theoreticalPi}", pp.theoreticalPi in 6.0..7.3)
        assertTrue(pp.titrationCurve.size > 10)
        assertEquals(20, pp.counts.size)

        val digest = ProteomicsEngine.digestProtein(
            rawSeq = "MKVLWAALLVTFLAGCQAKVEQAVETEPEPELRQQTEW",
            enzyme = ProteaseEnzyme.TRYPSIN,
            maxMissedCleavages = 1,
            fixedCarbamidomethylC = true
        )
        assertTrue(digest.isNotEmpty())

        val frag = ProteomicsEngine.fragmentPeptideMsMs("LHLVLRLR", PeptideModType.CARBAMIDOMETHYL_C)
        val bIons = frag.ions.filter { it.series == "b" }
        val yIons = frag.ions.filter { it.series == "y" }
        assertTrue(bIons.isNotEmpty())
        assertTrue(yIons.isNotEmpty())
        val obsText = (bIons.take(3) + yIons.take(3)).joinToString(" ") { "%.4f".format(it.mz) }
        val psm = ProteomicsEngine.matchObservedMsMsPeaks(frag, obsText, tolerancePpm = 20.0)
        assertEquals(6, psm.matchedCount)
        assertTrue(psm.hyperscore > 1.0)

        val ptms = ProteomicsEngine.scanPtms("MNKTSRGSPKTKKCE")
        assertTrue(ptms.any { it.category.contains("Glycosylation") })

        val volcano = ProteomicsEngine.parseDifferentialProteomicsCsv(ProteomicsEngine.DEMO_VOLCANO_CSV)
        assertTrue(volcano.isNotEmpty())
        assertTrue(volcano.all { it.fdrBh >= it.pValue - 1e-9 })
        assertTrue(volcano.any { it.regulation == "Up" })
    }

    @Test
    fun scientificApiClient_parsePdbText_extractsSecStructSsbondAndLigands() {
        val miniPdb = """
            EXPDTA    X-RAY DIFFRACTION
            TITLE     SYNTHETIC TEST DOMAIN
            HELIX    1   1 ALA A    1  GLY A    2  1                                   2
            SHEET    1   A 1 VAL A   3  CYS A   4  0
            SSBOND   1 CYS A    4    CYS A    4     1555   1555  2.05
            ATOM      1  CA  ALA A   1      10.000  10.000  10.000  1.00 18.50           C
            ATOM      2  CA  GLY A   2      13.800  10.000  10.000  1.00 22.00           C
            ATOM      3  CA  VAL A   3      15.200  13.200  10.000  1.00 12.40           C
            ATOM      4  CA  CYS A   4      18.800  13.200  10.000  1.00 14.10           C
            HETATM    5  C1  NAG L   1      20.000  11.000  11.000  1.00 30.00           C
            END
        """.trimIndent()
        val parsed = ScientificApiClient.parsePdbText(
            id = "9TST",
            pdbText = miniPdb,
            title = "RCSB PDB 9TST",
            method = "Experimental",
            resolution = "1.50 Å"
        )
        assertEquals(4, parsed.caAtoms.size)
        assertEquals('H', parsed.caAtoms[0].secStruct)
        assertEquals('E', parsed.caAtoms[2].secStruct)
        assertEquals(18.5f, parsed.caAtoms[0].bFactor, 0.01f)
        assertEquals(1, parsed.disulfides.size)
        assertEquals(1, parsed.heteroAtoms.size)
        assertEquals("X-RAY DIFFRACTION", parsed.method)
    }

    @Test
    fun pathogenTaxonomy_allKingdomsPartsAndEvolutionaryVariants() {
        val kingdomsPresent = PathogenTaxonomyKb.SPECIMENS.map { it.kingdom }.toSet()
        assertEquals(PathogenKingdom.entries.toSet(), kingdomsPresent)
        for (spec in PathogenTaxonomyKb.SPECIMENS) {
            assertTrue("Expected >= 4 parts for ${spec.id}", spec.parts.size >= 4)
            assertTrue("Expected >= 4 variants for ${spec.id}", spec.variants.size >= 4)
            val partIds = spec.parts.map { it.id }.toSet()
            for (v in spec.variants) {
                assertTrue("Variant ${v.id} references invalid part ${v.mutatedPartId}", v.mutatedPartId in partIds)
            }
        }
    }

    @Test
    fun spikeReceptorComparator_andCellularProcesses3dArena() {
        assertEquals(7, SpikeReceptorComparator.VIRAL_SPIKES.size)
        assertEquals(6, SpikeReceptorComparator.HOST_RECEPTORS.size)

        val scov = SpikeReceptorComparator.VIRAL_SPIKES.first { it.id == "sarscov2_jn1" }
        val ace2 = SpikeReceptorComparator.HOST_RECEPTORS.first { it.id == "ace2" }
        val cd4 = SpikeReceptorComparator.HOST_RECEPTORS.first { it.id == "cd4_ccr5" }

        val matchRes = SpikeReceptorComparator.comparePair(scov, ace2)
        val mismatchRes = SpikeReceptorComparator.comparePair(scov, cd4)
        assertTrue(matchRes.isCognateMatch)
        assertFalse(mismatchRes.isCognateMatch)
        assertTrue(matchRes.entryEfficiencyPct > mismatchRes.entryEfficiencyPct)
        assertTrue(matchRes.bindingAffinityScore > mismatchRes.bindingAffinityScore)
        val dockScene = SpikeReceptorComparator.buildSpikeHostDocking3DScene(scov, ace2, 0.5f)
        assertTrue(dockScene.spheres.isNotEmpty())

        for (proc in ImmunologyKb.PROCESSES) {
            val sceneT0 = buildCellularProcess3DScene(proc, 0.2f, null)
            val sceneT1 = buildCellularProcess3DScene(proc, 1.35f, null)
            assertTrue("Empty 3D spheres for cellular process ${proc.id}", sceneT1.spheres.isNotEmpty())
            assertTrue("Expected rich molecule/organelle legend for ${proc.id}", sceneT1.legendItems.size >= 4)
            // Confirm active molecule/organelle animation changes sphere coordinates between frames
            val posChanged = sceneT0.spheres.zip(sceneT1.spheres).any { (a, b) ->
                kotlin.math.abs(a.x - b.x) > 1e-3f || kotlin.math.abs(a.y - b.y) > 1e-3f || kotlin.math.abs(a.z - b.z) > 1e-3f
            }
            assertTrue("Expected dynamic animation for cellular process ${proc.id}", posChanged)
            val focusedId = sceneT1.legendItems.first().targetId
            assertNotNull(focusedId)
            val focused = buildCellularProcess3DScene(proc, 1.35f, focusedId)
            assertTrue(focused.spheres.any { it.id == focusedId && it.alpha == 1.0f })
        }
    }

    @Test
    fun platformSearchIndex_coversPathogensSpikesReceptorsProcessesAndRoutes() {
        val index = buildPlatformSearchIndex()
        assertTrue("Expected populated platform search index", index.size >= 50)
        assertTrue(index.any { it.route == AppRoute.PATHOGEN_LAB && it.title.contains("SARS-CoV-2") })
        assertTrue(index.any { it.route == AppRoute.PATHOGEN_LAB && it.category.contains("Host Surface Receptor") })
        assertTrue(index.any { it.route == AppRoute.CELLS && it.category.contains("3D Cellular Process") })
        assertTrue(index.any { it.route == AppRoute.PROTEOMICS })
        assertTrue(index.any { it.route == AppRoute.GLOSSARY })
    }

    @Test
    fun researchAlertsEngine_matchesInterestsProjectsBookmarksAndLivePapers() {
        val interests = ResearchAlertsEngine.extractEffectiveInterests(
            explicitInterests = setOf("CAR-T & Checkpoint Blockade"),
            bookmarks = listOf(BookmarkItem(id = "b1", title = "NLRP3 Inflammasome")),
            projects = listOf(ResearchProject(id = "p1", name = "SARS-CoV-2 Spike Mutational Escape"))
        )
        assertTrue(interests.contains("CAR-T & Checkpoint Blockade"))
        assertTrue(interests.contains("NLRP3 Inflammasome"))
        assertTrue(interests.contains("SARS-CoV-2 Spike Mutational Escape"))

        val query = ResearchAlertsEngine.buildEuropePmcInterestQuery(interests)
        assertTrue(query.contains("CAR-T") && query.contains("NLRP3"))

        val livePaper = LiteraturePaper(
            id = "epmc1",
            source = "Europe PMC",
            title = "Next-Generation Armored CAR-T Checkpoint Blockade in Solid Tumors",
            authors = "Doe J et al.",
            journal = "Nature Immunology",
            year = "2025",
            doi = "10.1038/test",
            url = "https://europepmc.org",
            abstractText = "Sustained checkpoint blockade and cytokine armoring prevent T-cell exhaustion.",
            pubType = "Journal Article"
        )
        val alerts = ResearchAlertsEngine.buildAlerts(interests, listOf(livePaper))
        assertTrue(alerts.isNotEmpty())
        val first = alerts.first()
        assertTrue(first.isLivePaper)
        assertEquals("CAR-T & Checkpoint Blockade", first.matchedInterest)
    }

    @Test
    fun libraryCatalogHelper_classifiesFiltersAndRoutesSavedItems() {
        val article = BookmarkItem(
            id = "b_art",
            itemType = "paper",
            itemId = "pmid_1",
            title = "Neutralizing Spike Antibodies",
            subtitle = "Nature (2025)",
            meta = "DOI: 10.1038/x • https://europepmc.org/article/MED/1"
        )
        val pathogen = BookmarkItem(
            id = "b_path",
            itemType = "pathogen",
            itemId = "sars_cov_2",
            title = "SARS-CoV-2",
            subtitle = "Viruses • Spike Trimer"
        )
        val spike = BookmarkItem(
            id = "b_spk",
            itemType = "spike",
            itemId = "jn1_ace2",
            title = "JN.1 Spike vs ACE2",
            subtitle = "Kd 8.2 nM"
        )
        val topic = BookmarkItem(
            id = "b_top",
            itemType = "glossary",
            itemId = "cross_pres",
            title = "Cross-Presentation (cDC1)",
            subtitle = "Adaptive Priming"
        )
        val all = listOf(article, pathogen, spike, topic)

        assertEquals(LibraryCategory.ARTICLES, LibraryCatalogHelper.classify(article))
        assertEquals(LibraryCategory.PATHOGENS, LibraryCatalogHelper.classify(pathogen))
        assertEquals(LibraryCategory.PATHOGENS, LibraryCatalogHelper.classify(spike))
        assertEquals(LibraryCategory.TOPICS, LibraryCatalogHelper.classify(topic))

        val counts = LibraryCatalogHelper.countByCategory(all)
        assertEquals(4, counts[LibraryCategory.ALL])
        assertEquals(1, counts[LibraryCategory.ARTICLES])
        assertEquals(2, counts[LibraryCategory.PATHOGENS])
        assertEquals(1, counts[LibraryCategory.TOPICS])

        assertEquals(listOf(spike), LibraryCatalogHelper.filter(all, LibraryCategory.PATHOGENS, "ACE2"))
        assertEquals(AppRoute.LITERATURE, LibraryCatalogHelper.resolveRoute(article))
        assertEquals(AppRoute.PATHOGEN_LAB, LibraryCatalogHelper.resolveRoute(pathogen))
        assertEquals(AppRoute.GLOSSARY, LibraryCatalogHelper.resolveRoute(topic))
        assertEquals("https://europepmc.org/article/MED/1", LibraryCatalogHelper.extractHttpUrl(article.meta))
    }
}
