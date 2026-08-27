import com.android.build.api.dsl.CommonExtension

plugins {
    alias(deps.plugins.kotlin.android) apply false
    alias(deps.plugins.kotlin.jvm) apply false
    alias(deps.plugins.kotlin.serialization) apply false
    alias(deps.plugins.kotlin.kapt) apply false
    alias(deps.plugins.android.application) apply false
    alias(deps.plugins.android.library) apply false
    alias(deps.plugins.hilt.android) apply false
    alias(deps.plugins.google.services) apply false
    alias(deps.plugins.firebase.crashlytics) apply false
    alias(deps.plugins.firebase.perf) apply false
    alias(deps.plugins.room) apply false
    alias(deps.plugins.kotlin.compose.compiler) apply false
    alias(deps.plugins.ksp) apply false
    // Applied (not `apply false`) so the root project gets the aggregate `buildHealth` task.
    alias(deps.plugins.dependency.analysis)
}

buildscript {
    configurations.classpath {
        resolutionStrategy {
            // DAGP -> kotlin-metadata-jvm:2.2.21 -> kotlin-bom:2.2.21, whose platform
            // constraints upgrade kotlin-daemon-client to 2.2.21 while kotlin-compiler-runner stays
            // at the project Kotlin version. The version skew breaks incremental compilation via the
            // daemon (NoSuchMethodError on IncrementalCompilationOptions.<init>). Pin daemon-client
            // back to the project Kotlin version from the catalog.
            force("org.jetbrains.kotlin:kotlin-daemon-client:${deps.gradle.kotlin.get().version}")
        }
    }
    dependencies {
        classpath(deps.gradle.android)
        classpath(deps.agconnect.agcp)
    }
}

// `-Pdependency.analysis.android.ignored.variants` is passed only by the PR check, which trims the build
// to two build types (see run_code_quality.yml); the nightly run and a local `buildHealth`
// analyse every variant and pass nothing. Reusing that flag as the marker for "this is the trimmed run"
// beats inventing a private switch: the compensation it gates cannot outlive the variant trimming that
// made it necessary, because there is only one thing to forget instead of two.
// Presence is what counts, not content. A present-but-empty value means the workflow tried to trim and
// the list did not survive the trip — DAGP would then quietly analyse everything — so it is left to fail
// in `verifyDagpIgnoredVariantsProperty` rather than papered over here. Both readers use this one value,
// so they cannot disagree about what "passed" means.
val dagpIgnoredVariants: String? = providers
    .gradleProperty("dependency.analysis.android.ignored.variants")
    .orNull

val isTrimmedVariantRun = dagpIgnoredVariants != null

// Declaration buckets dropped from the analysis in the trimmed run. Single source of truth: consumed
// by `ignoreSourceSet` below and checked against the module's real source sets by
// `verifyDagpIgnoredSourceSets`, because DAGP matches these names literally and a stale one is a
// silent no-op rather than an error.
val dagpIgnoredSourceSets = listOf("internal", "external", "release", "mocked")

