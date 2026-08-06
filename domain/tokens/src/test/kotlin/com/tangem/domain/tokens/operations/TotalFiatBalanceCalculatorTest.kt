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
import com.tangem.domain.models.staking.P2PEthPoolExitQueue
import com.tangem.domain.models.staking.P2PEthPoolExitRequest
import com.tangem.domain.models.staking.P2PEthPoolStake
import com.tangem.domain.models.staking.P2PEthPoolStakingAccount
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.staking.YieldBalanceItem
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.lib.crypto.BlockchainUtils
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Instant
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
            // Arrange — this used to be a copy of the NoQuote case above and never built a MissedDerivation,
            // leaving that arm of `resolve` uncovered
            val statuses = nonEmptyListOf(
                createMissedDerivation(currency = cryptoCurrencyFactory.stellar),
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

        @Test
        fun `yield supply token contributes zero to total`() {
            // Arrange - yield supply is set but the calculator must ignore it (supplied principal is already
            // inside value.amount / fiatAmount, so it contributes nothing extra to the total).
            val statuses = nonEmptyListOf(
                createLoaded(
                    currency = cryptoCurrencyFactory.ethereum,
                    fiatAmount = BigDecimal.TEN,
                    yieldSupplyStatus = YieldSupplyStatus(
                        isActive = true,
                        isInitialized = true,
                        isAllowedToSpend = true,
                        effectiveProtocolBalance = BigDecimal(5),
                    ),
                ),
            )

            // Act
            val actual = TotalFiatBalanceCalculator.calculate(statuses)

            // Assert
            val expected = TotalFiatBalance.Loaded(amount = BigDecimal.TEN, source = StatusSource.ACTUAL)
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `Solana staking balance is fully added to total`() {
            // Arrange - Solana is not Cardano, so isIncludeStakingTotalBalance is true and the full staking
            // balance counts towards the total.
            val statuses = nonEmptyListOf(
                createLoaded(
                    currency = cryptoCurrencyFactory.createCoin(Blockchain.Solana),
                    fiatAmount = BigDecimal.ZERO,
                    stakingBalance = createStakeKitBalance(
                        amount = BigDecimal(7),
                        balanceType = BalanceType.STAKED,
                    ),
                ),
            )

            // Act
            val actual = TotalFiatBalanceCalculator.calculate(statuses)

            // Assert
            val expected = TotalFiatBalance.Loaded(amount = BigDecimal(7), source = StatusSource.ACTUAL)
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `P2PEthPool staking balance flows into total`() {
            // Arrange - confirms the calculator handles the P2PEthPool provider type, not only StakeKit.
            val statuses = nonEmptyListOf(
                createLoaded(
                    currency = cryptoCurrencyFactory.ethereum,
                    fiatAmount = BigDecimal.ZERO,
                    stakingBalance = createP2PEthPoolBalance(staked = 4),
                ),
            )

            // Act
            val actual = TotalFiatBalanceCalculator.calculate(statuses)

            // Assert
            val expected = TotalFiatBalance.Loaded(amount = BigDecimal(4), source = StatusSource.ACTUAL)
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `Solana staking as a contribution is fully added to total`() {
            // Arrange — toggle-on shape: the typed field is null and the balance arrives as a contribution
            val statuses = nonEmptyListOf(
                createLoaded(
                    currency = cryptoCurrencyFactory.createCoin(Blockchain.Solana),
                    fiatAmount = BigDecimal.ZERO,
                    stakingBalance = createStakeKitBalance(amount = BigDecimal(7), balanceType = BalanceType.STAKED),
                    useContributions = true,
                ),
            )

            // Act
            val actual = TotalFiatBalanceCalculator.calculate(statuses)

            // Assert
            val expected = TotalFiatBalance.Loaded(amount = BigDecimal(7), source = StatusSource.ACTUAL)
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `Cardano staking as a contribution adds rewards only`() {
            // Arrange — the per-network rule now travels on the contribution as a stamped flag rather than being
            // resolved at call time, so this is the case that catches a contribution reaching the list unstamped
            val statuses = nonEmptyListOf(
                createLoaded(
                    currency = cryptoCurrencyFactory.cardano,
                    fiatAmount = BigDecimal.ZERO,
                    stakingBalance = createStakeKitBalance(amount = BigDecimal(3), balanceType = BalanceType.REWARDS),
                    useContributions = true,
                ),
            )

            // Act
            val actual = TotalFiatBalanceCalculator.calculate(statuses)

            // Assert — an unstamped contribution would sum every item instead and over-count the principal
            val expected = TotalFiatBalance.Loaded(amount = BigDecimal(3), source = StatusSource.ACTUAL)
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `contribution is converted at the fiat rate, not added raw`() {
            // Arrange — every other fixture uses a rate of 1, which cannot tell `rate * extra` from `extra`
            val statuses = nonEmptyListOf(
                createLoaded(
                    currency = cryptoCurrencyFactory.createCoin(Blockchain.Solana),
                    fiatAmount = BigDecimal.ZERO,
                    stakingBalance = createStakeKitBalance(amount = BigDecimal(3), balanceType = BalanceType.STAKED),
                    fiatRate = BigDecimal(2),
                    useContributions = true,
                ),
            )

            // Act
            val actual = TotalFiatBalanceCalculator.calculate(statuses)

            // Assert
            val expected = TotalFiatBalance.Loaded(amount = BigDecimal(6), source = StatusSource.ACTUAL)
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `custom token contribution is added to total`() {
            // Arrange — Custom is built by a different `plusLoaded` overload, whose contributions path is
            // otherwise never exercised
            val statuses = nonEmptyListOf(
                createCustom(
                    currency = cryptoCurrencyFactory.createCoin(Blockchain.Solana),
                    fiatAmount = BigDecimal.ZERO,
                    stakingBalance = createStakeKitBalance(amount = BigDecimal(5), balanceType = BalanceType.STAKED),
                    useContributions = true,
                ),
            )

            // Act
            val actual = TotalFiatBalanceCalculator.calculate(statuses)

            // Assert
            val expected = TotalFiatBalance.Loaded(amount = BigDecimal(5), source = StatusSource.ACTUAL)
            Truth.assertThat(actual).isEqualTo(expected)
        }
    }

    /**
     * Dual-run gate for `TWI_1717_BALANCE_CONTRIBUTIONS`: the same portfolio must total the same whether extra
     * balances arrive through `Value.contributions` (toggle on) or through the legacy typed field (toggle off).
     *
     * Each case is asserted against the *other* mode rather than a literal, which alone cannot catch "both wrong
     * the same way" — the literal-asserted contribution cases in [CalculateCurrenciesComputable] are the anchor
     * that makes these meaningful. The fixtures null the typed field on the toggle-on side, exactly as
     * `CryptoCurrencyStatusFactory` does; keeping it populated would let a dead contributions path pass here.
     */
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToggleParity {

        @Test
        fun `GIVEN Cardano staking rewards WHEN toggle on and off THEN totals match`() {
            // Arrange — Cardano stakes are already inside the network balance, so only rewards may be added
            assertParity(
                currency = cryptoCurrencyFactory.cardano,
                stakingBalance = createStakeKitBalance(amount = BigDecimal(20), balanceType = BalanceType.REWARDS),
            )
        }

        @Test
        fun `GIVEN Solana staking WHEN toggle on and off THEN totals match`() {
            // Arrange — everything counts for Solana
            assertParity(
                currency = cryptoCurrencyFactory.createCoin(Blockchain.Solana),
                stakingBalance = createStakeKitBalance(amount = BigDecimal(7), balanceType = BalanceType.STAKED),
            )
        }

        @Test
        fun `GIVEN P2PEthPool staking WHEN toggle on and off THEN totals match`() {
            // Arrange
            assertParity(
                currency = cryptoCurrencyFactory.ethereum,
                stakingBalance = createP2PEthPoolBalance(staked = 5, unstaking = 2, withdrawable = 1, rewards = 3),
            )
        }

        @Test
        fun `GIVEN no staking WHEN toggle on and off THEN totals match`() {
            // Arrange — nothing outside the network balance in either mode
            assertParity(currency = cryptoCurrencyFactory.ethereum, stakingBalance = null)
        }

        @Test
        fun `GIVEN a custom token WHEN toggle on and off THEN totals match`() {
            // Arrange — the Custom overload has its own fiat-rate handling, so it needs its own parity case
            val currency = cryptoCurrencyFactory.createCoin(Blockchain.Solana)
            val staking = createStakeKitBalance(amount = BigDecimal(6), balanceType = BalanceType.STAKED)

            // Act
            val legacy = TotalFiatBalanceCalculator.calculate(
                nonEmptyListOf(
                    createCustom(
                        currency = currency,
                        fiatAmount = BigDecimal.TEN,
                        stakingBalance = staking,
                        useContributions = false,
                    ),
                ),
            )
            val contributions = TotalFiatBalanceCalculator.calculate(
                nonEmptyListOf(
                    createCustom(
                        currency = currency,
                        fiatAmount = BigDecimal.TEN,
                        stakingBalance = staking,
                        useContributions = true,
                    ),
                ),
            )

            // Assert
            Truth.assertThat(contributions).isEqualTo(legacy)
        }

        private fun assertParity(currency: CryptoCurrency, stakingBalance: StakingBalance.Data?) {
            // Act
            val legacy = TotalFiatBalanceCalculator.calculate(
                nonEmptyListOf(
                    createLoaded(
                        currency = currency,
                        fiatAmount = BigDecimal.TEN,
                        stakingBalance = stakingBalance,
                        useContributions = false,
                    ),
                ),
            )
            val contributions = TotalFiatBalanceCalculator.calculate(
                nonEmptyListOf(
                    createLoaded(
                        currency = currency,
                        fiatAmount = BigDecimal.TEN,
                        stakingBalance = stakingBalance,
                        useContributions = true,
                    ),
                ),
            )

            // Assert
            Truth.assertThat(contributions).isEqualTo(legacy)
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
        stakingBalance: StakingBalance.Data? = null,
        yieldSupplyStatus: YieldSupplyStatus? = null,
        fiatRate: BigDecimal = BigDecimal.ONE,
        useContributions: Boolean = false,
    ): CryptoCurrencyStatus {
        val balance = stakingBalance?.takeIf { !useContributions } ?: stakingBalance?.stampFor(currency)

        return CryptoCurrencyStatus(
            currency = currency,
            value = CryptoCurrencyStatus.Custom(
                amount = BigDecimal.ONE,
                fiatAmount = fiatAmount,
                fiatRate = fiatRate,
                priceChange = BigDecimal.ZERO,
                // the factory nulls the typed field whenever contributions are on, so a status carrying BOTH
                // cannot occur in production — and would let every parity assertion pass off the legacy fallback
                stakingBalance = balance.takeIf { !useContributions },
                yieldSupplyStatus = yieldSupplyStatus,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = createNetworkAddress(),
                sources = CryptoCurrencyStatus.Sources(),
                contributions = if (useContributions) listOfNotNull(balance) else emptyList(),
            ),
        )
    }

    private fun createLoaded(
        currency: CryptoCurrency,
        fiatAmount: BigDecimal,
        stakingBalance: StakingBalance.Data? = null,
        yieldSupplyStatus: YieldSupplyStatus? = null,
        source: StatusSource = StatusSource.ACTUAL,
        fiatRate: BigDecimal = BigDecimal.ONE,
        useContributions: Boolean = false,
    ): CryptoCurrencyStatus {
        val balance = stakingBalance?.takeIf { !useContributions } ?: stakingBalance?.stampFor(currency)

        return CryptoCurrencyStatus(
            currency = currency,
            value = CryptoCurrencyStatus.Loaded(
                amount = BigDecimal.ONE,
                fiatAmount = fiatAmount,
                fiatRate = fiatRate,
                priceChange = BigDecimal.ZERO,
                // see createCustom: a hybrid (typed field + contributions) status is not producible by the factory
                stakingBalance = balance.takeIf { !useContributions },
                yieldSupplyStatus = yieldSupplyStatus,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = createNetworkAddress(),
                sources = CryptoCurrencyStatus.Sources(source, source, source),
                contributions = if (useContributions) listOfNotNull(balance) else emptyList(),
            ),
        )
    }

    /**
     * Mirrors what `CryptoCurrencyStatusFactory` stamps when it attaches a balance — a contribution can only
     * answer with the right number once the per-network rule has been resolved from the owning currency.
     */
    private fun StakingBalance.Data.stampFor(currency: CryptoCurrency): StakingBalance.Data {
        val isStakedIncludedInNetworkBalance = !BlockchainUtils.isIncludeStakingTotalBalance(currency.network.rawId)

        return when (this) {
            is StakingBalance.Data.StakeKit -> copy(
                isStakedIncludedInNetworkBalance = isStakedIncludedInNetworkBalance,
            )
            is StakingBalance.Data.P2PEthPool -> copy(
                isStakedIncludedInNetworkBalance = isStakedIncludedInNetworkBalance,
            )
        }
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

    /**
     * A real instance, not a mock: `totalDeltaCryptoAmount()` is a member function, so a mocked balance would
     * intercept the very math under test on the contributions path. Unstaking / withdrawable are derived from the
     * exit queue by claimability, so they go in as requests.
     */
    private fun createP2PEthPoolBalance(
        staked: Int,
        unstaking: Int = 0,
        withdrawable: Int = 0,
        rewards: Int = 0,
    ): StakingBalance.Data.P2PEthPool {
        return StakingBalance.Data.P2PEthPool(
            stakingId = StakingID(integrationId = "integration", address = "0x1"),
            source = StatusSource.ACTUAL,
            accounts = listOf(
                P2PEthPoolStakingAccount(
                    delegatorAddress = "0xdelegator",
                    vaultAddress = "0xvault",
                    stake = P2PEthPoolStake(
                        assets = BigDecimal(staked),
                        totalEarnedAssets = BigDecimal(rewards),
                    ),
                    availableToUnstake = BigDecimal.ZERO,
                    availableToWithdraw = BigDecimal.ZERO,
                    exitQueue = P2PEthPoolExitQueue(
                        total = BigDecimal(unstaking + withdrawable),
                        requests = listOf(
                            createExitRequest(totalAssets = unstaking, isClaimable = false),
                            createExitRequest(totalAssets = withdrawable, isClaimable = true),
                        ),
                    ),
                ),
            ),
        )
    }

    private fun createExitRequest(totalAssets: Int, isClaimable: Boolean): P2PEthPoolExitRequest {
        return P2PEthPoolExitRequest(
            ticket = "ticket-$totalAssets-$isClaimable",
            totalAssets = BigDecimal(totalAssets),
            timestamp = Instant.fromEpochSeconds(epochSeconds = 0),
            withdrawalTimestamp = null,
            isClaimable = isClaimable,
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