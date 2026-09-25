package com.tangem.common

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
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
 * (`getTotalFiatAmount` / `getTotalCryptoAmount`), which fold the balance contributions of a status into the
 * displayed amount.
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
     * `getExtraBalanceOrNull` reads [CryptoCurrencyStatus.Value.contributions] and nothing else: a producer that
     * reports no contribution adds nothing to the displayed total, whatever other state the status carries.
     */
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetExtraBalanceOrNull {

        @Test
        fun `GIVEN a staking contribution WHEN getExtraBalanceOrNull THEN returns its delta`() {
            // Arrange
            val status = loaded(
                amount = BigDecimal.TEN,
                fiatAmount = BigDecimal.TEN,
                fiatRate = BigDecimal.ONE,
                staking = stakeKit(staked = 9),
            )

            // Act
            val actual = status.getExtraBalanceOrNull()

            // Assert
            assertThat(actual).isEqualTo(BigDecimal(9))
        }

        @Test
        fun `GIVEN no contributions WHEN getExtraBalanceOrNull THEN null`() {
            // Arrange
            val status = loaded(
                amount = BigDecimal.TEN,
                fiatAmount = BigDecimal.TEN,
                fiatRate = BigDecimal.ONE,
                staking = null,
            )

            // Act
            val actual = status.getExtraBalanceOrNull()

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN yield supply status but no contributions WHEN getExtraBalanceOrNull THEN null`() {
            // Arrange
            val status = loaded(
                amount = BigDecimal.TEN,
                fiatAmount = BigDecimal.TEN,
                fiatRate = BigDecimal.ONE,
                staking = null,
                yieldSupplyStatus = activeYieldSupply(),
            )

            // Act
            val actual = status.getExtraBalanceOrNull()

            // Assert
            assertThat(actual).isNull()
        }
    }

    // region Fixtures
    private fun loaded(
        amount: BigDecimal,
        fiatAmount: BigDecimal,
        fiatRate: BigDecimal,
        staking: StakingBalance?,
        yieldSupplyStatus: YieldSupplyStatus? = null,
    ): CryptoCurrencyStatus {
        return CryptoCurrencyStatus(
            currency = currency,
            value = CryptoCurrencyStatus.Loaded(
                amount = amount,
                fiatAmount = fiatAmount,
                fiatRate = fiatRate,
                priceChange = BigDecimal.ZERO,
                yieldSupplyStatus = yieldSupplyStatus,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = networkAddress(),
                sources = CryptoCurrencyStatus.Sources(),
                contributions = listOfNotNull(staking),
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
                yieldSupplyStatus = null,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = networkAddress(),
                sources = CryptoCurrencyStatus.Sources(),
                contributions = listOfNotNull(staking),
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