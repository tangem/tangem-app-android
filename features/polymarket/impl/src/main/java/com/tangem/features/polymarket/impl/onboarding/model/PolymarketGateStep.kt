package com.tangem.features.polymarket.impl.onboarding.model

import com.tangem.domain.polymarket.model.PolymarketAccessMode
import com.tangem.domain.polymarket.model.PolymarketEntry

/** What the gate does with a resolved [PolymarketEntry]: either it renders, or it leaves for the feed. */
internal sealed interface PolymarketGateStep {

    data object ShowWelcome : PolymarketGateStep

    data object ShowRegionRestrictions : PolymarketGateStep

    data class OpenFeed(val accessMode: PolymarketAccessMode) : PolymarketGateStep
}

internal fun PolymarketEntry.toGateStep(): PolymarketGateStep = when (this) {
    is PolymarketEntry.Onboard -> PolymarketGateStep.ShowWelcome
    PolymarketEntry.Trade -> PolymarketGateStep.OpenFeed(accessMode = PolymarketAccessMode.TRADING)
    PolymarketEntry.ReadOnly -> PolymarketGateStep.OpenFeed(accessMode = PolymarketAccessMode.READ_ONLY)
    PolymarketEntry.RegionBlocked -> PolymarketGateStep.ShowRegionRestrictions
}