package com.tangem.domain.yield.supply.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.domain.yield.supply.models.incentives.YieldIncentiveCampaign
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ResolveYieldApyUseCaseTest {

    private val repository: YieldSupplyRepository = mockk()

    private val useCase = ResolveYieldApyUseCase(yieldSupplyRepository = repository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun resolve(model: ApyModel) = runTest {
        // Arrange
        coEvery { repository.getCachedMarkets() } returns listOf(model.market)

        // Act
        val actual = useCase(chainId = CHAIN_ID, tokenAddress = TOKEN_ADDRESS)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        ApyModel(
            market = market(hasCampaign = false, rewardsApr = "1.5", totalApyEst = "5.5"),
            expected = BigDecimal("4"),
        ),
        ApyModel(
            market = market(hasCampaign = true, rewardsApr = "1.5", totalApyEst = "5.5"),
            expected = BigDecimal("5.5"),
        ),
        ApyModel(market = market(campaign = null), expected = BigDecimal("4")),
        ApyModel(
            market = market(tokenAddress = TOKEN_ADDRESS.uppercase(), hasCampaign = true, totalApyEst = "5.5"),
            expected = BigDecimal("5.5"),
        ),
        ApyModel(market = market(chainId = 137, hasCampaign = true, totalApyEst = "5.5"), expected = null),
        ApyModel(market = market(tokenAddress = "0xOther", hasCampaign = true, totalApyEst = "5.5"), expected = null),
    )

    @Test
    fun `GIVEN empty cache WHEN invoke THEN markets are updated once`() = runTest {
        // Arrange
        coEvery { repository.getCachedMarkets() } returns emptyList()
        coEvery { repository.updateMarkets() } returns listOf(market(hasCampaign = true, totalApyEst = "5.5"))

        // Act
        val actual = useCase(chainId = CHAIN_ID, tokenAddress = TOKEN_ADDRESS)

        // Assert
        assertThat(actual).isEqualTo(BigDecimal("5.5"))
        coVerify(exactly = 1) { repository.updateMarkets() }
    }

    @Test
    fun `GIVEN update fails WHEN invoke THEN null is returned`() = runTest {
        // Arrange
        coEvery { repository.getCachedMarkets() } returns null
        coEvery { repository.updateMarkets() } throws IllegalStateException("no network")

        // Act
        val actual = useCase(chainId = CHAIN_ID, tokenAddress = TOKEN_ADDRESS)

        // Assert
        assertThat(actual).isNull()
    }

    internal data class ApyModel(val market: YieldMarketToken, val expected: BigDecimal?)

    private fun market(
        tokenAddress: String = TOKEN_ADDRESS,
        chainId: Int = CHAIN_ID,
        apy: String = "4",
        hasCampaign: Boolean = false,
        rewardsApr: String = "0",
        totalApyEst: String = "0",
        campaign: YieldIncentiveCampaign? = YieldIncentiveCampaign(
            hasActiveCampaign = hasCampaign,
            rewardsApr = BigDecimal(rewardsApr),
            totalApyEst = BigDecimal(totalApyEst),
        ),
    ) = YieldMarketToken(
        tokenAddress = tokenAddress,
        chainId = chainId,
        apy = BigDecimal(apy),
        isActive = true,
        maxFeeNative = BigDecimal.ZERO,
        maxFeeUSD = BigDecimal.ZERO,
        campaign = campaign,
    )

    private companion object {
        const val CHAIN_ID = 1
        const val TOKEN_ADDRESS = "0xabc"
    }
}