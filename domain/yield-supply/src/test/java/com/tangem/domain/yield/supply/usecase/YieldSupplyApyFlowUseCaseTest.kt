package com.tangem.domain.yield.supply.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.domain.yield.supply.models.incentives.YieldIncentiveCampaign
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.yield.MockYieldSupply
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldSupplyApyFlowUseCaseTest {

    private val repository: YieldSupplyRepository = mockk()

    private val useCase = YieldSupplyApyFlowUseCase(yieldSupplyRepository = repository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun invoke(model: ApyModel) = runTest {
        // Arrange
        every { repository.getMarketsFlow() } returns flowOf(listOf(model.market))

        // Act
        val actual = useCase().first()

        // Assert
        assertThat(actual).containsExactly(YIELD_SUPPLY_KEY, model.expected)
    }

    private fun provideTestModels() = listOf(
        ApyModel(market = market(hasCampaign = true), expected = BigDecimal("5.5")),
        ApyModel(market = market(hasCampaign = false), expected = BigDecimal("4")),
        ApyModel(market = market(campaign = null), expected = BigDecimal("4")),
    )

    @Test
    fun `GIVEN inactive market WHEN apy flow emits THEN it is skipped`() = runTest {
        // Arrange
        every { repository.getMarketsFlow() } returns flowOf(listOf(market(hasCampaign = true, isActive = false)))

        // Act
        val actual = useCase().first()

        // Assert
        assertThat(actual).isEmpty()
    }

    internal data class ApyModel(val market: YieldMarketToken, val expected: BigDecimal)

    private fun market(
        hasCampaign: Boolean = false,
        isActive: Boolean = true,
        campaign: YieldIncentiveCampaign? = MockYieldSupply.campaign(hasActiveCampaign = hasCampaign),
    ) = MockYieldSupply.market(apy = BigDecimal("4"), isActive = isActive, campaign = campaign)

    private companion object {
        val YIELD_SUPPLY_KEY = "${MockYieldSupply.NETWORK_ID}_${MockYieldSupply.TOKEN_ADDRESS.lowercase()}"
    }
}