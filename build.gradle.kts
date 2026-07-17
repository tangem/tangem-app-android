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
            // DAGP 3.16.0 -> kotlin-metadata-jvm:2.2.21 -> kotlin-bom:2.2.21, whose platform
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

// Dependency Analysis (DAGP) global configuration.
dependencyAnalysis {
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
            // hilt-android / hilt-core get flagged as `api`: Kotlin `internal` @Inject classes (and
            // public @Inject impls) compile to public bytecode carrying RUNTIME javax.inject annotations,
            // so DAGP sees javax.inject in the ABI. The Hilt runtime is never genuinely public API — keep
            // it `implementation` repo-wide.
            onIncorrectConfiguration {
                exclude(
                    "com.google.dagger:hilt-android",
                    "com.google.dagger:hilt-core",
                )
            }
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
            }
        }
        // In :libs:auth these are injected only into `internal` classes (DI modules / Default* impls).
        // DAGP advises `api` because Kotlin `internal` compiles to public bytecode (and Hilt's
        // generated `_Factory` classes expose the constructor types publicly) — a false positive, not
        // a real ABI leak. Keep them `implementation`. Scoped to this module so genuine api advice in
        // other modules still surfaces.
        project(":libs:auth") {
            onIncorrectConfiguration {
                exclude(
                    "com.squareup.moshi:moshi",
                    ":core:config-toggles",
                    ":core:datasource",
                    ":core:utils",
                )
            }
        }
        // Same Kotlin-`internal`-compiles-to-public false positive: these are used only inside
        // `internal` classes / DI modules / method bodies (verified), not in the public ABI.
        project(":libs:blockchain-sdk") {
            onIncorrectConfiguration {
                exclude(
                    "androidx.datastore:datastore-preferences",
                    "com.squareup.moshi:moshi",
                    ":core:analytics",
                    ":core:utils",
                )
            }
        }
        // config-toggles is used only inside the `internal` DefaultCardSdkFeatureToggles (the public
        // CardSdkFeatureToggles interface is empty) — internal→public false positive.
        project(":libs:tangem-sdk-api") {
            onIncorrectConfiguration {
                exclude(":core:config-toggles")
            }
        }
        // web3j leaks into the ABI only via incidentally-public generated contract wrappers
        // (ERC20/TangemPaymentAccount/…), not the module's intended public API (VisaContractInfoProvider
        // / VisaContractInfo) — keep it `implementation` (see PR review), so silence the `api` advice.
        project(":libs:visa") {
            onIncorrectConfiguration {
                exclude("org.web3j:core")
            }
        }
        // datasource/utils are used only inside the `internal` DI module + Amplitude impl (the public
        // ABTestsManager interface doesn't expose them) — internal→public false positive.
        project(":core:ab-tests") {
            onIncorrectConfiguration {
                exclude(":core:datasource", ":core:utils")
            }
        }
        // datasource is used only inside `internal` DI modules + LocalTogglesStorage (the public
        // FeatureTogglesManager API doesn't expose it) — internal→public false positive.
        project(":core:config-toggles") {
            onIncorrectConfiguration {
                exclude(":core:datasource")
            }
        }
        // moshi-polymorphic-adapter is used only in @Provides bodies / as annotation args, never in a
        // public signature — internal→public false positive, keep it `implementation`.
        project(":core:datasource") {
            onIncorrectConfiguration {
                exclude("dev.onenowy.moshipolymorphicadapter:moshi-polymorphic-adapter")
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
        // core:utils is deliberately re-exported as api from the ubiquitous domain:models module so the
        // many consumers that use TangemLogger / utils through it keep compiling. Demoting it to
        // implementation would cascade across the whole repo, so silence the incorrect-config advice.
        project(":domain:models") {
            onIncorrectConfiguration {
                exclude(":core:utils")
            }
        }
        // :domain:models is kept as api because domain:markets:models exposes CryptoCurrency.RawID (a
        // domain:models type) in its public data classes (TokenMarket/RawMarketToken/TokenMarketParams).
        // DAGP misses this nested-type ABI leak and advises implementation; that advice is a false negative.
        project(":domain:markets:models") {
            onIncorrectConfiguration {
                exclude(":domain:models")
            }
        }
        // :domain:core is kept as api in :domain:legacy and :domain:express because their public APIs
        // (RampStateManager, ExpressServiceFetcher#getInitializationStatus) return Flow<Lce<...>> where Lce
        // is a domain:core type. DAGP doesn't trace the generic type argument into the ABI and advises
        // implementation; that advice is a false negative.
        project(":domain:legacy") {
            onIncorrectConfiguration {
                exclude(":domain:core")
            }
        }
        project(":domain:express") {
            onIncorrectConfiguration {
                exclude(":domain:core")
            }
        }
        // :domain:core kept as api in :domain:onramp — public use cases (GetOnrampCurrenciesUseCase etc.)
        // return EitherFlow<...> (a domain:core alias). DAGP doesn't trace the alias/generic into the ABI.
        project(":domain:onramp") {
            onIncorrectConfiguration {
                exclude(":domain:core")
            }
        }
        // arrow-core kept as api in :domain:onboarding — WasTwinsOnboardingShownUseCase#invoke returns
        // Flow<Either<...>> (arrow.core.Either in the public ABI). DAGP advises implementation here (false
        // negative on the generic type argument).
        project(":domain:onboarding") {
            onIncorrectConfiguration {
                exclude("io.arrow-kt:arrow-core")
            }
        }
        // :domain:core kept as api in :domain:wallets — GetUserWalletUseCase#invokeFlow returns
        // EitherFlow<...> (a domain:core alias). DAGP false-negative on the alias/generic.
        project(":domain:wallets") {
            onIncorrectConfiguration {
                exclude(":domain:core")
            }
        }
        // :domain:models kept as api in :domain:yield-supply:models — YieldMarketToken (public data
        // class) exposes SerializedBigDecimal (a domain:models type) in public fields. DAGP misses it.
        project(":domain:yield-supply:models") {
            onIncorrectConfiguration {
                exclude(":domain:models")
            }
        }
        // :common is deliberately re-exported as api from common:ui (a ubiquitous UI dependency) so the
        // many feature modules that use TangemBlogUrlBuilder / common types through it keep compiling.
        // Demoting it to implementation would cascade across the feature graph.
        project(":common:ui") {
            onIncorrectConfiguration {
                exclude(":common")
            }
        }
        // libs:crypto is deliberately re-exported as api from the ubiquitous :common module so the many
        // feature modules that use BlockchainUtils / crypto helpers through it keep compiling. Demoting
        // it to implementation would cascade across the feature graph.
        project(":common") {
            onIncorrectConfiguration {
                exclude(":libs:crypto")
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

subprojects {
    // Dependency Analysis (DAGP) registers `projectHealth`/`reason` on each module. In 3.x the
    // root application no longer auto-applies to subprojects, so apply it here. Reusing the plugin
    // already resolved by the root `plugins {}` block keeps it in the same classloader as AGP/Kotlin.
    apply(plugin = "com.autonomousapps.dependency-analysis")

    // App module
    plugins.withId("com.android.application") {
        afterEvaluate {
            unitTest.configure { dependsOn(tasks.named("testGoogleDebugUnitTest")) }
        }
    }

    // Android libraries
    plugins.withId("com.android.library") {
        afterEvaluate {
            unitTest.configure { dependsOn(tasks.named("testDebugUnitTest")) }
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