// Dependency Analysis (DAGP) global configuration.
dependencyAnalysis {
    // Every module declares project dependencies through the type-safe accessors enabled by
    // TYPESAFE_PROJECT_ACCESSORS (`projects.core.utils`). Without this, advice and `fixDependencies`
    // emit the `project(":core:utils")` string form, which doesn't match anything in the build files.
    useTypesafeProjectAccessors(true)

    structure {
        // Treat co-versioned artifact splits as a single logical dependency, so the plugin doesn't
        // advise "declare the transitive directly" when you depend on one part and use a sibling.
        // e.g. `serialization-json` always brings `serialization-core` (@Serializable lives there).
        bundle("kotlinx-serialization") {
            includeDependency("org.jetbrains.kotlinx:kotlinx-serialization-core")
            includeDependency("org.jetbrains.kotlinx:kotlinx-serialization-json")
        }
        // Hilt/Dagger is split across many artifacts (hilt-android pulls dagger, hilt-core,
        // javax.inject; hilt-compiler pulls dagger-compiler). Declaring hilt-android + hilt-compiler
        // is the single entry point — don't advise expanding each transitive separately.
        bundle("hilt") {
            primary("com.google.dagger:hilt-android")
            includeGroup("com.google.dagger")
            includeDependency("javax.inject:javax.inject")
        }
        // `junit-jupiter` is an aggregator over `-api`/`-params`/`-engine`. Tests import the `-api`
        // package; declaring the `junit-jupiter` aggregator (deps.test.junit5) is the project standard.
        bundle("junit5") {
            primary("org.junit.jupiter:junit-jupiter")
            includeGroup("org.junit.jupiter")
        }
        // MockK's DSL functions (every/coEvery/verify) live in `mockk-dsl`, pulled by `mockk`.
        // Declaring `mockk` (deps.test.mockk) is enough.
        bundle("mockk") {
            primary("io.mockk:mockk")
            includeGroup("io.mockk")
        }
        // DataStore is split into datastore-core / datastore-preferences-core / datastore-preferences,
        // all co-versioned. Declaring the one you use covers the sibling packages.
        bundle("androidx-datastore") {
            includeGroup("androidx.datastore")
        }
        // web3j is split across core/abi/crypto/tuples (+ its rxjava reactive layer), all co-versioned.
        // The generated Solidity contract wrappers expose these in their public API; declaring
        // web3j-core (the entry point) covers the family. rxjava is web3j-only in this repo.
        bundle("web3j") {
            primary("org.web3j:core")
            includeGroup("org.web3j")
            includeDependency("io.reactivex.rxjava2:rxjava")
        }
        // Decompose's public API (ComponentContext etc.) extends Essenty owner interfaces, so the
        // essenty artifacts are part of decompose's surface. Declaring `decompose` covers them.
        bundle("decompose") {
            primary("com.arkivanov.decompose:decompose")
            includeGroup("com.arkivanov.decompose")
            includeGroup("com.arkivanov.essenty")
        }
        // Jetpack Compose is split across ~15 co-versioned artifacts (ui, ui-graphics, ui-text,
        // ui-unit, foundation, foundation-layout, animation, animation-core, runtime, runtime-saveable,
        // material-ripple, …). Treat the whole `androidx.compose.*` family as one logical dependency.
        bundle("compose") {
            include("^androidx\\.compose\\..*")
        }
        // Co-versioned families split across many artifacts — declaring one covers the siblings.
        bundle("androidx-lifecycle") {
            primary("androidx.lifecycle:lifecycle-runtime-ktx")
            includeGroup("androidx.lifecycle")
        }
        bundle("androidx-appcompat") {
            primary("androidx.appcompat:appcompat")
            includeGroup("androidx.appcompat")
        }
        bundle("androidx-paging") {
            includeGroup("androidx.paging")
        }
        bundle("coil") {
            includeGroup("io.coil-kt")
        }
        // Families where the split artifact arrives from a sibling this repo already declares:
        // huawei-push brings opendevice, the customer.io SDKs bring their shared core, navigation-compose
        // brings navigation-common/runtime, and reorderable's debug variant is the same module.
        bundle("huawei") {
            primary("com.huawei.hms:push")
            includeGroup("com.huawei.hms")
        }
        bundle("customerio") {
            includeGroup("io.customer.android")
        }
        bundle("androidx-navigation") {
            primary("androidx.navigation:navigation-compose")
            includeGroup("androidx.navigation")
        }
        bundle("reorderable") {
            primary("sh.calvin.reorderable:reorderable")
            includeGroup("sh.calvin.reorderable")
        }
        // The instrumentation stack is a single co-versioned family: kaspresso pulls espresso, the
        // androidx.test runner/rules/monitor/core, uiautomator, kakaocup and the allure adapters.
        // Declaring kaspresso is the entry point — pinning each transitive separately would mean
        // hand-syncing eleven versions on every kaspresso bump.
        bundle("kaspresso") {
            primary("com.kaspersky.android-components:kaspresso")
            includeGroup("com.kaspersky.android-components")
            includeGroup("androidx.test")
            includeGroup("androidx.test.uiautomator")
            includeGroup("androidx.test.espresso")
            includeGroup("io.github.kakaocup")
            includeGroup("io.qameta.allure")
        }
        // Room is split into runtime/ktx/common/compiler/paging (+ its androidx.sqlite runtime).
        // Declaring room-runtime covers the siblings.
        bundle("room") {
            primary("androidx.room:room-runtime")
            includeGroup("androidx.room")
            includeGroup("androidx.sqlite")
        }
    }
    issues {
        all {
            // Only meaningful in the trimmed run. A dependency declared on `<buildType>Implementation`
            // for a build type with no analysed variant reads as unused, so those source sets are
            // dropped from the analysis rather than silenced by coordinate — the artifact stays checked
            // wherever it is still declared. `mocked` is in the list even though its variant IS analysed
            // (androidTest and src/mocked hang off it): chucker is the real library in debug and mocked
            // and a no-op stub elsewhere, so once the stub build types are gone it looks used in every
            // analysed variant and the plugin advises collapsing the pair into one `implementation`.
            // Dropping the mocked bucket leaves `debugImplementation(chucker)` as the declaration under
            // watch. The guard matters because this is a global setting: applied unconditionally it also
            // blinded the full run, where all five build types are analysed and the twelve declarations
            // it hides (chucker/chuckerStub in :app and :core:datasource, :features:kyc:impl in :app)
            // are exactly what the nightly report exists to watch.
            if (isTrimmedVariantRun) {
                ignoreSourceSet(*dagpIgnoredSourceSets.toTypedArray())
            }
            // Default severity is "warn", which is what local runs get: buildHealth/projectHealth only
            // report. Both CI workflows pass -Pdagp.severity=fail, so advice fails the build there — the
            // PR check in the modules it analyses, the nightly across the whole graph.
            onAny { severity(providers.gradleProperty("dagp.severity").getOrElse("warn")) }
            // Ignores the whole "wrong configuration" category, not only `implementation` -> `api`. The
            // plugin's filter is `Advice.isChange()` — declared and used, but on a different configuration
            // than the usage implies, minus the compileOnly and runtimeOnly targets, which route to
            // `onCompileOnly` / `onRuntimeOnly` (that is why the material and mlkit exclusions below still
            // work). Four kinds of advice ride on this one line:
            //  1. `implementation` -> `api`: raised across the whole feature graph, so acting on it would
            //     promote nearly every `implementation` there to `api` — the opposite of the api/impl
            //     split modularization exists for, and every ABI change would cascade to all consumers.
            //  2. `api` -> `implementation`: raised for domain modules whose public API is generic
            //     (`Flow<Lce<…>>`, `EitherFlow<…>`); demoting the `api` breaks the consumers compiling
            //     against those types.
            //     The exact mechanism behind 1 and 2 was never pinned down — the reason to silence them is
            //     that both were systematic and unusable, not a theory about how the plugin reads ABIs.
            //  3. `<buildType>Implementation` -> `implementation`: the variant-collapse advice. Once a
            //     dependency is used in every analysed variant, DAGP merges the declaration onto the base
            //     configuration — that is the chucker pair once the stub build types leave the analysis.
            //  4. `implementation` -> a variant-scoped configuration: the mirror image, e.g. googlePlay
            //     review, declared plainly in :app but referenced only from app/src/google.
            // Cases 1 and 2 are what the 17 per-module exclusions this rule replaced were about; 3 and 4
            // come with the category. Narrowing it back to "only -> api" resurrects all four at once and
            // fails the PR check. To re-enable one case, exclude the coordinate under this handler rather
            // than re-scoping the severity.
            onIncorrectConfiguration { severity("ignore") }
            // dagger-compiler is always pulled by hilt-compiler (declared via kapt(deps.hilt.kapt));
            // no need to declare the annotation processor separately.
            // :test:core is the documented single entry point for the unit-test stack — it re-exports
            // junit5/mockk/truth/turbine/coroutines-test as `api`. Because those re-exports are excluded
            // from the "declare directly" advice above, DAGP sees :test:core itself as unused. It isn't —
            // keep declaring testImplementation(projects.test.core) and silence the false positive.
            onUnusedDependencies {
                exclude(":test:core")
            }
            onUsedTransitiveDependencies {
                exclude("com.google.dagger:dagger-compiler")
                // jsr305 is a ubiquitous CLASS-retention nullability-annotation transitive (pulled in
                // by many libs); consumers tolerate its absence, it's never declared directly.
                exclude("com.google.code.findbugs:jsr305")
                // :test:core re-exports the unit-test stack as `api` and is the documented single
                // entry point (testImplementation(projects.test.core)). Don't advise declaring its
                // re-exports directly in every test module.
                exclude(
                    "app.cash.turbine:turbine",
                    "com.google.truth:truth",
                    "io.mockk:mockk",
                    "org.jetbrains.kotlinx:kotlinx-coroutines-test",
                    "org.junit.jupiter:junit-jupiter",
                )
                // wire-runtime (protobuf) and jakarta.inject-api are deep transitives of the Visa /
                // WalletConnect (reown) SDKs. A few generated/SDK-facing types reference them, but they're
                // never declared directly — they always arrive with their owning SDK. Don't advise
                // declaring them per-module.
                exclude(
                    "com.squareup.wire:wire-runtime",
                    "jakarta.inject:jakarta.inject-api",
                )
                // The lifecycle artifacts reach these modules from androidx.activity/fragment rather
                // than from a declared androidx.lifecycle member, so the bundle cannot absorb them.
                // Declaring them is worse than silencing: they resolve to 2.9.4 while the catalog's
                // androidxLifecycle is 2.5.1, so a direct declaration would quietly upgrade the module.
                exclude(
                    "androidx.lifecycle:lifecycle-viewmodel",
                    "androidx.lifecycle:lifecycle-viewmodel-compose",
                    "androidx.lifecycle:lifecycle-viewmodel-savedstate",
                )
                // datastore-preferences-core arrives through core:datasource rather than through the
                // datastore-core these modules declare, so the androidx-datastore bundle misses it.
                exclude("androidx.datastore:datastore-preferences-core")
                // core:ui exports haze as `api` and puts HazeState in the signatures of public DS
                // components (TangemPagerIndicator, Fade, TangemModalHost). Every module that renders
                // one of those ends up referencing haze in bytecode without importing it, so the advice
                // would reappear for each new screen that uses a bottom sheet.
                exclude("dev.chrisbanes.haze:haze")
                // :core:datasource is the data layer's single entry point and deliberately re-exports
                // these two as `api` (see its build file). Every data module reaches RetrofitFactory and
                // the local stores through it; declaring them in ~35 modules would duplicate the facade.
                exclude(":core:local", ":core:remote")
            }
        }
        // Everything excluded here is reachable only at runtime, so DAGP cannot see a reference and
        // reports it unused. Keeping the list explicit means any *new* unused-dependency advice is a
        // real regression rather than known noise.
        project(":app") {
            onUnusedDependencies {
                // Feature `impl` modules are wired in per build type purely so Hilt picks up their
                // @Module bindings; dropping them removes the feature from the built app at runtime.
                exclude(":features:kyc:impl")
                // CameraX is split into runtime-wired artifacts: camera-lifecycle and camera-view back
                // the preview/ProcessCameraProvider machinery that camera2 (runtimeOnly) drives.
                exclude(
                    "androidx.camera:camera-lifecycle",
                    "androidx.camera:camera-view",
                )
                // Guava's listenablefuture stub exists solely to resolve the duplicate-class conflict
                // between guava and the standalone ListenableFuture artifact.
                exclude("com.google.guava:listenablefuture")
                // OAID collection is a drop-in runtime add-on for the AppsFlyer SDK; it has no API.
                exclude("com.appsflyer:oaid")
                // Moshi adapters are registered reflectively when building the Moshi instance.
                exclude("com.squareup.moshi:moshi-adapters")
                // :data:wallet-connect excludes app.cash.sqldelight:android-driver from both reown
                // artifacts, so these unexcluded declarations are what supply that driver at runtime.
                // Nothing here references reown by type — dropping them crashes the app on start.
                exclude(
                    "com.reown:android-core",
                    "com.reown:walletkit",
                )
                // agcp brings the AGConnect runtime the huawei flavor reads through
                // AGConnectOptionsBuilder in HuaweiPushNotificationsTokenProvider.
                exclude("com.huawei.agconnect:agcp")
                // The androidx JUnit runner is instrumentation infrastructure, never imported.
                exclude("androidx.test.ext:junit")
            }
            // Declaring junit4 directly breaks resolution: the main runtime classpath pins it to
            // strictly 4.12, and AGP's consistent resolution then rejects the 4.13.2 the catalog
            // carries. It arrives transitively through espresso/kaspresso at the pinned version.
            onUsedTransitiveDependencies {
                exclude("junit:junit")
                // Deep transitives of libraries :app already declares — cardview via the legacy
                // material widgets, coroutines-play-services via the Play libraries. Neither is a
                // dependency anyone chose, and neither belongs to a family a bundle could group.
                exclude(
                    "androidx.cardview:cardview",
                    "org.jetbrains.kotlinx:kotlinx-coroutines-play-services",
                )
            }
        }
        // Same CameraX runtime split as in :app.
        project(":features:qr-scanning:impl") {
            onUnusedDependencies {
                exclude(
                    "androidx.camera:camera-lifecycle",
                    "com.google.guava:listenablefuture",
                )
            }
            // DAGP wants mlkit barcode-scanning demoted to runtimeOnly and the Play-services-backed
            // artifact declared instead. MLKitBarcodeAnalyzer imports BarcodeScanning directly, and
            // swapping the provider would move barcode detection from the bundled model to one
            // delivered by Play Services — which the huawei flavor does not have. Keep it compile-scoped.
            onRuntimeOnly {
                exclude("com.google.mlkit:barcode-scanning")
            }
            onUsedTransitiveDependencies {
                // Internals of the bundled ML Kit barcode scanner. play-services-mlkit-barcode-scanning
                // is the Play-services-backed alternative to the declared bundled artifact — declaring
                // it is the provider swap refused above, and the other three are its own transitives.
                exclude(
                    "com.google.android.gms:play-services-mlkit-barcode-scanning",
                    "com.google.android.gms:play-services-tasks",
                    "com.google.mlkit:barcode-scanning-common",
                    "com.google.mlkit:vision-common",
                )
            }
        }
        // detekt-rules compiles against the Detekt API, which exposes kotlin-compiler-embeddable types.
        // The advised 2.0.21 is Gradle's embedded Kotlin, not the project's — declaring it would put a
        // second, older Kotlin compiler in the catalog next to kotlin = 2.1.10.
        project(":plugins:detekt-rules") {
            onUsedTransitiveDependencies {
                exclude("org.jetbrains.kotlin:kotlin-compiler-embeddable")
            }
        }
        // :libs:visa uses no Android APIs, but its `packaging { resources { excludes += "/META-INF/*" } }`
        // resolves the duplicate META-INF entries web3j ships. A JVM module has no packaging block, so
        // the conflict would move to every consumer.
        project(":libs:visa") {
            onModuleStructure {
                severity("ignore")
            }
        }
        // :features:hot-wallet:impl is the only path that pulls the cloud-backup modules into the app
        // graph, and data:cloud-backup contributes CloudBackupDataModule's Hilt bindings. Nothing
        // references them by type, so DAGP calls them unused — dropping them would remove the bindings.
        project(":features:hot-wallet:impl") {
            onUnusedDependencies {
                exclude(":data:cloud-backup", ":domain:cloud-backup")
            }
        }
        // material is pulled in through XML themes (styles.xml inherits MaterialComponents), which DAGP
        // cannot see — the same resource-only usage already excluded for :core:ui below.
        project(":features:staking:impl") {
            onUnusedDependencies {
                exclude("com.google.android.material:material")
            }
        }
        // :test:core deliberately re-exports the unit-test stack as `api` — it is the documented single
        // entry point for test modules (testImplementation(projects.test.core)). It never uses turbine
        // itself, so DAGP calls it unused; removing it would break every module relying on the re-export.
        project(":test:core") {
            onUnusedDependencies {
                exclude("app.cash.turbine:turbine")
            }
        }
        // core:ui declares ComposableContentComponent, the supertype every feature Component implements.
        // It reaches this module through :features:survey:api, so DAGP sees no direct reference and calls
        // it unused — but kapt needs the supertype on the classpath to generate the Hilt stubs.
        project(":features:survey:impl") {
            onUnusedDependencies {
                exclude(":core:ui")
            }
        }
        // material is consumed only via resources (styles.xml inherits MaterialComponents themes), which
        // DAGP can't see — it suggests runtimeOnly, but resource linking needs it on the compile
        // classpath. Keep it `implementation`.
        project(":core:ui") {
            onRuntimeOnly {
                exclude("com.google.android.material:material")
            }
        }
        // core:error exposes UniversalError, a supertype of VisaActivationError that card consumes via
        // domain:visa:models. The compiler needs the supertype on the classpath, but DAGP sees no direct
        // reference and flags it unused — false positive, keep it `implementation`.
        project(":domain:card") {
            onUnusedDependencies {
                exclude(":core:error")
            }
        }
        // The only thing this module takes from the data-layer facade is the DataStore helper pair
        // (AppDataStoreFactory, MoshiDataStoreSerializer), which lives in :core:local and arrives through
        // `api(projects.core.local)` in :core:datasource. Nothing in :core:datasource itself is referenced,
        // so DAGP calls the facade unused — but :core:local is excluded from the "declare directly" advice
        // above precisely so modules keep reaching it through the facade. Dropping the declaration would
        // take :core:local off the classpath and stop the module compiling.
        project(":data:search") {
            onUnusedDependencies {
                exclude(":core:datasource")
            }
        }
    }
}

