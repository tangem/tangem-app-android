package com.tangem.domain.search.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.*
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.portfolio.UserAssetEntry
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.search.model.RecentSearchToken
import com.tangem.domain.search.model.SearchResult
import com.tangem.domain.search.model.SearchTextHint
import com.tangem.domain.search.model.UserAssetSearchItem
import com.tangem.domain.search.repository.SearchRepository
import com.tangem.test.core.getEmittedValues
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetSearchResultsUseCaseTest {

    private val searchRepository = mockk<SearchRepository>()
    private val multiAccountStatusListSupplier = mockk<MultiAccountStatusListSupplier>()
    private val userWalletsListRepository = mockk<UserWalletsListRepository>()

    private val useCase = GetSearchResultsUseCase(
        searchRepository = searchRepository,
        multiAccountStatusListSupplier = multiAccountStatusListSupplier,
        userWalletsListRepository = userWalletsListRepository,
    )

    private val factory = MockCryptoCurrencyFactory()
    private val cardano = factory.cardano
    private val chia = factory.chia
    private val ethereum = factory.ethereum

    private val walletId1 = UserWalletId("011")
    private val walletId2 = UserWalletId("022")

    @BeforeEach
    fun setUp() {
        clearMocks(searchRepository, multiAccountStatusListSupplier, userWalletsListRepository)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class History {

        @Test
        fun `GIVEN blank query WHEN invoke THEN returns history with empty user assets`() = runTest {
            // Arrange
            val hints = listOf(SearchTextHint(text = "eth", timestamp = 1L))
            val tokens = listOf<RecentSearchToken>()
            every { searchRepository.getTextHints() } returns flowOf(hints)
            every { searchRepository.getRecentTokens() } returns flowOf(tokens)

            // Act
            val actual = getEmittedValues(useCase(query = "  "))

            // Assert
            val expected = SearchResult(textHints = hints, recentTokens = tokens, userAssets = emptyList())
            assertThat(actual).containsExactly(expected)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NoGrouping {

        @Test
        fun `GIVEN single wallet single account WHEN search THEN items are Single sorted by fiat desc`() = runTest {
            // Arrange
            val cardanoStatus = statusOf(cardano, fiatAmount = BigDecimal(10))
            val chiaStatus = statusOf(chia, fiatAmount = BigDecimal(20))
            val mainAccount = mainAccount(walletId1)
            val wallet = wallet(walletId1)

            stub(
                wallets = listOf(wallet),
                statusLists = listOf(
                    statusList(walletId1, cryptoPortfolio(mainAccount, listOf(cardanoStatus, chiaStatus))),
                ),
            )

            // Act
            val actual = search(query = "c")

            // Assert — single wallet + single account: nothing is grouped, sorted by fiat desc
            assertThat(actual).containsExactly(
                UserAssetSearchItem.Single(entry(wallet, mainAccount, chiaStatus)),
                UserAssetSearchItem.Single(entry(wallet, mainAccount, cardanoStatus)),
            ).inOrder()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Grouping {

        @Test
        fun `GIVEN several wallets but each asset appears once WHEN search THEN every asset stays Single`() = runTest {
            // Arrange — the regression: a unique asset must not become a Grouped item just because several wallets exist
            val cardanoStatus = statusOf(cardano, fiatAmount = BigDecimal(10))
            val chiaStatus = statusOf(chia, fiatAmount = BigDecimal(20))
            val mainAccount1 = mainAccount(walletId1)
            val mainAccount2 = mainAccount(walletId2)
            val firstWallet = wallet(walletId1)
            val secondWallet = wallet(walletId2)

            stub(
                wallets = listOf(firstWallet, secondWallet),
                statusLists = listOf(
                    statusList(walletId1, cryptoPortfolio(mainAccount1, listOf(cardanoStatus))),
                    statusList(walletId2, cryptoPortfolio(mainAccount2, listOf(chiaStatus))),
                ),
            )

            // Act
            val actual = search(query = "c")

            // Assert
            assertThat(actual).containsExactly(
                UserAssetSearchItem.Single(entry(secondWallet, mainAccount2, chiaStatus)),
                UserAssetSearchItem.Single(entry(firstWallet, mainAccount1, cardanoStatus)),
            ).inOrder()
        }

        @Test
        fun `GIVEN same currency in two wallets WHEN search THEN it is Grouped and unique asset stays Single`() =
            runTest {
                // Arrange
                val chiaStatusW1 = statusOf(chia, fiatAmount = BigDecimal(15))
                val chiaStatusW2 = statusOf(chia, fiatAmount = BigDecimal(5))
                val cardanoStatus = statusOf(cardano, fiatAmount = BigDecimal(30))
                val mainAccount1 = mainAccount(walletId1)
                val mainAccount2 = mainAccount(walletId2)
                val firstWallet = wallet(walletId1)
                val secondWallet = wallet(walletId2)

                stub(
                    wallets = listOf(firstWallet, secondWallet),
                    statusLists = listOf(
                        statusList(walletId1, cryptoPortfolio(mainAccount1, listOf(chiaStatusW1, cardanoStatus))),
                        statusList(walletId2, cryptoPortfolio(mainAccount2, listOf(chiaStatusW2))),
                    ),
                )

                // Act
                val actual = search(query = "c")

                // Assert — cardano (30) outranks the chia group (15 + 5 = 20)
                assertThat(actual).containsExactly(
                    UserAssetSearchItem.Single(entry(firstWallet, mainAccount1, cardanoStatus)),
                    UserAssetSearchItem.Grouped(
                        tokenName = chia.name,
                        tokenSymbol = chia.symbol,
                        tokenIconUrl = chia.iconUrl,
                        entries = listOf(
                            entry(firstWallet, mainAccount1, chiaStatusW1),
                            entry(secondWallet, mainAccount2, chiaStatusW2),
                        ),
                    ),
                ).inOrder()
            }

        @Test
        fun `GIVEN single wallet with several accounts WHEN same currency in two accounts THEN it is Grouped`() =
            runTest {
                // Arrange
                val chiaStatusMain = statusOf(chia, fiatAmount = BigDecimal(15))
                val chiaStatusSecondary = statusOf(chia, fiatAmount = BigDecimal(5))
                val cardanoStatus = statusOf(cardano, fiatAmount = BigDecimal(30))
                val mainAccount = mainAccount(walletId1)
                val secondaryAccount = secondaryAccount(walletId1, derivationIndex = 1)
                val wallet = wallet(walletId1)

                stub(
                    wallets = listOf(wallet),
                    statusLists = listOf(
                        statusList(
                            walletId = walletId1,
                            cryptoPortfolio(mainAccount, listOf(chiaStatusMain, cardanoStatus)),
                            cryptoPortfolio(secondaryAccount, listOf(chiaStatusSecondary)),
                        ),
                    ),
                )

                // Act — single wallet, but more than one account enables aggregation
                val actual = search(query = "c")

                // Assert
                assertThat(actual).containsExactly(
                    UserAssetSearchItem.Single(entry(wallet, mainAccount, cardanoStatus)),
                    UserAssetSearchItem.Grouped(
                        tokenName = chia.name,
                        tokenSymbol = chia.symbol,
                        tokenIconUrl = chia.iconUrl,
                        entries = listOf(
                            entry(wallet, mainAccount, chiaStatusMain),
                            entry(wallet, secondaryAccount, chiaStatusSecondary),
                        ),
                    ),
                ).inOrder()
            }

        @Test
        fun `GIVEN custom token without backend id in two wallets WHEN search THEN grouped by name and symbol`() =
            runTest {
                // Arrange — a custom token has no rawCurrencyId, so the merge falls back to name + symbol
                val customToken = customToken(name = "MyToken", symbol = "MTK")
                val statusW1 = statusOf(customToken, fiatAmount = BigDecimal(7))
                val statusW2 = statusOf(customToken, fiatAmount = BigDecimal(3))
                val mainAccount1 = mainAccount(walletId1)
                val mainAccount2 = mainAccount(walletId2)
                val firstWallet = wallet(walletId1)
                val secondWallet = wallet(walletId2)

                stub(
                    wallets = listOf(firstWallet, secondWallet),
                    statusLists = listOf(
                        statusList(walletId1, cryptoPortfolio(mainAccount1, listOf(statusW1))),
                        statusList(walletId2, cryptoPortfolio(mainAccount2, listOf(statusW2))),
                    ),
                )

                // Act
                val actual = search(query = "myto")

                // Assert
                assertThat(actual).containsExactly(
                    UserAssetSearchItem.Grouped(
                        tokenName = "MyToken",
                        tokenSymbol = "MTK",
                        tokenIconUrl = customToken.iconUrl,
                        entries = listOf(
                            entry(firstWallet, mainAccount1, statusW1),
                            entry(secondWallet, mainAccount2, statusW2),
                        ),
                    ),
                )
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class WalletFiltering {

        @Test
        fun `GIVEN locked wallet WHEN search THEN its assets are excluded`() = runTest {
            // Arrange
            val cardanoStatus = statusOf(cardano, fiatAmount = BigDecimal(10))
            val chiaStatus = statusOf(chia, fiatAmount = BigDecimal(20))
            val mainAccount1 = mainAccount(walletId1)
            val mainAccount2 = mainAccount(walletId2)
            val unlockedWallet = wallet(walletId1, locked = false)
            val lockedWallet = wallet(walletId2, locked = true)

            stub(
                wallets = listOf(unlockedWallet, lockedWallet),
                statusLists = listOf(
                    statusList(walletId1, cryptoPortfolio(mainAccount1, listOf(cardanoStatus))),
                    statusList(walletId2, cryptoPortfolio(mainAccount2, listOf(chiaStatus))),
                ),
            )

            // Act
            val actual = search(query = "c")

            // Assert — only the single unlocked wallet remains, so no grouping and chia is gone
            assertThat(actual).containsExactly(
                UserAssetSearchItem.Single(entry(unlockedWallet, mainAccount1, cardanoStatus)),
            )
        }

        @Test
        fun `GIVEN all wallets locked WHEN search THEN user assets are empty`() = runTest {
            // Arrange
            val cardanoStatus = statusOf(cardano, fiatAmount = BigDecimal(10))
            stub(
                wallets = listOf(wallet(walletId1, locked = true)),
                statusLists = listOf(
                    statusList(walletId1, cryptoPortfolio(mainAccount(walletId1), listOf(cardanoStatus))),
                ),
            )

            // Act
            val actual = search(query = "c")

            // Assert
            assertThat(actual).isEmpty()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Matching {

        @Test
        fun `GIVEN upper case query matching symbol WHEN search THEN match is case insensitive`() = runTest {
            // Arrange
            val ethereumStatus = statusOf(ethereum, fiatAmount = BigDecimal(10))
            val mainAccount = mainAccount(walletId1)
            val wallet = wallet(walletId1)
            stub(
                wallets = listOf(wallet),
                statusLists = listOf(statusList(walletId1, cryptoPortfolio(mainAccount, listOf(ethereumStatus)))),
            )

            // Act
            val actual = search(query = "ETH")

            // Assert
            assertThat(actual).containsExactly(
                UserAssetSearchItem.Single(entry(wallet, mainAccount, ethereumStatus)),
            )
        }

        @Test
        fun `GIVEN query matching nothing WHEN search THEN user assets are empty`() = runTest {
            // Arrange
            val cardanoStatus = statusOf(cardano, fiatAmount = BigDecimal(10))
            val mainAccount = mainAccount(walletId1)
            stub(
                wallets = listOf(wallet(walletId1)),
                statusLists = listOf(statusList(walletId1, cryptoPortfolio(mainAccount, listOf(cardanoStatus)))),
            )

            // Act
            val actual = search(query = "zzz")

            // Assert
            assertThat(actual).isEmpty()
        }
    }

    // region helpers

    private fun stub(wallets: List<UserWallet>, statusLists: List<AccountStatusList>) {
        every { multiAccountStatusListSupplier() } returns flowOf(statusLists)
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(wallets)
    }

    private fun TestScope.search(query: String): List<UserAssetSearchItem> =
        getEmittedValues(useCase(query)).last().userAssets

    private fun wallet(id: UserWalletId, locked: Boolean = false): UserWallet = mockk<UserWallet.Cold> {
        every { walletId } returns id
        every { name } returns "Wallet ${id.stringValue}"
        every { isLocked } returns locked
    }

    private fun mainAccount(walletId: UserWalletId): Account.CryptoPortfolio =
        Account.CryptoPortfolio.createMainAccount(userWalletId = walletId)

    private fun secondaryAccount(walletId: UserWalletId, derivationIndex: Int): Account.CryptoPortfolio {
        val index = DerivationIndex(derivationIndex).getOrNull()!!
        return Account.CryptoPortfolio(
            accountId = AccountId.forCryptoPortfolio(userWalletId = walletId, derivationIndex = index),
            accountName = AccountName("Account #$derivationIndex").getOrNull()!!,
            icon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
            derivationIndex = index,
            cryptoCurrencies = emptyList(),
        )
    }

    private fun statusOf(currency: CryptoCurrency, fiatAmount: BigDecimal?): CryptoCurrencyStatus {
        val statusValue = mockk<CryptoCurrencyStatus.Value> {
            every { this@mockk.fiatAmount } returns fiatAmount
        }
        return CryptoCurrencyStatus(currency = currency, value = statusValue)
    }

    private fun cryptoPortfolio(
        account: Account.CryptoPortfolio,
        statuses: List<CryptoCurrencyStatus>,
    ): AccountStatus.CryptoPortfolio = AccountStatus.CryptoPortfolio(
        account = account,
        tokenList = TokenList.Ungrouped(
            totalFiatBalance = TotalFiatBalance.Loading,
            sortedBy = TokensSortType.NONE,
            currencies = statuses,
        ),
        priceChangeLce = lceLoading(),
    )

    private fun statusList(walletId: UserWalletId, vararg accounts: AccountStatus.CryptoPortfolio): AccountStatusList =
        AccountStatusList(
            userWalletId = walletId,
            accountStatuses = accounts.toList(),
            totalAccounts = accounts.size,
            totalArchivedAccounts = 0,
            totalFiatBalance = TotalFiatBalance.Loading,
            sortType = TokensSortType.NONE,
            groupType = TokensGroupType.NONE,
        )

    private fun entry(
        wallet: UserWallet,
        account: Account.CryptoPortfolio,
        status: CryptoCurrencyStatus,
    ): UserAssetEntry = UserAssetEntry(
        userWalletId = wallet.walletId,
        userWalletName = wallet.name,
        accountId = account.accountId,
        accountName = account.accountName,
        accountIcon = account.icon,
        currencyStatus = status,
    )

    private fun customToken(name: String, symbol: String): CryptoCurrency.Token {
        val network = ethereum.network
        val contractAddress = "0xCUSTOM_$symbol"
        return CryptoCurrency.Token(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(rawId = "custom-network"),
                suffix = CryptoCurrency.ID.Suffix.ContractAddress(contractAddress = contractAddress),
            ),
            network = network,
            name = name,
            symbol = symbol,
            decimals = 8,
            iconUrl = null,
            isCustom = true,
            contractAddress = contractAddress,
        )
    }

    // endregion
}