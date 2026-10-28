package com.tangem.features.yield.supply.impl.active.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.yield.MockYieldSupply.campaign
import com.tangem.test.mock.yield.MockYieldSupply.market
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ApyBreakdownTest {

    @ParameterizedTest
    @ProvideTestModels
    fun resolveApyBreakdown(model: BreakdownModel) {
        // Act
        val actual = resolveApyBreakdown(market = model.market)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class BreakdownModel(val market: YieldMarketToken, val expected: ApyBreakdown?)

    private fun provideTestModels() = listOf(
        BreakdownModel(
            market = market(campaign = campaign(rewardsApy = BigDecimal("1.40"))),
            expected = ApyBreakdown(baseApy = BigDecimal("3.09"), rewardsApy = BigDecimal("1.40")),
        ),
        BreakdownModel(
            market = market(campaign = campaign(hasActiveCampaign = false, rewardsApy = BigDecimal("1.40"))),
            expected = null,
        ),
        BreakdownModel(
            market = market(campaign = campaign(rewardsApy = BigDecimal.ZERO)),
            expected = null,
        ),
        BreakdownModel(market = market(campaign = null), expected = null),
    )
}