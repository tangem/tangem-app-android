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

    data class Failed(val onRetryClick: () -> Unit) : PolymarketOnboardingUM
}