package com.tangem.core.remote.moshi

import com.squareup.moshi.Moshi

/**
 * Contributes adapters to the shared network [Moshi] builder.
 *
 * Modules provide implementations via Dagger `@IntoSet` so the central builder can be assembled from
 * every contributor without depending on the module each contributor lives in. This lets stream
 * modules register their own (e.g. enum-fallback) adapters without the core builder knowing about them.
 */
fun interface NetworkMoshiConfigurer {

    fun configure(builder: Moshi.Builder): Moshi.Builder
}