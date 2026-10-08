# Maintenance audit

This fork updates the overlay for the upstream Now Playing 1.3.5 recognition engine. It does not update the proprietary ASI core to a newer branch.

## Repairs

- Database counting streams each SST entry instead of retaining all song protobufs and flattening multiple full copies. Only title/artist identities are retained for deduplication.
- Cache fingerprints include file size and modification time, detecting same-name shard replacements.
- Completed download rows are excluded from pending music-download counts.
- Caller discovery supports LSPlant and Pine trampoline stacks, including reflection frames and missing callers.
- First-run missing country settings do not crash queries or select every country. Japan-only selection is covered by a regression test.
- Bitmap cropping uses square bounds, handles absent pixel configuration, and includes radius in its cache key.
- Compile-only text-classifier stubs stay out of the complete APK.
- Complete APK assembly uses the current AGP output, replaces previous overlay classes, preserves dex precedence when removing inherited duplicates, handles Apktool 3 metadata and private framework resource references, and signs only when its signing task executes.

## Dependencies and binary inputs

Gradle 9.8.1, AGP 9.4.1, Kotlin 2.4.20, JDK 17, compile SDK 37; target SDK remains 33 for the retained ASI engine. Protobuf generator/runtime, Guava, Bouncy Castle, compression, Pine, and available AndroidX dependencies were updated. Aliuhook uses stable 1.1.4 with its matching LSPlant and Dobby dependencies rather than a floating snapshot. Dead Bintray/local Maven repositories were removed. CI and weekly Dependabot checks are included.

Full APK validation used the public upstream [Now Playing 1.3.5 release](https://github.com/KieronQuinn/NowPlaying/releases/tag/1.3.5) as the already-patched base, with `-PbaseAlreadyPatched=true`. This is public release input, not a backup extracted from a phone. Build inputs are not committed. The complete APK and Ambient Music Mod must use the same signing certificate. The standalone overlay APK is not an installable recognition replacement.

## Validation

`bash gradlew buildApkDebug :overlay:testDebugUnitTest :overlay:lintDebug -PbaseAlreadyPatched=true --max-workers=2` passed locally. The overlay has 12 behavior tests covering country selection, first run, empty/corrupt shards, same-name replacement, country deduplication, caller discovery, and download state.

The stress fixture contains 20,000 songs with deliberately large synthetic player metadata, under a 256 MB test heap. The old materializing count implementation failed with `OutOfMemoryError`; the streaming implementation passed the same fixture. The merged base contained zero duplicate class paths after cleanup. APK signatures were verified against the paired app. The full companion started and read its bundled database on a disposable Android 16 emulator.

The CI job validates overlay assembly, tests, and lint. Proprietary full-APK inputs are not downloaded by CI. Recognition accuracy, background microphone behavior, 16 KB native-library compatibility, old ARMv7 behavior, and full country-download completion remain unverified. Native support on every OEM Android build cannot be inferred from emulator startup.
