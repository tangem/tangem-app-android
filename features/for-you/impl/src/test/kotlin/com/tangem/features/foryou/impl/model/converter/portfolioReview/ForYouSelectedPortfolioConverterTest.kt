package com.tangem.features.foryou.impl.model.converter.portfolioReview

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.foryou.impl.createLoadedValue
import com.tangem.features.foryou.impl.createStakedBalance
import com.tangem.features.foryou.impl.model.ForYouSelectedPortfolio
import com.tangem.test.mock.MockAccounts
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class ForYouSelectedPortfolioConverterTest {

    @Nested
    inner class Total {

        @Test
        fun `GIVEN staked balance WHEN convert THEN total fiat includes the staked fiat`() {
            // Arrange — 100 held on-chain plus a rate of 50 over 2 staked units
            val account = createAccount(
                statuses = listOf(
                    createStatus(
                        createCoin("eth"),
                        createLoadedValue(
                            amount = BigDecimal.ONE,
                            fiatAmount = BigDecimal("100"),
                            fiatRate = BigDecimal("50"),
                            staking = createStakedBalance(BigDecimal("2")),
                        ),
                    ),
                ),
            )

            // Act
            val result = convert(accounts = listOf(account))

            // Assert
            assertThat(result.totalFiatBalance).isEqualTo(
                TotalFiatBalance.Loaded(amount = BigDecimal("200"), source = StatusSource.ACTUAL),
            )
        }

        @Test
        fun `GIVEN staking across several accounts WHEN convert THEN every staked fiat is summed`() {
            // Arrange — two accounts, each holding 100 on-chain and 50 x 1 staked
            val first = createAccount(statuses = listOf(stakedStatus()), derivationIndex = 1)
            val second = createAccount(statuses = listOf(stakedStatus()), derivationIndex = 2)

            // Act
            val result = convert(accounts = listOf(first, second))

            // Assert
            assertThat(result.totalFiatBalance).isEqualTo(
                TotalFiatBalance.Loaded(amount = BigDecimal("300"), source = StatusSource.ACTUAL),
            )
        }

        @Test
        fun `GIVEN staking without a fiat rate WHEN convert THEN only the bare fiat amount is counted`() {
            // Arrange — a staked balance the app cannot price contributes nothing to the fiat total
            val account = createAccount(
                statuses = listOf(
                    createStatus(
                        createCoin("eth"),
                        CryptoCurrencyStatus.Custom(
                            amount = BigDecimal.ONE,
                            fiatAmount = BigDecimal("100"),
                            fiatRate = null,
                            priceChange = null,
                            stakingBalance = createStakedBalance(BigDecimal("2")),
                            yieldSupplyStatus = null,
                            hasCurrentNetworkTransactions = false,
                            pendingTransactions = emptySet(),
                            networkAddress = mockk(),
                            sources = CryptoCurrencyStatus.Sources(),
                        ),
                    ),
                ),
            )

            // Act
            val result = convert(accounts = listOf(account))

            // Assert
            assertThat(result.totalFiatBalance).isEqualTo(
                TotalFiatBalance.Loaded(amount = BigDecimal("100"), source = StatusSource.ACTUAL),
            )
        }

        @Test
        fun `GIVEN all statuses loading WHEN convert THEN total is Loading`() {
            // Arrange
            val account = createAccount(
                statuses = listOf(createStatus(createCoin("eth"), CryptoCurrencyStatus.Loading)),
            )

            // Act
            val result = convert(accounts = listOf(account))

            // Assert
            assertThat(result.totalFiatBalance).isEqualTo(TotalFiatBalance.Loading)
        }

        @Test
        fun `GIVEN no selected accounts WHEN convert THEN total is Failed`() {
            // Arrange
            val account = createAccount(statuses = listOf(stakedStatus()))

            // Act
            val result = convert(accounts = listOf(account), selected = emptyList())

            // Assert
            assertThat(result.totalFiatBalance).isEqualTo(TotalFiatBalance.Failed)
        }

        @Test
        fun `GIVEN one only-cache status WHEN convert THEN the worst source across the selection is reported`() {
            // Arrange — a single stale currency downgrades the whole total's source
            val account = createAccount(
                statuses = listOf(
                    createStatus(createCoin("eth"), createLoadedValue(fiatAmount = BigDecimal("100"))),
                    createStatus(
                        createCoin("btc"),
                        createLoadedValue(fiatAmount = BigDecimal("100"), source = StatusSource.ONLY_CACHE),
                    ),
                ),
            )

            // Act
            val result = convert(accounts = listOf(account))

            // Assert
            assertThat(result.totalFiatBalance).isEqualTo(
                TotalFiatBalance.Loaded(amount = BigDecimal("200"), source = StatusSource.ONLY_CACHE),
            )
        }
    }

    @Nested
    inner class Selection {

        @Test
        fun `GIVEN unselected account WHEN convert THEN its currencies are excluded but it is still counted`() {
            // Arrange — the selector badge needs the total account count, not the selected one
            val selected = createAccount(
                statuses = listOf(createStatus(createCoin("eth"), createLoadedValue(fiatAmount = BigDecimal("100")))),
                derivationIndex = 1,
            )
            val unselected = createAccount(
                statuses = listOf(createStatus(createCoin("btc"), createLoadedValue(fiatAmount = BigDecimal("900")))),
                derivationIndex = 2,
            )

            // Act
            val result = convert(accounts = listOf(selected, unselected), selected = listOf(selected))

            // Assert
            assertThat(result.totalAccountsCount).isEqualTo(2)
            assertThat(result.accountCryptoCurrencyStatuses.map { it.status.currency.id.value })
                .containsExactly("coin-eth")
            assertThat(result.totalFiatBalance).isEqualTo(
                TotalFiatBalance.Loaded(amount = BigDecimal("100"), source = StatusSource.ACTUAL),
            )
        }

        @Test
        fun `GIVEN accounts of several wallets WHEN convert THEN the selected ones are flattened together`() {
            // Arrange
            val firstWallet = UserWalletId("01")
            val secondWallet = UserWalletId("02")
            val onFirst = createAccount(
                statuses = listOf(createStatus(createCoin("eth"), createLoadedValue(fiatAmount = BigDecimal("100")))),
                walletId = firstWallet,
            )
            val onSecond = createAccount(
                statuses = listOf(createStatus(createCoin("btc"), createLoadedValue(fiatAmount = BigDecimal("200")))),
                walletId = secondWallet,
            )

            // Act
            val result = convert(walletAccounts = mapOf(firstWallet to listOf(onFirst), secondWallet to listOf(onSecond)))

            // Assert
            assertThat(result.accountCryptoCurrencyStatuses.map { it.status.currency.id.value })
                .containsExactly("coin-eth", "coin-btc")
            assertThat(result.totalFiatBalance).isEqualTo(
                TotalFiatBalance.Loaded(amount = BigDecimal("300"), source = StatusSource.ACTUAL),
            )
        }
    }

    // region Fixtures
    /** An account paired with the statuses of the currencies it holds. */
    private class PortfolioAccount(
        val account: Account.CryptoPortfolio,
        val statuses: List<CryptoCurrencyStatus>,
    )

    private fun convert(
        accounts: List<PortfolioAccount>,
        selected: List<PortfolioAccount> = accounts,
    ): ForYouSelectedPortfolio = convert(
        walletAccounts = mapOf(WALLET_ID to accounts),
        selected = selected,
    )

    private fun convert(
        walletAccounts: Map<UserWalletId, List<PortfolioAccount>>,
        selected: List<PortfolioAccount> = walletAccounts.values.flatten(),
    ): ForYouSelectedPortfolio = ForYouSelectedPortfolioConverter(
        selectedAccounts = selected.mapTo(mutableSetOf()) { it.account.accountId },
    ).convert(walletAccounts.mapValues { (_, accounts) -> createAccountStatusList(accounts) })

    private fun createAccountStatusList(accounts: List<PortfolioAccount>): AccountStatusList = mockk {
        every { accountStatuses } returns accounts.map { portfolioAccount ->
            mockk<AccountStatus.CryptoPortfolio> {
                every { account } returns portfolioAccount.account
                every { accountId } returns portfolioAccount.account.accountId
                every { flattenCurrencies() } returns portfolioAccount.statuses
            }
        }
    }

    /**
     * [AccountCryptoCurrencyStatus] requires the account to own the currency, so the account is always built
     * from the statuses it is given.
     */
    private fun createAccount(
        statuses: List<CryptoCurrencyStatus>,
        derivationIndex: Int = 1,
        walletId: UserWalletId = WALLET_ID,
    ): PortfolioAccount = PortfolioAccount(
        account = MockAccounts.createAccount(
            derivationIndex = derivationIndex,
            userWalletId = walletId,
            cryptoCurrencies = statuses.map { it.currency },
        ),
        statuses = statuses,
    )

    /** 100 held on-chain plus a rate of 50 over 1 staked unit, so its total fiat is 150. */
    private fun stakedStatus(): CryptoCurrencyStatus = createStatus(
        createCoin("eth"),
        createLoadedValue(
            amount = BigDecimal.ONE,
            fiatAmount = BigDecimal("100"),
            fiatRate = BigDecimal("50"),
            staking = createStakedBalance(BigDecimal.ONE),
        ),
    )

    private fun createStatus(currency: CryptoCurrency, value: CryptoCurrencyStatus.Value) = CryptoCurrencyStatus(
        currency = currency,
        value = value,
    )

    private fun createCoin(rawCurrencyId: String): CryptoCurrency.Coin {
        val network: Network = mockk {
            every { rawId } returns rawCurrencyId
        }
        val currencyId: CryptoCurrency.ID = mockk {
            every { value } returns "coin-$rawCurrencyId"
        }
        return mockk {
            every { id } returns currencyId
            every { this@mockk.network } returns network
        }
    }
    // endregion

    private companion object {
        /** UserWalletId parses its value as hex, so the id must be a valid hex string. */
        val WALLET_ID = UserWalletId("01")
    }
}