val clean by tasks.registering {
    delete(rootProject.buildDir)
}

interface Injected {
    @get:Inject
    val fs: FileSystemOperations
}

// Test task to run unit tests for debug/googleDebug variant (Android) and all JVM modules
val unitTest by tasks.registering {
    group = "verification"
    description = "Run unit tests for debug/googleDebug variant and all JVM modules"
}

/**
 * Fails if a name in [dagpIgnoredSourceSets] is not a real Android source set of this module.
 *
 * DAGP resolves `ignoreSourceSet` against `CommonExtension.sourceSets` by exact string, so a typo or a
 * build type renamed in `BuildType` is not an error there — the entry silently stops matching and the
 * declarations it was meant to drop come back as unused-dependency advice. Runs only where the list is
 * actually applied: validating it on an ordinary build would put a new way to fail into every assemble,
 * test and IDE sync for no gain. Pure-JVM modules have no Android extension and return early.
 */
fun Project.verifyDagpIgnoredSourceSets() {
    if (!isTrimmedVariantRun) return
    val sourceSets = extensions.findByType(CommonExtension::class.java)?.sourceSets?.names ?: return
    val unknown = dagpIgnoredSourceSets.filterNot { it in sourceSets }
    if (unknown.isEmpty()) return

    error(
        "Dependency analysis ignores source sets that do not exist in $path: ${unknown.joinToString()}. " +
            "Run `./gradlew $path:sourceSets` for the names this module has. Build types come from " +
            "BuildType.kt in plugins/configuration; renaming one means updating `dagpIgnoredSourceSets` " +
            "here and IGNORED_VARIANTS in .github/workflows/run_code_quality.yml together.",
    )
}

