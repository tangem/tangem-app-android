package com.tangem.domain.tokens.operations

import arrow.core.nonEmptyListOf
import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.staking.BalanceItem
import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.YieldBalanceItem
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TotalFiatBalanceCalculatorTest {

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val binance = cryptoCurrencyFactory.createCoin(Blockchain.Binance)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CalculateCurrenciesLoadingOrNonComputable {

        @Test
        fun `one token is Loading, total is Loading`() {
            // Arrange
            val statuses = nonEmptyListOf(
                createLoading(currency = cryptoCurrencyFactory.ethereum),
                createNoQuote(currency = cryptoCurrencyFactory.stellar),
                createMissedDerivation(currency = cryptoCurrencyFactory.chia),
            )

            // Act
            val actual = TotalFiatBalanceCalculator.calculate(statuses)

            // Assert
            val expected = TotalFiatBalance.Loading
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `one token is NoQuote, total is Failed`() {
            // Arrange
            val statuses = nonEmptyListOf(
                createNoQuote(currency = cryptoCurrencyFactory.stellar),
                createUnreachable(currency = cryptoCurrencyFactory.chia),
            )

            // Act
            val actual = TotalFiatBalanceCalculator.calculate(statuses)

            // Assert
            val expected = TotalFiatBalance.Failed
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `one token is MissedDerivation, total is Failed`() {
            // Arrange
            val statuses = nonEmptyListOf(
                createNoQuote(currency = cryptoCurrencyFactory.stellar),
                createUnreachable(currency = cryptoCurrencyFactory.chia),
            )

            // Act
            val actual = TotalFiatBalanceCalculator.calculate(statuses)

            // Assert
            val expected = TotalFiatBalance.Failed
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `one token is Unreachable and isIncludeToBalanceOnError is FALSE, total is Failed`() {
            // Arrange
            val statuses = nonEmptyListOf(
                createUnreachable(currency = cryptoCurrencyFactory.stellar),
                createNoAccount(currency = cryptoCurrencyFactory.chia),
            )

            // Act
            val actual = TotalFiatBalanceCalculator.calculate(statuses)

            // Assert
            val expected = TotalFiatBalance.Failed
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `one token is NoAmount and isIncludeToBalanceOnError is FALSE, total is Failed`() {
            // Arrange
            val statuses = nonEmptyListOf(
                createNoAmount(currency = cryptoCurrencyFactory.stellar),
                createNoAccount(currency = cryptoCurrencyFactory.chia),
            )

            // Act
            val actual = TotalFiatBalanceCalculator.calculate(statuses)

            // Assert
            val expected = TotalFiatBalance.Failed
            Truth.assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CalculateCurrenciesComputable {

        @Test
        fun `Unreachable token isIncludeToBalanceOnError, total is Loaded`() {
            // Arrange
            val statuses = nonEmptyListOf(createUnreachable(currency = binance))

            // Act
            val actual = TotalFiatBalanceCalculator.calculate(statuses)

            // Assert
            val expected = TotalFiatBalance.Loaded(
                amount = BigDecimal.ZERO,
                source = StatusSource.ACTUAL,
            )
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `NoAmount token isIncludeToBalanceOnError, total is Loaded`() {
            // Arrange
            val statuses = nonEmptyListOf(createNoAmount(currency = binance))

            // Act
            val actual = TotalFiatBalanceCalculator.calculate(statuses)

            // Assert
            val expected = TotalFiatBalance.Loaded(
                amount = BigDecimal.ZERO,
                source = StatusSource.ACTUAL,
            )
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `all tokens are NoAccount, total is Loaded`() {
            // Arrange
            val statuses = nonEmptyListOf(
                createNoAccount(currency = cryptoCurrencyFactory.ethereum),
                createNoAccount(currency = cryptoCurrencyFactory.stellar),
                createNoAccount(currency = cryptoCurrencyFactory.chia),
            )

            // Act
            val actual = TotalFiatBalanceCalculator.calculate(statuses)

            // Assert
            val expected = TotalFiatBalance.Loaded(
                amount = BigDecimal.ZERO,
                source = StatusSource.ACTUAL,
            )
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `all tokens are Custom, total is Loaded`() {
            // Arrange
            val statuses = nonEmptyListOf(
                /**
                 * Balance: 10
                 * - fiat: 10
                 * - staking: 0
                 */
                createCustom(
                    currency = cryptoCurrencyFactory.ethereum,
                    fiatAmount = BigDecimal.TEN,
                ),
                /**
                 * Balance: 20
                 * - fiat: 0
                 * - staking: 20 (REWARDS)
                 */
                createCustom(
                    currency = cryptoCurrencyFactory.cardano,
                    fiatAmount = BigDecimal.ZERO,
                    stakingBalance = createStakeKitBalance(
                        amount = BigDecimal(20),
                        // It is important to use `BalanceType.REWARDS` because Cardano should not include the full
                        // staking balance. See `getTotalWithRewardsStakingBalance`.
                        balanceType = BalanceType.REWARDS,
                    ),
                ),
                /**
                 * Balance: 10
                 * - fiat: 1
                 * - staking: 9 (STAKED)
                 */
                createCustom(
                    currency = cryptoCurrencyFactory.stellar,
                    fiatAmount = BigDecimal.ONE,
                    stakingBalance = createStakeKitBalance(
                        amount = BigDecimal(9),
                        balanceType = BalanceType.STAKED,
                    ),
                ),
            )

            // Act
            val actual = TotalFiatBalanceCalculator.calculate(statuses)

            // Assert
            val expected = TotalFiatBalance.Loaded(
                amount = BigDecimal(40),
                source = StatusSource.ACTUAL,
            )
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `all tokens are Loaded, total is Loaded`() {
            // Arrange
            val statuses = nonEmptyListOf(
                /**
                 * Balance: 10
                 * - fiat: 10
                 * - staking: 0
                 */
                createLoaded(
                    currency = cryptoCurrencyFactory.ethereum,
                    fiatAmount = BigDecimal.TEN,
                ),
                /**
                 * Balance: 20
                 * - fiat: 0
                 * - staking: 20 (REWARDS)
                 */
                createLoaded(
                    currency = cryptoCurrencyFactory.cardano,
                    fiatAmount = BigDecimal.ZERO,
                    stakingBalance = createStakeKitBalance(
                        amount = BigDecimal(20),
                        // It is important to use `BalanceType.REWARDS` because Cardano should not include the full
                        // staking balance. See `getTotalWithRewardsStakingBalance`.
                        balanceType = BalanceType.REWARDS,
                    ),
                ),
                /**
                 * Balance: 10
                 * - fiat: 1
                 * - staking: 9 (STAKED)
                 */
                createLoaded(
                    currency = cryptoCurrencyFactory.stellar,
                    fiatAmount = BigDecimal.ONE,
                    stakingBalance = createStakeKitBalance(
                        amount = BigDecimal(9),
                        balanceType = BalanceType.STAKED,
                    ),
                ),
            )

            // Act
            val actual = TotalFiatBalanceCalculator.calculate(statuses)

            // Assert
            val expected = TotalFiatBalance.Loaded(
                amount = BigDecimal(40),
                source = StatusSource.ACTUAL,
            )
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `tokens contain all types of computable statuses, total is Loaded`() {
            // Arrange
            val statuses = nonEmptyListOf(
                createNoAccount(currency = cryptoCurrencyFactory.ethereum), // 0
                createNoAmount(currency = binance), // 0
                createUnreachable(currency = binance), // 0
                createCustom(
                    // 1
                    currency = cryptoCurrencyFactory.cardano,
                    fiatAmount = BigDecimal.ONE,
                ),
                createLoaded(
                    // 10
                    currency = cryptoCurrencyFactory.stellar,
                    fiatAmount = BigDecimal.TEN,
                ),
            )

            // Act
            val actual = TotalFiatBalanceCalculator.calculate(statuses)

            // Assert
            val expected = TotalFiatBalance.Loaded(
                amount = BigDecimal(11),
                source = StatusSource.ACTUAL,
            )
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `StatusSource is Actual is all tokens are Actual`() {
            // Arrange
            val statuses = nonEmptyListOf(
                createNoAccount(currency = cryptoCurrencyFactory.ethereum),
                createNoAmount(currency = binance),
                createUnreachable(currency = binance),
                createCustom(
                    currency = cryptoCurrencyFactory.cardano,
                    fiatAmount = BigDecimal.ONE,
                ),
                createLoaded(
                    currency = cryptoCurrencyFactory.stellar,
                    fiatAmount = BigDecimal.TEN,
                ),
            )

            // Act
            val actual = (TotalFiatBalanceCalculator.calculate(statuses) as TotalFiatBalance.Loaded).source

            // Assert
            val expected = StatusSource.ACTUAL
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `StatusSource is Cache is any token is Cache`() {
            // Arrange
            val statuses = nonEmptyListOf(
                createNoAccount(currency = cryptoCurrencyFactory.ethereum),
                createNoAmount(currency = binance),
                createLoaded(
                    currency = cryptoCurrencyFactory.cardano,
                    fiatAmount = BigDecimal.ONE,
                    source = StatusSource.CACHE,
                ),
            )

            // Act
            val actual = (TotalFiatBalanceCalculator.calculate(statuses) as TotalFiatBalance.Loaded).source

            // Assert
            val expected = StatusSource.CACHE
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `StatusSource is ONLY_CACHE is any token is ONLY_CACHE`() {
            // Arrange
            val statuses = nonEmptyListOf(
                createNoAccount(currency = cryptoCurrencyFactory.ethereum),
                createNoAmount(currency = binance),
                createLoaded(
                    currency = cryptoCurrencyFactory.cardano,
                    fiatAmount = BigDecimal.ONE,
                    source = StatusSource.ONLY_CACHE,
                ),
            )

            // Act
            val actual = (TotalFiatBalanceCalculator.calculate(statuses) as TotalFiatBalance.Loaded).source

            // Assert
            val expected = StatusSource.ONLY_CACHE
            Truth.assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CalculateBalancesLoadingOrNonComputable {

        @Test
        fun `one balance is Loading, total is Loading`() {
            // Arrange
            val balances = nonEmptyListOf(
                TotalFiatBalance.Loading,
                TotalFiatBalance.Failed,
            )

            // Act
            val actual = TotalFiatBalanceCalculator.calculate(balances)

            // Assert
            val expected = TotalFiatBalance.Loading
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `one balance is Failed, total is Failed`() {
            // Arrange
            val balances = nonEmptyListOf(
                TotalFiatBalance.Failed,
                TotalFiatBalance.Loaded(amount = BigDecimal.ONE, source = StatusSource.ACTUAL),
            )

            // Act
            val actual = TotalFiatBalanceCalculator.calculate(balances)

            // Assert
            val expected = TotalFiatBalance.Failed
            Truth.assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CalculateBalancesComputable {

        @Test
        fun `all balances are Loaded, total is Loaded`() {
            // Arrange
            val balances = nonEmptyListOf(
                TotalFiatBalance.Loaded(amount = BigDecimal.TEN, source = StatusSource.ACTUAL),
                TotalFiatBalance.Loaded(amount = BigDecimal.ONE, source = StatusSource.ACTUAL),
            )

            // Act
            val actual = TotalFiatBalanceCalculator.calculate(balances)

            // Assert
            val expected = TotalFiatBalance.Loaded(
                amount = BigDecimal(11),
                source = StatusSource.ACTUAL,
            )
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `StatusSource is Actual is all balances are Actual`() {
            // Arrange
            val balances = nonEmptyListOf(
                TotalFiatBalance.Loaded(amount = BigDecimal.TEN, source = StatusSource.ACTUAL),
                TotalFiatBalance.Loaded(amount = BigDecimal.ONE, source = StatusSource.ACTUAL),
            )

            // Act
            val actual = (TotalFiatBalanceCalculator.calculate(balances) as TotalFiatBalance.Loaded).source

            // Assert
            val expected = StatusSource.ACTUAL
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `StatusSource is Cache is any balance is Cache`() {
            // Arrange
            val balances = nonEmptyListOf(
                TotalFiatBalance.Loaded(amount = BigDecimal.TEN, source = StatusSource.ACTUAL),
                TotalFiatBalance.Loaded(amount = BigDecimal.ONE, source = StatusSource.CACHE),
            )

            // Act
            val actual = (TotalFiatBalanceCalculator.calculate(balances) as TotalFiatBalance.Loaded).source

            // Assert
            val expected = StatusSource.CACHE
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `StatusSource is ONLY_CACHE is any balance is ONLY_CACHE`() {
            // Arrange
            val balances = nonEmptyListOf(
                TotalFiatBalance.Loaded(amount = BigDecimal.TEN, source = StatusSource.ACTUAL),
                TotalFiatBalance.Loaded(amount = BigDecimal.ONE, source = StatusSource.ONLY_CACHE),
            )

            // Act
            val actual = (TotalFiatBalanceCalculator.calculate(balances) as TotalFiatBalance.Loaded).source
            // Assert
            val expected = StatusSource.ONLY_CACHE
            Truth.assertThat(actual).isEqualTo(expected)
        }
    }

    private fun createLoading(currency: CryptoCurrency): CryptoCurrencyStatus {
        return CryptoCurrencyStatus(
            currency = currency,
            value = CryptoCurrencyStatus.Loading,
        )
    }

    private fun createNoQuote(currency: CryptoCurrency): CryptoCurrencyStatus {
        return CryptoCurrencyStatus(
            currency = currency,
            value = CryptoCurrencyStatus.NoQuote(
                amount = BigDecimal.ONE,
                stakingBalance = null,
                yieldSupplyStatus = null,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = createNetworkAddress(),
                sources = CryptoCurrencyStatus.Sources(),
            ),
        )
    }

    private fun createMissedDerivation(currency: CryptoCurrency): CryptoCurrencyStatus {
        return CryptoCurrencyStatus(
            currency = currency,
            value = CryptoCurrencyStatus.MissedDerivation(priceChange = null, fiatRate = null),
        )
    }

    private fun createUnreachable(currency: CryptoCurrency): CryptoCurrencyStatus {
        return CryptoCurrencyStatus(
            currency = currency,
            value = CryptoCurrencyStatus.Unreachable(priceChange = null, fiatRate = null, networkAddress = null),
        )
    }

    private fun createNoAmount(currency: CryptoCurrency): CryptoCurrencyStatus {
        return CryptoCurrencyStatus(
            currency = currency,
            value = CryptoCurrencyStatus.NoAmount(priceChange = null, fiatRate = null),
        )
    }

    private fun createNoAccount(currency: CryptoCurrency): CryptoCurrencyStatus {
        return CryptoCurrencyStatus(
            currency = currency,
            value = CryptoCurrencyStatus.NoAccount(
                priceChange = null,
                amountToCreateAccount = BigDecimal.ONE,
                fiatAmount = null,
                fiatRate = null,
                networkAddress = createNetworkAddress(),
                sources = CryptoCurrencyStatus.Sources(),
            ),
        )
    }

    private fun createCustom(
        currency: CryptoCurrency,
        fiatAmount: BigDecimal?,
        stakingBalance: StakingBalance.Data.StakeKit? = null,
    ): CryptoCurrencyStatus {
        return CryptoCurrencyStatus(
            currency = currency,
            value = CryptoCurrencyStatus.Custom(
                amount = BigDecimal.ONE,
                fiatAmount = fiatAmount,
                fiatRate = BigDecimal.ONE,
                priceChange = BigDecimal.ZERO,
                stakingBalance = stakingBalance,
                yieldSupplyStatus = null,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = createNetworkAddress(),
                sources = CryptoCurrencyStatus.Sources(),
            ),
        )
    }

    private fun createLoaded(
        currency: CryptoCurrency,
        fiatAmount: BigDecimal,
        stakingBalance: StakingBalance.Data.StakeKit? = null,
        source: StatusSource = StatusSource.ACTUAL,
    ): CryptoCurrencyStatus {
        return CryptoCurrencyStatus(
            currency = currency,
            value = CryptoCurrencyStatus.Loaded(
                amount = BigDecimal.ONE,
                fiatAmount = fiatAmount,
                fiatRate = BigDecimal.ONE,
                priceChange = BigDecimal.ZERO,
                stakingBalance = stakingBalance,
                yieldSupplyStatus = null,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = createNetworkAddress(),
                sources = CryptoCurrencyStatus.Sources(source, source, source),
            ),
        )
    }

    private fun createStakeKitBalance(amount: BigDecimal, balanceType: BalanceType): StakingBalance.Data.StakeKit {
        return StakingBalance.Data.StakeKit(
            stakingId = mockk(relaxed = true),
            source = StatusSource.ACTUAL,
            balance = YieldBalanceItem(
                items = listOf(
                    mockk<BalanceItem>(relaxed = true) {
                        every { this@mockk.amount } returns amount
                        every { this@mockk.type } returns balanceType
                    },
                ),
                integrationId = "",
            ),
        )
    }

    private fun createNetworkAddress(): NetworkAddress.Single {
        return NetworkAddress.Single(
            defaultAddress = NetworkAddress.Address(
                value = "0x1",
                type = NetworkAddress.Address.Type.Primary,
            ),
        )
    }
}