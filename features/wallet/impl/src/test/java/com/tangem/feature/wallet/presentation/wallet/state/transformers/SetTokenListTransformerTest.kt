package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.tokens.TokenConverterParams
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.appcurrency.model.AppCurrency
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
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.state.model.*
import com.tangem.features.tangempay.entity.TangemPayMainUM
import com.tangem.features.virtualaccount.main.entity.VirtualAccountMainUM
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class SetTokenListTransformerTest {

    private val userWalletId = UserWalletId("00")

    @Test
    fun `GIVEN empty account list WHEN transform THEN tokens are Empty and buttons disabled`() {
        val transformer = createTransformer(accountList = emptyAccountList())

        val result = transformer.transform(walletUM(buttonEnabled = true)) as WalletUM.Content

        assertThat(result.tokensListUM).isInstanceOf(WalletTokensListUM.Empty::class.java)
        assertThat(result.buttons).hasSize(1)
        assertThat(result.buttons.single().isEnabled).isFalse()
    }

    @Test
    fun `GIVEN non-empty account list WHEN transform THEN tokens not Empty and buttons enabled`() {
        val transformer = createTransformer(accountList = nonEmptyAccountList())

        val result = transformer.transform(walletUM(buttonEnabled = false)) as WalletUM.Content

        assertThat(result.tokensListUM).isNotInstanceOf(WalletTokensListUM.Empty::class.java)
        assertThat(result.buttons).hasSize(1)
        assertThat(result.buttons.single().isEnabled).isTrue()
    }

    private fun createTransformer(accountList: AccountStatusList): SetTokenListTransformer {
        val userWallet = mockk<UserWallet.Hot>(relaxed = true) {
            every { walletId } returns userWalletId
        }
        return SetTokenListTransformer(
            params = TokenConverterParams.Account(
                accountList = accountList,
                expandedAccounts = emptySet(),
            ),
            userWallet = userWallet,
            appCurrency = AppCurrency.Default,
            clickIntents = mockk<WalletClickIntents>(relaxed = true),
            shouldShowMainPromo = false,
            isAccountsModeEnabled = false,
            isRedesignEnabled = true,
            isAddAndManageTokensEnabled = false,
            isMultipleCardsEnabled = false,
        )
    }

    private fun emptyAccountList(): AccountStatusList = AccountStatusList(
        userWalletId = userWalletId,
        accountStatuses = listOf(mainCryptoPortfolioStatus(tokenList = TokenList.Empty)),
        totalAccounts = 1,
        totalArchivedAccounts = 0,
        totalFiatBalance = TotalFiatBalance.Loading,
        sortType = TokensSortType.NONE,
        groupType = TokensGroupType.NONE,
    )

    private fun nonEmptyAccountList(): AccountStatusList {
        val token = createToken()
        val tokenList = TokenList.Ungrouped(
            currencies = listOf(createLoadedStatus(token)),
            totalFiatBalance = TotalFiatBalance.Loaded(BigDecimal.ZERO, StatusSource.ACTUAL),
            sortedBy = TokensSortType.NONE,
        )
        return AccountStatusList(
            userWalletId = userWalletId,
            accountStatuses = listOf(mainCryptoPortfolioStatus(tokenList = tokenList)),
            totalAccounts = 1,
            totalArchivedAccounts = 0,
            totalFiatBalance = TotalFiatBalance.Loading,
            sortType = TokensSortType.NONE,
            groupType = TokensGroupType.NONE,
        )
    }

    private fun mainCryptoPortfolioStatus(tokenList: TokenList): AccountStatus.CryptoPortfolio =
        AccountStatus.CryptoPortfolio(
            account = createMainAccount(userWalletId),
            tokenList = tokenList,
            priceChangeLce = Unit.lceError(),
        )

    private fun walletUM(buttonEnabled: Boolean): WalletUM.Content = WalletUM.Content(
        pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = {}),
        walletsBalanceUM = WalletBalanceUM.Loading(
            id = userWalletId,
            name = "test",
            deviceIcon = DeviceIconUM.Mobile,
        ),
        buttons = persistentListOf(
            TangemButtonUM(
                text = stringReference(value = "Buy"),
                type = TangemButtonType.Secondary,
                onClick = {},
                isEnabled = buttonEnabled,
            ),
        ),
        notifications = persistentListOf(),
        notificationsCarousel = persistentListOf(),
        tokensListUM = WalletTokensListUM.Loading,
        nftState = WalletNFTItemUM.Hidden,
        type = WalletType.Hot,
        tangemPayMainUM = TangemPayMainUM.Empty,
        virtualAccountMainUM = VirtualAccountMainUM.Empty,
    )

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
        val networkAddress = NetworkAddress.Single(
            defaultAddress = NetworkAddress.Address(
                value = "addr",
                type = NetworkAddress.Address.Type.Primary,
            ),
        )
        val value = CryptoCurrencyStatus.Loaded(
            amount = BigDecimal.ONE,
            fiatAmount = BigDecimal.ZERO,
            fiatRate = BigDecimal.ONE,
            priceChange = BigDecimal.ZERO,
            stakingBalance = null,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = networkAddress,
            sources = CryptoCurrencyStatus.Sources(),
        )
        return CryptoCurrencyStatus(currency = token, value = value)
    }
}