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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * Tests [stakingBalanceData] — the single accessor every staking-breakdown reader goes through.
 *
 * It resolves the staking balance out of `contributions`, which is what lets a call site stay ignorant of which
 * provider reported it. A wrong answer here silently empties a staking block rather than failing.
 *
 * The accessor narrows to [StakingBalance.Data]: `Empty` and `Error` are `BalanceContribution`s in their own right,
 * so an un-narrowed lookup could hand a caller a variant with no breakdown to show. The models ending in `-> null`
 * are what pin that.
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

    /**
     * [stakingSource] is the freshness half of the same accessor, and it exists because
     * `CryptoCurrencyStatus.Sources.stakingBalanceSource` no longer carries the staking staleness: the real
     * freshness rides on the contribution itself.
     */
    @Nested
    inner class StakingSourceTest {

        @Test
        fun `GIVEN no staking balance WHEN stakingSource THEN actual`() {
            // Act & Assert — nothing to be stale, matching what the factory stamps for this case
            assertThat(value().stakingSource).isEqualTo(StatusSource.ACTUAL)
        }

        @Test
        fun `GIVEN a cached contribution WHEN stakingSource THEN cache`() {
            // Arrange
            val cached = StakingBalance.Data.P2PEthPool(
                stakingId = STAKING_ID,
                source = StatusSource.CACHE,
                accounts = emptyList(),
            )

            // Act & Assert — reading Sources.stakingBalanceSource here would report a false "fresh"
            assertThat(value(contributions = listOf(cached)).stakingSource).isEqualTo(StatusSource.CACHE)
        }

        @Test
        fun `GIVEN only a yield contribution WHEN stakingSource THEN actual`() {
            // Arrange — yield rides the network status, so it must not register as staking staleness
            val staleYield = YieldSupplyContribution(
                status = mockk<YieldSupplyStatus>(relaxed = true),
                source = StatusSource.ONLY_CACHE,
            )

            // Act & Assert
            assertThat(value(contributions = listOf(staleYield)).stakingSource).isEqualTo(StatusSource.ACTUAL)
        }

        @Test
        fun `GIVEN a non-Data contribution WHEN stakingSource THEN actual`() {
            // Arrange — Empty carries a source of its own, but the accessor narrows it away, as the factory does
            // Act & Assert
            assertThat(value(contributions = listOf(EMPTY)).stakingSource).isEqualTo(StatusSource.ACTUAL)
        }
    }

    private fun provideTestModels() = listOf(
        AccessorTestModel(
            description = "contribution present -> returned",
            value = value(contributions = listOf(STAKE_KIT)),
            expected = STAKE_KIT,
        ),
        AccessorTestModel(
            description = "no contributions -> null",
            value = value(),
            expected = null,
        ),
        AccessorTestModel(
            description = "only a non-staking contribution -> null",
            value = value(contributions = listOf(YIELD_SUPPLY)),
            expected = null,
        ),
        AccessorTestModel(
            description = "staking contribution alongside another kind -> the staking one is picked",
            value = value(contributions = listOf(YIELD_SUPPLY, P2P_ETH_POOL)),
            expected = P2P_ETH_POOL,
        ),
        AccessorTestModel(
            description = "contribution is a non-Data variant -> null, the accessor narrows",
            value = value(contributions = listOf(EMPTY)),
            expected = null,
        ),
        AccessorTestModel(
            description = "a non-Data staking entry first -> the Data one, the scan does not stop at the first entry",
            value = value(contributions = listOf(EMPTY, STAKE_KIT)),
            expected = STAKE_KIT,
        ),
    )

    internal data class AccessorTestModel(
        val description: String,
        val value: CryptoCurrencyStatus.Value,
        val expected: StakingBalance.Data?,
    ) {

        override fun toString(): String = description
    }

    // region Fixtures
    private fun value(contributions: List<BalanceContribution> = emptyList()): CryptoCurrencyStatus.Value {
        return CryptoCurrencyStatus.Loaded(
            amount = BigDecimal.TEN,
            fiatAmount = BigDecimal.TEN,
            fiatRate = BigDecimal.ONE,
            priceChange = BigDecimal.ZERO,
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

        /** A staking [BalanceContribution] with no breakdown — the variant the accessor must reject. */
        val EMPTY = StakingBalance.Empty(stakingId = STAKING_ID, source = StatusSource.ACTUAL)
    }
    // endregion
}