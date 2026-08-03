package com.tangem.features.polymarket.impl.onboarding.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketAccessMode
import com.tangem.domain.polymarket.model.PolymarketEntry
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketGateStepTest {

    @ParameterizedTest
    @ProvideTestModels
    fun toGateStep(model: GateStepModel) {
        assertThat(model.entry.toGateStep()).isEqualTo(model.expected)
    }

    internal data class GateStepModel(val entry: PolymarketEntry, val expected: PolymarketGateStep)

    private fun provideTestModels() = listOf(
        GateStepModel(
            entry = PolymarketEntry.Onboard(PolymarketWalletStatus.NOT_CREATED),
            expected = PolymarketGateStep.ShowWelcome,
        ),
        GateStepModel(
            entry = PolymarketEntry.Onboard(PolymarketWalletStatus.DEPLOYED),
            expected = PolymarketGateStep.ShowWelcome,
        ),
        GateStepModel(
            entry = PolymarketEntry.Trade,
            expected = PolymarketGateStep.OpenFeed(PolymarketAccessMode.TRADING),
        ),
        GateStepModel(
            entry = PolymarketEntry.ReadOnly,
            expected = PolymarketGateStep.OpenFeed(PolymarketAccessMode.READ_ONLY),
        ),
        GateStepModel(
            entry = PolymarketEntry.RegionBlocked,
            expected = PolymarketGateStep.ShowRegionRestrictions,
        ),
    )
}