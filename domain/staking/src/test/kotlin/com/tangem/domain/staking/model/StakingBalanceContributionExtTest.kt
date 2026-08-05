package com.tangem.domain.staking.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.balance.BalanceContribution
import com.tangem.domain.models.staking.BalanceItem
import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.staking.YieldBalanceItem
import com.tangem.domain.models.yield.supply.YieldSupplyContribution
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

/**
 * Tests [stakingBalanceData] — the single accessor every staking-breakdown reader goes through.
 *
 * It is the gate between the two architectures: `contributions` first, legacy typed field as the fallback, which is
 * what lets a call site stay ignorant of `TWI_1717_BALANCE_CONTRIBUTIONS`. Both directions are asserted here, plus
 * the parity between them, because a wrong answer here silently empties a staking block rather than failing.
 *
 * **Both** halves narrow to [StakingBalance.Data]: `Empty` and `Error` are `BalanceContribution`s in their own right,
 * so an un-narrowed lookup on either side could hand a caller a variant with no breakdown to show. The two models
 * ending in `-> null` are what pin that.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StakingBalanceContributionExtTest {

    @ParameterizedTest
    @ProvideTestModels
    fun stakingBalanceData(model: AccessorTestModel) {
        // Act
        val actual = model.value.stakingBalanceData

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("provideParityTestModels")
    fun `GIVEN the same balance WHEN reached either way THEN both paths agree`(model: ParityTestModel) {
        // Arrange — the same balance reached through each architecture
        val viaContributions = value(contributions = listOf(model.balance))
        val viaLegacyField = value(stakingBalance = model.balance)

        // Act
        val fromContributions = viaContributions.stakingBalanceData
        val fromLegacy = viaLegacyField.stakingBalanceData

        // Assert
        assertThat(fromContributions).isEqualTo(fromLegacy)
        assertThat(fromContributions).isEqualTo(model.balance)
    }

    private fun provideTestModels() = listOf(
        AccessorTestModel(
            description = "contribution present -> returned",
            value = value(contributions = listOf(STAKE_KIT)),
            expected = STAKE_KIT,
        ),
        AccessorTestModel(
            description = "contributions empty, legacy field set -> falls back to the legacy field",
            value = value(stakingBalance = STAKE_KIT),
            expected = STAKE_KIT,
        ),
        AccessorTestModel(
            description = "both present -> the contribution wins, never the legacy field",
            value = value(contributions = listOf(STAKE_KIT), stakingBalance = P2P_ETH_POOL),
            expected = STAKE_KIT,
        ),
        AccessorTestModel(
            description = "neither present -> null",
            value = value(),
            expected = null,
        ),
        AccessorTestModel(
            description = "only a non-staking contribution -> still falls back, does not stop at the first entry",
            value = value(contributions = listOf(YIELD_SUPPLY), stakingBalance = STAKE_KIT),
            expected = STAKE_KIT,
        ),
        AccessorTestModel(
            description = "only a non-staking contribution and no legacy field -> null",
            value = value(contributions = listOf(YIELD_SUPPLY)),
            expected = null,
        ),
        AccessorTestModel(
            description = "staking contribution alongside another kind -> the staking one is picked",
            value = value(contributions = listOf(YIELD_SUPPLY, P2P_ETH_POOL)),
            expected = P2P_ETH_POOL,
        ),
        AccessorTestModel(
            description = "legacy field holds a non-Data variant -> null, matching what the factory stores",
            value = value(stakingBalance = EMPTY),
            expected = null,
        ),
        AccessorTestModel(
            description = "contribution is a non-Data variant -> null, the contributions half narrows too",
            value = value(contributions = listOf(EMPTY)),
            expected = null,
        ),
        AccessorTestModel(
            description = "non-Data contribution over a Data legacy field -> the legacy Data one, not the Empty",
            value = value(contributions = listOf(EMPTY), stakingBalance = STAKE_KIT),
            expected = STAKE_KIT,
        ),
    )

    private fun provideParityTestModels() = listOf(
        ParityTestModel(description = "StakeKit", balance = STAKE_KIT),
        ParityTestModel(description = "P2PEthPool", balance = P2P_ETH_POOL),
    )

    internal data class AccessorTestModel(
        val description: String,
        val value: CryptoCurrencyStatus.Value,
        val expected: StakingBalance.Data?,
    ) {

        override fun toString(): String = description
    }

    internal data class ParityTestModel(val description: String, val balance: StakingBalance.Data) {

        override fun toString(): String = description
    }

    // region Fixtures
    private fun value(
        contributions: List<BalanceContribution> = emptyList(),
        stakingBalance: StakingBalance? = null,
    ): CryptoCurrencyStatus.Value {
        return CryptoCurrencyStatus.Loaded(
            amount = BigDecimal.TEN,
            fiatAmount = BigDecimal.TEN,
            fiatRate = BigDecimal.ONE,
            priceChange = BigDecimal.ZERO,
            stakingBalance = stakingBalance,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = mockk(relaxed = true),
            sources = CryptoCurrencyStatus.Sources(),
            contributions = contributions,
        )
    }

    private companion object {

        val STAKING_ID = StakingID(integrationId = "integration", address = "0x1")

        /**
         * Real [StakingBalance.Data] instances, never mocks: `totalDeltaCryptoAmount()` is a member function, so
         * MockK intercepts it and a relaxed mock returns a mocked `BigDecimal` — an assertion could pass while
         * testing nothing. [com.tangem.domain.models.staking.BalanceItem] is a plain carrier and is safe to mock.
         */
        val STAKE_KIT = StakingBalance.Data.StakeKit(
            stakingId = STAKING_ID,
            source = StatusSource.ACTUAL,
            balance = YieldBalanceItem(
                items = listOf(
                    mockk<BalanceItem>(relaxed = true) {
                        every { amount } returns BigDecimal.ONE
                        every { type } returns BalanceType.STAKED
                    },
                ),
                integrationId = "integration",
            ),
        )

        val P2P_ETH_POOL = StakingBalance.Data.P2PEthPool(
            stakingId = STAKING_ID,
            source = StatusSource.ACTUAL,
            accounts = emptyList(),
        )

        val YIELD_SUPPLY = YieldSupplyContribution(
            status = mockk<YieldSupplyStatus>(relaxed = true),
            source = StatusSource.ACTUAL,
        )

        /** A staking [BalanceContribution] with no breakdown — the variant both halves of the accessor must reject. */
        val EMPTY = StakingBalance.Empty(stakingId = STAKING_ID, source = StatusSource.ACTUAL)
    }
    // endregion
}