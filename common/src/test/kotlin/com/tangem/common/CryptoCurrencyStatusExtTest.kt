package com.tangem.common

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.balance.BalanceContribution
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.staking.BalanceItem
import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.YieldBalanceItem
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
 * Characterization tests for [CryptoCurrencyStatusExt] — the per-currency total helpers
 * (`getTotalFiatAmount` / `getTotalCryptoAmount`), which currently fold the staking balance into the
 * displayed amount and are directly rewritten by the `BalanceContribution` migration.
 *
 * They also lock the "yield-supply adds zero" invariant at the per-currency level: these helpers never
 * read `yieldSupplyStatus`, so a yield token's total is its bare amount. The staking branch is driven
 * through the real `getTotalWithRewardsStakingBalance`; the currency is mocked because the only thing the
 * helpers read from it is `network.rawId` (Ethereum here => full staking balance is added, non-Cardano).
 */
internal class CryptoCurrencyStatusExtTest {

    private val currency = mockk<CryptoCurrency.Coin>(relaxed = true) {
        every { network.rawId } returns Blockchain.Ethereum.toNetworkId()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetTotalFiatAmount {

        @Test
        fun `GIVEN staking with fiatAmount and fiatRate WHEN getTotalFiatAmount THEN adds fiat value of staked`() {
            // Arrange — fiat 10 + fiatRate 1 × staked 9
            val status = loaded(amount = BigDecimal.TEN, fiatAmount = BigDecimal.TEN, fiatRate = BigDecimal.ONE, staking = stakeKit(staked = 9))

            // Act
            val actual = status.getTotalFiatAmount()

            // Assert
            assertThat(actual).isEqualTo(BigDecimal(19))
        }

        @Test
        fun `GIVEN fiatRate is null WHEN getTotalFiatAmount THEN returns bare fiatAmount`() {
            // Arrange
            val status = custom(amount = BigDecimal.TEN, fiatAmount = BigDecimal.TEN, fiatRate = null, staking = stakeKit(staked = 9))

            // Act
            val actual = status.getTotalFiatAmount()

            // Assert
            assertThat(actual).isEqualTo(BigDecimal.TEN)
        }

        @Test
        fun `GIVEN fiatAmount is null but staking present WHEN getTotalFiatAmount THEN returns staked fiat only`() {
            // Arrange — fiatRate 1 × staked 9, no base fiat amount
            val status = custom(amount = BigDecimal.TEN, fiatAmount = null, fiatRate = BigDecimal.ONE, staking = stakeKit(staked = 9))

            // Act
            val actual = status.getTotalFiatAmount()

            // Assert
            assertThat(actual).isEqualTo(BigDecimal(9))
        }

        @Test
        fun `GIVEN no staking WHEN getTotalFiatAmount THEN returns bare fiatAmount`() {
            // Arrange
            val status = loaded(amount = BigDecimal.TEN, fiatAmount = BigDecimal.TEN, fiatRate = BigDecimal.ONE, staking = null)

            // Act
            val actual = status.getTotalFiatAmount()

            // Assert
            assertThat(actual).isEqualTo(BigDecimal.TEN)
        }

        @Test
        fun `GIVEN yield supply but no staking WHEN getTotalFiatAmount THEN yield adds zero`() {
            // Arrange — yield supply is set; the helper must ignore it entirely
            val status = loaded(
                amount = BigDecimal.TEN,
                fiatAmount = BigDecimal.TEN,
                fiatRate = BigDecimal.ONE,
                staking = null,
                yieldSupplyStatus = activeYieldSupply(),
            )

            // Act
            val actual = status.getTotalFiatAmount()

            // Assert
            assertThat(actual).isEqualTo(BigDecimal.TEN)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetTotalCryptoAmount {

        @Test
        fun `GIVEN staking present WHEN getTotalCryptoAmount THEN adds staked to amount`() {
            // Arrange — amount 10 + staked 9
            val status = loaded(amount = BigDecimal.TEN, fiatAmount = BigDecimal.TEN, fiatRate = BigDecimal.ONE, staking = stakeKit(staked = 9))

            // Act
            val actual = status.getTotalCryptoAmount()

            // Assert
            assertThat(actual).isEqualTo(BigDecimal(19))
        }

        @Test
        fun `GIVEN no staking WHEN getTotalCryptoAmount THEN returns bare amount`() {
            // Arrange
            val status = loaded(amount = BigDecimal.TEN, fiatAmount = BigDecimal.TEN, fiatRate = BigDecimal.ONE, staking = null)

            // Act
            val actual = status.getTotalCryptoAmount()

            // Assert
            assertThat(actual).isEqualTo(BigDecimal.TEN)
        }

        @Test
        fun `GIVEN yield supply but no staking WHEN getTotalCryptoAmount THEN yield adds zero`() {
            // Arrange
            val status = loaded(
                amount = BigDecimal.TEN,
                fiatAmount = BigDecimal.TEN,
                fiatRate = BigDecimal.ONE,
                staking = null,
                yieldSupplyStatus = activeYieldSupply(),
            )

            // Act
            val actual = status.getTotalCryptoAmount()

            // Assert
            assertThat(actual).isEqualTo(BigDecimal.TEN)
        }
    }

    /**
     * The migration gate at the read site: `getExtraBalanceOrNull` prefers
     * [CryptoCurrencyStatus.Value.contributions] (filled only while `TWI_1717_BALANCE_CONTRIBUTIONS` is on) and
     * otherwise falls back to the legacy typed field. Both modes must produce the same numbers — that is what
     * makes the toggle safe to flip in either direction.
     */
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToggleParity {

        @Test
        fun `GIVEN staking WHEN toggle on and off THEN every total is identical`() {
            // Arrange — the same staking balance seen through both architectures
            val legacy = loadedWithStaking(useContributions = false)
            val contributions = loadedWithStaking(useContributions = true)

            // Act & Assert
            assertThat(contributions.getExtraBalanceOrNull()).isEqualTo(legacy.getExtraBalanceOrNull())
            assertThat(contributions.getTotalCryptoAmount()).isEqualTo(legacy.getTotalCryptoAmount())
            assertThat(contributions.getTotalFiatAmount()).isEqualTo(legacy.getTotalFiatAmount())
        }

        @Test
        fun `GIVEN toggle on WHEN getTotalCryptoAmount THEN staked amount is added`() {
            // Arrange
            val status = loadedWithStaking(useContributions = true)

            // Act
            val actual = status.getTotalCryptoAmount()

            // Assert — Ethereum, so the staked principal sits outside the network balance: 10 + 9
            assertThat(actual).isEqualTo(BigDecimal(19))
        }

        @Test
        fun `GIVEN no staking WHEN getExtraBalanceOrNull THEN null in both modes`() {
            // Arrange
            val legacy = loaded(
                amount = BigDecimal.TEN,
                fiatAmount = BigDecimal.TEN,
                fiatRate = BigDecimal.ONE,
                staking = null,
            )
            val contributions = loaded(
                amount = BigDecimal.TEN,
                fiatAmount = BigDecimal.TEN,
                fiatRate = BigDecimal.ONE,
                staking = null,
                useContributions = true,
            )

            // Act & Assert
            assertThat(legacy.getExtraBalanceOrNull()).isNull()
            assertThat(contributions.getExtraBalanceOrNull()).isNull()
        }

        @Test
        fun `GIVEN yield supply only WHEN getExtraBalanceOrNull THEN null`() {
            // Arrange — yield supply is not a contribution yet; it is projected by its own provider later
            val status = loaded(
                amount = BigDecimal.TEN,
                fiatAmount = BigDecimal.TEN,
                fiatRate = BigDecimal.ONE,
                staking = null,
                yieldSupplyStatus = activeYieldSupply(),
                useContributions = true,
            )

            // Act & Assert
            assertThat(status.getExtraBalanceOrNull()).isNull()
        }

        private fun loadedWithStaking(useContributions: Boolean): CryptoCurrencyStatus {
            return loaded(
                amount = BigDecimal.TEN,
                fiatAmount = BigDecimal.TEN,
                fiatRate = BigDecimal.ONE,
                staking = stakeKit(staked = 9),
                useContributions = useContributions,
            )
        }
    }

    // region Fixtures
    private fun loaded(
        amount: BigDecimal,
        fiatAmount: BigDecimal,
        fiatRate: BigDecimal,
        staking: StakingBalance?,
        yieldSupplyStatus: YieldSupplyStatus? = null,
        useContributions: Boolean = false,
    ): CryptoCurrencyStatus {
        return CryptoCurrencyStatus(
            currency = currency,
            value = CryptoCurrencyStatus.Loaded(
                amount = amount,
                fiatAmount = fiatAmount,
                fiatRate = fiatRate,
                priceChange = BigDecimal.ZERO,
                stakingBalance = staking,
                yieldSupplyStatus = yieldSupplyStatus,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = networkAddress(),
                sources = CryptoCurrencyStatus.Sources(),
                // mirrors CryptoCurrencyStatusFactory: filled only while the toggle is on
                contributions = if (useContributions) listOfNotNull(staking as? BalanceContribution) else emptyList(),
            ),
        )
    }

    private fun custom(
        amount: BigDecimal,
        fiatAmount: BigDecimal?,
        fiatRate: BigDecimal?,
        staking: StakingBalance?,
    ): CryptoCurrencyStatus {
        return CryptoCurrencyStatus(
            currency = currency,
            value = CryptoCurrencyStatus.Custom(
                amount = amount,
                fiatAmount = fiatAmount,
                fiatRate = fiatRate,
                priceChange = null,
                stakingBalance = staking,
                yieldSupplyStatus = null,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = networkAddress(),
                sources = CryptoCurrencyStatus.Sources(),
            ),
        )
    }

    private fun stakeKit(staked: Int): StakingBalance.Data.StakeKit {
        return StakingBalance.Data.StakeKit(
            stakingId = mockk(relaxed = true),
            source = StatusSource.ACTUAL,
            balance = YieldBalanceItem(
                items = listOf(
                    mockk<BalanceItem>(relaxed = true) {
                        every { amount } returns BigDecimal(staked)
                        every { type } returns BalanceType.STAKED
                    },
                ),
                integrationId = "",
            ),
        )
    }

    private fun activeYieldSupply(): YieldSupplyStatus {
        return YieldSupplyStatus(
            isActive = true,
            isInitialized = true,
            isAllowedToSpend = true,
            effectiveProtocolBalance = BigDecimal(5),
        )
    }

    private fun networkAddress(): NetworkAddress.Single {
        return NetworkAddress.Single(
            defaultAddress = NetworkAddress.Address(
                value = "0x1",
                type = NetworkAddress.Address.Type.Primary,
            ),
        )
    }
    // endregion
}