/**
 * Fails if the CI-supplied variant list is malformed. No-op locally, where the property is absent.
 *
 * `Flags.androidIgnoredVariants()` splits the value on ',' without trimming, so a stray space after a
 * comma matches nothing and that variant is analysed after all — silently, and in the direction that
 * produces advice rather than hiding it. Only the shape is checked, never the exact contents: which
 * variants CI pays to analyse is a cost decision that is allowed to change without touching this file.
 */
fun Project.verifyDagpIgnoredVariantsProperty() {
    val raw = dagpIgnoredVariants ?: return
    val android = extensions.findByType(CommonExtension::class.java) ?: return

    val buildTypes = android.buildTypes.names.toList()
    // Library variants are bare build types; only :app is flavored, where they are <flavor><BuildType>.
    val known = buildTypes + android.productFlavors.names.flatMap { flavor ->
        buildTypes.map { flavor + it.replaceFirstChar(Char::uppercaseChar) }
    }
    val problems = raw.split(",").filterNot { it.isNotBlank() && it == it.trim() && it in known }
    if (problems.isEmpty()) return

    error(
        "-Pdependency.analysis.android.ignored.variants has entries that match no variant of $path: " +
            "${problems.joinToString { "\"$it\"" }}. Known: ${known.sorted().joinToString()}. Blank " +
            "entries and stray whitespace count — the value is split on ',' and compared literally. " +
            "It is set by IGNORED_VARIANTS in .github/workflows/run_code_quality.yml.",
    )
}

