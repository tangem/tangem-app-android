package com.tangem.domain.yield.supply.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.yield.supply.incentives.YieldIncentivesRepository
import com.tangem.domain.yield.supply.models.incentives.YieldIncentives
import com.tangem.domain.yield.supply.models.incentives.YieldIncentivesError
import com.tangem.domain.yield.supply.models.incentives.YieldReward
import com.tangem.domain.yield.supply.models.incentives.YieldRewardDestination
import com.tangem.domain.yield.supply.models.incentives.YieldRewardsSummary
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.yield.MockYieldSupply
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetYieldRewardsSummaryUseCaseTest {

    private val repository: YieldIncentivesRepository = mockk()

    private val useCase = GetYieldRewardsSummaryUseCase(yieldIncentivesRepository = repository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun invoke(model: SummaryModel) = runTest {
        // Arrange
        coEvery {
            repository.getIncentives(chainId = CHAIN_ID, userAddress = USER_ADDRESS)
        } returns incentives(
            moduleAddress = model.moduleAddress,
            isClaimRecommended = model.isClaimRecommended,
            rewards = model.rewards,
        ).right()

        // Act
        val actual = useCase(chainId = CHAIN_ID, userAddress = USER_ADDRESS)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN incentives unavailable WHEN summarised THEN error propagated`() = runTest {
        // Arrange
        coEvery {
            repository.getIncentives(chainId = CHAIN_ID, userAddress = USER_ADDRESS)
        } returns YieldIncentivesError.Unavailable.left()

        // Act
        val actual = useCase(chainId = CHAIN_ID, userAddress = USER_ADDRESS)

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(YieldIncentivesError.Unavailable)
    }

    internal data class SummaryModel(
        val moduleAddress: String?,
        val isClaimRecommended: Boolean,
        val rewards: List<YieldReward>,
        val expected: YieldRewardsSummary,
    )

    @Suppress("LongMethod")
    private fun provideTestModels(): List<SummaryModel> {
        val weth = reward(symbol = "WETH", claimable = "1000", netUsd = "1.00")
        val aave = reward(symbol = "AAVE", claimable = "2000", netUsd = "0.30")
        val unsupported = reward(
            symbol = "GHO",
            claimable = "3000",
            netUsd = "9.00",
            destination = YieldRewardDestination.Unsupported,
        )
        val pending = reward(symbol = "USDC", pending = "500")
        val drained = reward(symbol = "WBTC")
        val malformed = reward(symbol = "BAD", rewardToken = "", claimable = "4000", netUsd = "5.00")
        val unsupportedPending = reward(
            symbol = "stkGHO",
            pending = "700",
            destination = YieldRewardDestination.Unsupported,
        )

        return listOf(
            SummaryModel(
                moduleAddress = MODULE_ADDRESS,
                isClaimRecommended = true,
                rewards = listOf(weth, pending, unsupported),
                expected = YieldRewardsSummary.Claimable(
                    moduleAddress = MODULE_ADDRESS,
                    claimable = listOf(weth),
                    pending = listOf(pending),
                    unsupported = listOf(unsupported),
                    supportedRewardTokens = setOf(token("WETH"), token("USDC")),
                    totalNetUsd = BigDecimal("1.00"),
                ),
            ),
            SummaryModel(
                moduleAddress = MODULE_ADDRESS,
                isClaimRecommended = true,
                rewards = listOf(weth, aave),
                expected = YieldRewardsSummary.Claimable(
                    moduleAddress = MODULE_ADDRESS,
                    claimable = listOf(weth, aave),
                    pending = emptyList(),
                    unsupported = emptyList(),
                    supportedRewardTokens = setOf(token("WETH"), token("AAVE")),
                    totalNetUsd = BigDecimal("1.30"),
                ),
            ),
            SummaryModel(
                moduleAddress = MODULE_ADDRESS,
                isClaimRecommended = false,
                rewards = listOf(aave, pending),
                expected = YieldRewardsSummary.Deferred(
                    moduleAddress = MODULE_ADDRESS,
                    claimable = listOf(aave),
                    pending = listOf(pending),
                    unsupported = emptyList(),
                    supportedRewardTokens = setOf(token("AAVE"), token("USDC")),
                    totalNetUsd = BigDecimal("0.30"),
                ),
            ),
            SummaryModel(
                moduleAddress = MODULE_ADDRESS,
                isClaimRecommended = false,
                rewards = listOf(aave, unsupported),
                expected = YieldRewardsSummary.Deferred(
                    moduleAddress = MODULE_ADDRESS,
                    claimable = listOf(aave),
                    pending = emptyList(),
                    unsupported = listOf(unsupported),
                    supportedRewardTokens = setOf(token("AAVE")),
                    totalNetUsd = BigDecimal("0.30"),
                ),
            ),
            SummaryModel(
                moduleAddress = MODULE_ADDRESS,
                isClaimRecommended = false,
                rewards = listOf(weth, aave),
                expected = YieldRewardsSummary.Deferred(
                    moduleAddress = MODULE_ADDRESS,
                    claimable = listOf(weth, aave),
                    pending = emptyList(),
                    unsupported = emptyList(),
                    supportedRewardTokens = setOf(token("WETH"), token("AAVE")),
                    totalNetUsd = BigDecimal("1.30"),
                ),
            ),
            SummaryModel(
                moduleAddress = MODULE_ADDRESS,
                isClaimRecommended = true,
                rewards = listOf(pending, unsupported),
                expected = YieldRewardsSummary.PendingOnly(pending = listOf(pending)),
            ),
            SummaryModel(
                moduleAddress = MODULE_ADDRESS,
                isClaimRecommended = true,
                rewards = listOf(weth, pending, unsupportedPending),
                expected = YieldRewardsSummary.Claimable(
                    moduleAddress = MODULE_ADDRESS,
                    claimable = listOf(weth),
                    pending = listOf(pending),
                    unsupported = listOf(unsupportedPending),
                    supportedRewardTokens = setOf(token("WETH"), token("USDC")),
                    totalNetUsd = BigDecimal("1.00"),
                ),
            ),
            SummaryModel(
                moduleAddress = MODULE_ADDRESS,
                isClaimRecommended = true,
                rewards = listOf(unsupportedPending),
                expected = YieldRewardsSummary.None,
            ),
            SummaryModel(
                moduleAddress = MODULE_ADDRESS,
                isClaimRecommended = true,
                rewards = listOf(unsupported),
                expected = YieldRewardsSummary.None,
            ),
            SummaryModel(
                moduleAddress = MODULE_ADDRESS,
                isClaimRecommended = true,
                rewards = listOf(weth, drained),
                expected = YieldRewardsSummary.Claimable(
                    moduleAddress = MODULE_ADDRESS,
                    claimable = listOf(weth),
                    pending = emptyList(),
                    unsupported = emptyList(),
                    supportedRewardTokens = setOf(token("WETH"), token("WBTC")),
                    totalNetUsd = BigDecimal("1.00"),
                ),
            ),
            SummaryModel(
                moduleAddress = MODULE_ADDRESS,
                isClaimRecommended = true,
                rewards = listOf(weth, malformed),
                expected = YieldRewardsSummary.Claimable(
                    moduleAddress = MODULE_ADDRESS,
                    claimable = listOf(weth),
                    pending = emptyList(),
                    unsupported = listOf(malformed),
                    supportedRewardTokens = setOf(token("WETH")),
                    totalNetUsd = BigDecimal("1.00"),
                ),
            ),
            SummaryModel(
                moduleAddress = MODULE_ADDRESS,
                isClaimRecommended = true,
                rewards = emptyList(),
                expected = YieldRewardsSummary.None,
            ),
            SummaryModel(
                moduleAddress = null,
                isClaimRecommended = true,
                rewards = listOf(weth),
                expected = YieldRewardsSummary.None,
            ),
            SummaryModel(
                moduleAddress = "",
                isClaimRecommended = true,
                rewards = listOf(weth),
                expected = YieldRewardsSummary.None,
            ),
        )
    }

    private fun incentives(moduleAddress: String?, isClaimRecommended: Boolean, rewards: List<YieldReward>) =
        YieldIncentives(
            chainId = CHAIN_ID,
            userAddress = USER_ADDRESS,
            moduleAddress = moduleAddress,
            isClaimRecommended = isClaimRecommended,
            rewards = rewards,
            lastUpdatedAt = Instant.fromEpochSeconds(0),
        )

    private fun reward(
        symbol: String,
        rewardToken: String = token(symbol),
        claimable: String = "0",
        pending: String = "0",
        netUsd: String = "0.00",
        destination: YieldRewardDestination = YieldRewardDestination.Owner,
    ) = MockYieldSupply.reward(
        rewardToken = rewardToken,
        symbol = symbol,
        claimableAmount = BigInteger(claimable),
        pendingAmount = BigInteger(pending),
        usdValue = BigDecimal(netUsd),
        estimatedNetValueUsd = BigDecimal(netUsd),
        estimatedServiceFeeUsd = BigDecimal.ZERO,
        finalDestination = destination,
    )

    private fun token(symbol: String): String = TOKENS.getValue(symbol)

    private companion object {
        val TOKENS = mapOf(
            "WETH" to "0x0000000000000000000000000000000000000001",
            "AAVE" to "0x0000000000000000000000000000000000000002",
            "GHO" to "0x0000000000000000000000000000000000000003",
            "USDC" to "0x0000000000000000000000000000000000000004",
            "WBTC" to "0x0000000000000000000000000000000000000005",
            "stkGHO" to "0x0000000000000000000000000000000000000006",
        )
        const val CHAIN_ID = MockYieldSupply.CHAIN_ID
        const val USER_ADDRESS = "0xuser"
        const val MODULE_ADDRESS = "0xmodule"
    }
}