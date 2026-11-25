# IMMUNOVERSE 2.0 — Android app (Capacitor)

Offline-first immunology & biomedical research command centre. The web app lives in `www/` (no build step, no runtime dependencies) and is wrapped as an Android APK with Capacitor.

## Build the APK

**Option A – GitHub Actions (no local setup):** push this folder to a GitHub repo → *Actions → Build IMMUNOVERSE APK → Run*. Download `immunoverse-debug-apk` and install `app-debug.apk` (enable "install unknown apps").

**Option B – locally** (Node 20+, JDK 17, Android SDK / Android Studio):
```
npm install
node tests/run.js
npx cap add android
npx @capacitor/assets generate --android --iconBackgroundColor '#050914' --splashBackgroundColor '#050914'
npx cap sync android
cd android && ./gradlew assembleDebug     # → android/app/build/outputs/apk/debug/app-debug.apk
```
For Play Store / release: create a keystore, add a `signingConfig` in `android/app/build.gradle`, run `./gradlew bundleRelease`.

**Try it in a browser first:** `npm run serve` → http://localhost:8080.

## What is implemented (all modules work offline unless noted)
| Module | Status |
|---|---|
| Dashboard | Animated 3D cell, daily discovery, quick launch, activity widgets, live Europe PMC feed (online) |
| Immune Atlas | 3D organ map (rotate/zoom/pan, cross-section, isolate, animated response), 3D cell viewer (receptors, section), interactions, compare; 15 cells × 4 explanation levels |
| Cellular Intelligence | Clickable organelle map, 10 stepwise animated processes, microscope-style stain simulator |
| Molecules | 15 entries; **real structures fetched live from RCSB PDB** (C-α trace, residue inspection, ligands) |
| Pathways | 11 pathways as interactive networks, signal propagation, node-inhibition what-if |
| Simulator | Documented ODE model, 7 scenarios, save/replay/compare, CSV/PNG export |
| Diseases | 13 profiles, comparison, HIV research section (virion, life cycle, evidence-labelled topics, latency-reversal model) |
| Virtual Lab | 22 simulated experiments with controls, observations, interpretation, limitations, questions, **DOCX/PDF/MD reports** |
| Immuno AI | Retrieval over the curated knowledge base + your uploads (txt/md/csv/fasta; PDF via pdf.js when online), evidence labels, quizzes/notes/compare, live Europe PMC citations; optional model proxy with Crossref DOI verification |
| Workspace | Projects, notes (Markdown + version history), hypotheses, experiment logs, timeline, references/bibliography, CSV datasets, role-based editing, DOCX/PDF/MD export |
| Genomics | FASTA viewer, translation/ORFs, global/local alignment, UPGMA tree, protein analysis, codon-level variant demo, expression heatmap, feature tracks, user-confirmed BLAST hand-off |
| Literature | Europe PMC, PubMed, Crossref, OpenAlex (+ preprints via Europe PMC); filters, save, compare, extractive summary, BibTeX, tracked topics |
| Academy | 10 tracks / 20 lessons, ~60 quiz questions, flashcards, exams, 3 cases, progress, study-notes export |
| Analytics | CSV → line / scatter+fit / bar / heatmap / correlation; summary stats; PNG & .md downloads |
| Profile / Admin | Local profile & roles, optional device passcode, backup/restore, feature flags, API health checks, storage & usage, logs, content management |

## Honest limitations
- **Single-device, local-first.** There is no server in the APK, so multi-user accounts, email verification, password recovery, PostgreSQL/Prisma, Redis, object storage and real-time collaboration are **not** implemented. Roles and the passcode are local only (browser storage is not encrypted). `server/ai-proxy.js` shows the pattern for a server-side model key; a full backend is a separate phase.
- **No React/Three.js/Tailwind.** The 3D engine is a small dependency-free software renderer; this keeps the APK offline-capable and small, but it is not photorealistic or GPU-volumetric.
- **Structures:** 3D molecules are real PDB data only for entries with a PDB ID, drawn as C-α traces (no surfaces/domains). AlphaFold is linked, not embedded.
- **Models & labs are educational.** Simulator parameters are illustrative, unfitted; lab results are simulated. Antibiotic breakpoints are illustrative — never use for clinical decisions.
- Textbook references name sources without page numbers; no paper DOIs are hard-coded. Live results come from the public APIs and require internet (Europe PMC, NCBI, Crossref, OpenAlex, RCSB all send CORS headers; check Admin → API status).
- Not load-tested; no scalability claims are made.
- Sample data (expression table, cytokine CSV, demo FASTA) is invented and labelled "demo".
