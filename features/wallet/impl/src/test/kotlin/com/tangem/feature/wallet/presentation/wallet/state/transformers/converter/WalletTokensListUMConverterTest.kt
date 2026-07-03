package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.card.CardTypesResolver
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account.CryptoPortfolio.Companion.createMainAccount
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListUM
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WalletTokensListUMConverterTest {

    private val userWalletId = UserWalletId("00")

    @AfterEach
    fun tearDown() {
        unmockkStatic(ScanResponse::cardTypesResolver)
    }

    @ParameterizedTest
    @MethodSource("provideOrganizeButtonModels")
    fun `GIVEN wallet type WHEN convert THEN Add and Manage button visibility matches`(model: OrganizeButtonModel) {
        // Arrange
        val converter = createConverter(
            selectedWallet = mockColdWallet(
                isSingleCurrency = model.isSingleCurrency,
                isSingleWalletWithToken = model.isSingleWalletWithToken,
            ),
        )

        // Act
        val result = converter.convert(value = nonEmptyAccountList()) as WalletTokensListUM.Content

        // Assert
        assertThat(result.organizeButtonUM != null).isEqualTo(model.expectedButtonShown)
    }

    private fun createConverter(selectedWallet: UserWallet): WalletTokensListUMConverter = WalletTokensListUMConverter(
        appCurrency = AppCurrency.Default,
        selectedWallet = selectedWallet,
        clickIntents = mockk(relaxed = true),
        yieldModuleApyMap = emptyMap(),
        isAccountsModeEnabled = false,
        expandedAccounts = emptySet(),
        stakingAvailabilityMap = emptyMap(),
        isAddAndManageTokensEnabled = true,
        shouldShowMainPromo = false,
    )

    private fun mockColdWallet(isSingleCurrency: Boolean, isSingleWalletWithToken: Boolean): UserWallet.Cold {
        mockkStatic(ScanResponse::cardTypesResolver)
        val resolver = mockk<CardTypesResolver>(relaxed = true) {
            every { isSingleCurrency() } returns isSingleCurrency
            every { isSingleWalletWithToken() } returns isSingleWalletWithToken
        }
        val scanResponse = mockk<ScanResponse>()
        every { scanResponse.cardTypesResolver } returns resolver
        return mockk<UserWallet.Cold>(relaxed = true) {
            every { this@mockk.scanResponse } returns scanResponse
        }
    }

    private fun nonEmptyAccountList(): AccountStatusList {
        val tokenList = TokenList.Ungrouped(
            currencies = listOf(createLoadedStatus(createToken())),
            totalFiatBalance = TotalFiatBalance.Loaded(BigDecimal.ZERO, StatusSource.ACTUAL),
            sortedBy = TokensSortType.NONE,
        )
        return AccountStatusList(
            userWalletId = userWalletId,
            accountStatuses = listOf(
                AccountStatus.CryptoPortfolio(
                    account = createMainAccount(userWalletId),
                    tokenList = tokenList,
                    priceChangeLce = Unit.lceError(),
                ),
            ),
            totalAccounts = 1,
            totalArchivedAccounts = 0,
            totalFiatBalance = TotalFiatBalance.Loaded(BigDecimal.ZERO, StatusSource.ACTUAL),
            sortType = TokensSortType.NONE,
            groupType = TokensGroupType.NONE,
        )
    }

    private fun createToken(): CryptoCurrency.Token {
        val network = Network(
            id = Network.ID(value = "ethereum", derivationPath = Network.DerivationPath.None),
            name = "ethereum",
            currencySymbol = "ETH",
            derivationPath = Network.DerivationPath.None,
            isTestnet = false,
            standardType = Network.StandardType.ERC20,
            hasFiatFeeRate = false,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )
        return CryptoCurrency.Token(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(rawId = "ethereum"),
                suffix = CryptoCurrency.ID.Suffix.ContractAddress(contractAddress = "0xABCDEF"),
            ),
            network = network,
            name = "Token",
            symbol = "TKN",
            decimals = 18,
            iconUrl = null,
            isCustom = false,
            contractAddress = "0xABCDEF",
        )
    }

    private fun createLoadedStatus(token: CryptoCurrency.Token): CryptoCurrencyStatus {
        val value = CryptoCurrencyStatus.Loaded(
            amount = BigDecimal.ONE,
            fiatAmount = BigDecimal.ZERO,
            fiatRate = BigDecimal.ONE,
            priceChange = BigDecimal.ZERO,
            stakingBalance = null,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(
                    value = "addr",
                    type = NetworkAddress.Address.Type.Primary,
                ),
            ),
            sources = CryptoCurrencyStatus.Sources(),
        )
        return CryptoCurrencyStatus(currency = token, value = value)
    }

    internal data class OrganizeButtonModel(
        val isSingleCurrency: Boolean,
        val isSingleWalletWithToken: Boolean,
        val expectedButtonShown: Boolean,
    )

    private fun provideOrganizeButtonModels() = listOf(
        // Multi-currency wallet (Wallet / Wallet2) — Add & Manage is shown.
        OrganizeButtonModel(isSingleCurrency = false, isSingleWalletWithToken = false, expectedButtonShown = true),
        // Single-currency wallet without token (e.g. S2C) — Add & Manage must be hidden ([REDACTED_TASK_KEY]).
        OrganizeButtonModel(isSingleCurrency = true, isSingleWalletWithToken = false, expectedButtonShown = false),
        // Single-currency wallet with token (e.g. NODL) — Add & Manage stays hidden.
        OrganizeButtonModel(isSingleCurrency = true, isSingleWalletWithToken = true, expectedButtonShown = false),
    )
}