package com.tangem.features.polymarket.impl.onboarding.ui.state

import androidx.compose.runtime.Immutable

/** State of the entry gate shown before the Polymarket feed. */
@Immutable
internal sealed interface PolymarketOnboardingUM {

    /**
     * The entry decision is being resolved. Covers the card session the derivation may open — the NFC prompt
     * is the SDK's own UI, so no separate state exists for it.
     */
    data object Loading : PolymarketOnboardingUM

    /** Onboarding is owed. Placeholder content until APP-13 builds the real screen. */
    data object Welcome : PolymarketOnboardingUM

    /** The region forbids trading and there is no wallet to fall back to — the sheet is shown over this. */
    data object RegionBlocked : PolymarketOnboardingUM

    /**
     * The entry decision could not be resolved, so the region is unknown. The gate holds here rather than
     * falling through to the feed or to onboarding — an unresolved region must never be read as permission to
     * trade — and the only way forward is [onRetryClick].
     */
    data class Failed(val onRetryClick: () -> Unit) : PolymarketOnboardingUM
}