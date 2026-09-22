package com.tangem.core.local.moshi

import com.squareup.moshi.Moshi

/**
 * Contributes adapters to the shared [SdkMoshi] builder.
 *
 * Modules provide implementations via Dagger `@IntoSet` so the central builder can be assembled from
 * every contributor without depending on the module each contributor lives in. This lets a module that
 * (de)serializes its own models through the SDK [Moshi] register the required adapters without the core
 * builder knowing about them.
 */
fun interface SdkMoshiConfigurer {

    fun configure(builder: Moshi.Builder): Moshi.Builder
}