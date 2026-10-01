package com.tangem.features.polymarket.api

interface PolymarketFeatureToggles {
    val isPolymarketEnabled: Boolean

    /** True in tester builds: the feed entry follows [isPolymarketEnabled] without the Amplitude rollout. */
    val isRolloutExperimentBypassed: Boolean
}