subprojects {
    // Dependency Analysis (DAGP) registers `projectHealth`/`reason` on each module. In 3.x the
    // root application no longer auto-applies to subprojects, so apply it here. Reusing the plugin
    // already resolved by the root `plugins {}` block keeps it in the same classloader as AGP/Kotlin.
    apply(plugin = "com.autonomousapps.dependency-analysis")

    // App module
    plugins.withId("com.android.application") {
        // Both checks run in `afterEvaluate` because the build types they compare against are created
        // by the `configuration` convention plugin, after AGP itself is applied.
        afterEvaluate {
            unitTest.configure { dependsOn(tasks.named("testGoogleDebugUnitTest")) }
            verifyDagpIgnoredSourceSets()
            // :app is the only flavored module, so it is the only one that can validate the
            // <flavor><BuildType> spellings the workflow passes.
            verifyDagpIgnoredVariantsProperty()
        }
    }

    // Android libraries
    plugins.withId("com.android.library") {
        afterEvaluate {
            unitTest.configure { dependsOn(tasks.named("testDebugUnitTest")) }
            verifyDagpIgnoredSourceSets()
        }
    }

    // Jvm modules
    plugins.withId("org.jetbrains.kotlin.jvm") {
        if (!plugins.hasPlugin("com.android.library") && !plugins.hasPlugin("com.android.application")) {
            unitTest.configure { dependsOn(tasks.named("test")) }
        }
    }
}

