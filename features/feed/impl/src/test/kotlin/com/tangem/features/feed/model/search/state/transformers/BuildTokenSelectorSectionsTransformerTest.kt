package com.tangem.features.feed.model.search.state.transformers

import com.google.common.truth.Truth
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.portfolio.UserAssetEntry
import com.tangem.common.ui.markets.tokenselector.TokenSelectorContentUM
import com.tangem.common.ui.markets.tokenselector.TokenSelectorSectionUM
import com.tangem.core.ui.ds.image.DeviceIconUM
import io.mockk.*
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BuildTokenSelectorSectionsTransformerTest {

    private val appCurrency: AppCurrency = AppCurrency.Default
    private val onTokenSelected: (UserAssetEntry) -> Unit = mockk(relaxed = true)
    private val prevState = TokenSelectorContentUM(sections = persistentListOf())

    @BeforeEach
    fun setup() {
        clearMocks(onTokenSelected)
    }

    @Test
    fun `should return empty sections when entries list is empty`() {
        val transformer = createTransformer(entries = emptyList())

        val result = transformer.transform(prevState)

        Truth.assertThat(result.sections).isEmpty()
    }

    @Test
    fun `should create single TokenGroup without wallet header for single wallet`() {
        val walletId = createMockUserWalletId("wallet1")
        val accountId = createMockAccountId("account1")
        val entries = listOf(
            createMockEntry(walletId, "Wallet 1", accountId, "btc", "Bitcoin", "BTC"),
            createMockEntry(walletId, "Wallet 1", accountId, "eth", "Ethereum", "ETH"),
        )

        val transformer = createTransformer(entries)
        val result = transformer.transform(prevState)

        Truth.assertThat(result.sections).hasSize(1)
        Truth.assertThat(result.sections[0]).isInstanceOf(TokenSelectorSectionUM.TokenGroup::class.java)

        val group = result.sections[0] as TokenSelectorSectionUM.TokenGroup
        Truth.assertThat(group.items).hasSize(2)
        Truth.assertThat(group.accountHeader).isNull()
    }

    @Test
    fun `should add wallet headers when multiple wallets present`() {
        val walletId1 = createMockUserWalletId("wallet1")
        val walletId2 = createMockUserWalletId("wallet2")
        val accountId = createMockAccountId("account1")
        val entries = listOf(
            createMockEntry(walletId1, "Wallet 1", accountId, "btc", "Bitcoin", "BTC"),
            createMockEntry(walletId2, "Wallet 2", accountId, "eth", "Ethereum", "ETH"),
        )

        val transformer = createTransformer(entries)
        val result = transformer.transform(prevState)

        val walletHeaders = result.sections.filterIsInstance<TokenSelectorSectionUM.WalletHeader>()
        val tokenGroups = result.sections.filterIsInstance<TokenSelectorSectionUM.TokenGroup>()

        Truth.assertThat(walletHeaders).hasSize(2)
        Truth.assertThat(walletHeaders[0].walletName).isEqualTo("Wallet 1")
        Truth.assertThat(walletHeaders[1].walletName).isEqualTo("Wallet 2")
        Truth.assertThat(tokenGroups).hasSize(2)
    }

    @Test
    fun `should not show account headers when single account per wallet`() {
        val walletId = createMockUserWalletId("wallet1")
        val accountId = createMockAccountId("account1")
        val entries = listOf(
            createMockEntry(walletId, "Wallet 1", accountId, "btc", "Bitcoin", "BTC"),
            createMockEntry(walletId, "Wallet 1", accountId, "eth", "Ethereum", "ETH"),
        )

        val transformer = createTransformer(entries)
        val result = transformer.transform(prevState)

        val groups = result.sections.filterIsInstance<TokenSelectorSectionUM.TokenGroup>()
        Truth.assertThat(groups).hasSize(1)
        Truth.assertThat(groups[0].accountHeader).isNull()
    }

    @Test
    fun `should show account headers when multiple accounts in same wallet`() {
        val walletId = createMockUserWalletId("wallet1")
        val accountId1 = createMockAccountId("account1")
        val accountId2 = createMockAccountId("account2")
        val entries = listOf(
            createMockEntry(walletId, "Wallet 1", accountId1, "btc", "Bitcoin", "BTC"),
            createMockEntry(walletId, "Wallet 1", accountId2, "eth", "Ethereum", "ETH"),
        )

        val transformer = createTransformer(entries)
        val result = transformer.transform(prevState)

        val groups = result.sections.filterIsInstance<TokenSelectorSectionUM.TokenGroup>()
        Truth.assertThat(groups).hasSize(2)
        Truth.assertThat(groups[0].accountHeader).isNotNull()
        Truth.assertThat(groups[1].accountHeader).isNotNull()
    }

    @Test
    fun `should group entries by wallet and then by account`() {
        val walletId1 = createMockUserWalletId("wallet1")
        val walletId2 = createMockUserWalletId("wallet2")
        val account1 = createMockAccountId("acc1")
        val account2 = createMockAccountId("acc2")

        val entries = listOf(
            createMockEntry(walletId1, "W1", account1, "btc", "Bitcoin", "BTC"),
            createMockEntry(walletId1, "W1", account2, "eth", "Ethereum", "ETH"),
            createMockEntry(walletId2, "W2", account1, "sol", "Solana", "SOL"),
        )

        val transformer = createTransformer(entries)
        val result = transformer.transform(prevState)

        // 2 wallet headers + 2 token groups for wallet1 (2 accounts) + 1 token group for wallet2
        val walletHeaders = result.sections.filterIsInstance<TokenSelectorSectionUM.WalletHeader>()
        val tokenGroups = result.sections.filterIsInstance<TokenSelectorSectionUM.TokenGroup>()

        Truth.assertThat(walletHeaders).hasSize(2)
        Truth.assertThat(tokenGroups).hasSize(3)

        // Wallet1 has 2 accounts so headers should be present
        Truth.assertThat(tokenGroups[0].accountHeader).isNotNull()
        Truth.assertThat(tokenGroups[1].accountHeader).isNotNull()
        // Wallet2 has 1 account so no account header
        Truth.assertThat(tokenGroups[2].accountHeader).isNull()
    }

    @Test
    fun `should correctly place multiple tokens in same account group`() {
        val walletId = createMockUserWalletId("wallet1")
        val accountId = createMockAccountId("account1")
        val entries = listOf(
            createMockEntry(walletId, "W1", accountId, "btc", "Bitcoin", "BTC"),
            createMockEntry(walletId, "W1", accountId, "eth", "Ethereum", "ETH"),
            createMockEntry(walletId, "W1", accountId, "sol", "Solana", "SOL"),
        )

        val transformer = createTransformer(entries)
        val result = transformer.transform(prevState)

        val groups = result.sections.filterIsInstance<TokenSelectorSectionUM.TokenGroup>()
        Truth.assertThat(groups).hasSize(1)
        Truth.assertThat(groups[0].items).hasSize(3)
    }

    @Test
    fun `should preserve order of wallets and accounts`() {
        val walletId1 = createMockUserWalletId("wallet1")
        val walletId2 = createMockUserWalletId("wallet2")
        val account1 = createMockAccountId("acc1")
        val account2 = createMockAccountId("acc2")

        val entries = listOf(
            createMockEntry(walletId1, "First Wallet", account1, "btc", "Bitcoin", "BTC"),
            createMockEntry(walletId1, "First Wallet", account2, "eth", "Ethereum", "ETH"),
            createMockEntry(walletId2, "Second Wallet", account1, "sol", "Solana", "SOL"),
        )

        val transformer = createTransformer(entries)
        val result = transformer.transform(prevState)

        val headers = result.sections.filterIsInstance<TokenSelectorSectionUM.WalletHeader>()
        Truth.assertThat(headers[0].walletName).isEqualTo("First Wallet")
        Truth.assertThat(headers[1].walletName).isEqualTo("Second Wallet")
    }

    @Test
    fun `should convert entries to UserAssetItemUM Single`() {
        val walletId = createMockUserWalletId("wallet1")
        val accountId = createMockAccountId("account1")
        val entries = listOf(
            createMockEntry(walletId, "W1", accountId, "btc", "Bitcoin", "BTC"),
        )

        val transformer = createTransformer(entries)
        val result = transformer.transform(prevState)

        val groups = result.sections.filterIsInstance<TokenSelectorSectionUM.TokenGroup>()
        Truth.assertThat(groups).hasSize(1)

        val item = groups[0].items[0]
        Truth.assertThat(item.tokenName).isEqualTo("Bitcoin")
        Truth.assertThat(item.tokenSymbol).isEqualTo("BTC")
    }

    @Test
    fun `should ignore previous state and build from scratch`() {
        val walletId = createMockUserWalletId("wallet1")
        val accountId = createMockAccountId("account1")
        val entries = listOf(
            createMockEntry(walletId, "W1", accountId, "btc", "Bitcoin", "BTC"),
        )

        val prevStateWithSections = TokenSelectorContentUM(
            sections = persistentListOf(
                TokenSelectorSectionUM.WalletHeader(
                    walletName = "Old Wallet",
                    deviceIcon = DeviceIconUM.Stub(cardsCount = 1),
                ),
            ),
        )

        val transformer = createTransformer(entries)
        val result = transformer.transform(prevStateWithSections)

        val headers = result.sections.filterIsInstance<TokenSelectorSectionUM.WalletHeader>()
        Truth.assertThat(headers).isEmpty()

        val groups = result.sections.filterIsInstance<TokenSelectorSectionUM.TokenGroup>()
        Truth.assertThat(groups).hasSize(1)
    }

    // region Helpers

    private fun createTransformer(entries: List<UserAssetEntry>): BuildTokenSelectorSectionsTransformer {
        return BuildTokenSelectorSectionsTransformer(
            entries = entries,
            appCurrency = appCurrency,
            isBalanceHidden = false,
            walletIcons = emptyMap(),
            onTokenSelected = onTokenSelected,
        )
    }

    private fun createMockUserWalletId(id: String): UserWalletId {
        return mockk<UserWalletId> {
            every { stringValue } returns id
        }
    }

    private fun createMockAccountId(id: String): AccountId {
        return mockk<AccountId> {
            every { value } returns id
        }
    }

    private fun createMockEntry(
        walletId: UserWalletId = createMockUserWalletId("wallet1"),
        walletName: String = "Wallet 1",
        accountId: AccountId = createMockAccountId("account1"),
        currencyId: String = "btc",
        currencyName: String = "Bitcoin",
        currencySymbol: String = "BTC",
    ): UserAssetEntry {
        val network = mockk<Network>(relaxed = true) {
            every { name } returns "Network"
        }
        val currencyIdObj = mockk<CryptoCurrency.ID> {
            every { value } returns currencyId
        }
        val currency = mockk<CryptoCurrency.Coin> {
            every { id } returns currencyIdObj
            every { name } returns currencyName
            every { symbol } returns currencySymbol
            every { this@mockk.network } returns network
            every { decimals } returns 8
            every { iconUrl } returns null
            every { isCustom } returns false
        }
        val currencyStatus = mockk<CryptoCurrencyStatus> {
            every { this@mockk.currency } returns currency
            every { value } returns CryptoCurrencyStatus.Loaded(
                amount = BigDecimal("1.0"),
                fiatAmount = BigDecimal("30000.0"),
                fiatRate = BigDecimal("30000.0"),
                priceChange = BigDecimal("1.0"),
                stakingBalance = null,
                yieldSupplyStatus = null,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = mockk(relaxed = true),
                sources = CryptoCurrencyStatus.Sources(),
            )
        }
        return mockk<UserAssetEntry> {
            every { userWalletId } returns walletId
            every { userWalletName } returns walletName
            every { this@mockk.accountId } returns accountId
            every { accountName } returns AccountName.DefaultMain
            every { accountIcon } returns mockk(relaxed = true)
            every { this@mockk.currencyStatus } returns currencyStatus
        }
    }

    // endregion
}