val assembleInternalQA by tasks.registering {
    group = "build"
    description = "Builds internal APK to 'build/outputs' directory"

    val appOutputApkDir = "$projectDir/app/build/outputs/apk/internal"
    val rootOutputApkDir = "$buildDir/outputs"
    val injected = objects.newInstance<Injected>()

    dependsOn(":app:assembleInternal")

    doFirst {
        injected.fs.delete {
            delete(appOutputApkDir)
            delete("$rootOutputApkDir/app-internal.apk")
        }
    }
    doLast {
        injected.fs.copy {
            from("$appOutputApkDir/app-internal.apk")
            into(rootOutputApkDir)
        }
    }
}

val assembleExternalQA by tasks.registering {
    group = "build"
    description = "Builds external APK to 'build/outputs' directory"

    val appOutputApkDir = "$projectDir/app/build/outputs/apk/external"
    val rootOutputApkDir = "$buildDir/outputs"
    val injected = objects.newInstance<Injected>()

    dependsOn(":app:assembleExternal")

    doFirst {
        injected.fs.delete {
            delete(appOutputApkDir)
            delete("$rootOutputApkDir/app-external.apk")
        }
    }
    doLast {
        injected.fs.copy {
            from("$appOutputApkDir/app-external.apk")
            into(rootOutputApkDir)
        }
    }
}

val assembleQA by tasks.registering {
    group = "build"
    description = "Builds internal and external APKs to 'build/outputs' directory"

    dependsOn(assembleInternalQA)
    dependsOn(assembleExternalQA)
}

val generateComposeMetrics by tasks.registering {
    group = "other"
    description = "Build external APK and generates compose metrics to 'build/compose-metrics' directory"

    subprojects {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            if (name.contains("compile")) {
                val outputDirectory = "${project.buildDir.absolutePath}/compose_metrics"
                compilerOptions {
                    freeCompilerArgs.addAll(
                        listOf(
                            "-P",
                            "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$outputDirectory",
                            "-P",
                            "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=$outputDirectory"
                            //     "-P",
                            //     "plugin:androidx.compose.compiler.plugins.kotlin:experimentalStrongSkipping=true",
                        )
                    )
                }
            }
        }
    }

    finalizedBy(assembleExternalQA)
}