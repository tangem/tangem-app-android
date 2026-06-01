package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.feature.tokendetails.presentation.tokendetails.state.AddFundsUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM.TitleState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TransferUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.ZeroBalanceActionsUM
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test

class SetTopBarTitleTransformerTest {

    private val tokenName = "Tether"
    private val walletName = "My Wallet"
    private val deviceIconUM: DeviceIconUM = DeviceIconUM.Card(
        mainColor = Color.DarkGray,
        secondColor = null,
    )
    private val cryptoCurrency: CryptoCurrency.Coin = mockk(relaxed = true) {
        every { name } returns tokenName
    }

    @Test
    fun `GIVEN single wallet single account WHEN transform THEN Simple title`() {
        // GIVEN
        val transformer = createTransformer(
            hasMultipleWallets = false,
            hasMultipleAccounts = false,
            account = null,
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.topAppBarUM.titleState).isEqualTo(TitleState.Simple(tokenName = tokenName))
    }

    @Test
    fun `GIVEN multiple wallets and single account WHEN transform THEN WithWallet title`() {
        // GIVEN
        val transformer = createTransformer(
            hasMultipleWallets = true,
            hasMultipleAccounts = false,
            account = null,
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val expected = TitleState.WithWallet(
            tokenName = tokenName,
            walletName = walletName,
            deviceIconUM = deviceIconUM,
        )
        assertThat(result.topAppBarUM.titleState).isEqualTo(expected)
    }

    @Test
    fun `GIVEN single wallet and multiple accounts WHEN transform THEN WithAccount title`() {
        // GIVEN
        val transformer = createTransformer(
            hasMultipleWallets = false,
            hasMultipleAccounts = true,
            account = stubAccount(),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val title = result.topAppBarUM.titleState
        assertThat(title).isInstanceOf(TitleState.WithAccount::class.java)
        assertThat((title as TitleState.WithAccount).tokenName).isEqualTo(tokenName)
    }

    @Test
    fun `GIVEN multiple wallets AND multiple accounts WHEN transform THEN WithAccount wins`() {
        // GIVEN — design priority: account branch wins over wallet branch
        val transformer = createTransformer(
            hasMultipleWallets = true,
            hasMultipleAccounts = true,
            account = stubAccount(),
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.topAppBarUM.titleState).isInstanceOf(TitleState.WithAccount::class.java)
    }

    @Test
    fun `GIVEN multiple accounts but null account WHEN transform THEN falls back to wallet branch`() {
        // GIVEN — race protection: hasMultipleAccounts=true but account not loaded yet
        val transformer = createTransformer(
            hasMultipleWallets = true,
            hasMultipleAccounts = true,
            account = null,
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.topAppBarUM.titleState).isInstanceOf(TitleState.WithWallet::class.java)
    }

    @Test
    fun `GIVEN multiple accounts but null account AND single wallet WHEN transform THEN falls back to Simple`() {
        // GIVEN
        val transformer = createTransformer(
            hasMultipleWallets = false,
            hasMultipleAccounts = true,
            account = null,
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.topAppBarUM.titleState).isEqualTo(TitleState.Simple(tokenName = tokenName))
    }

    @Test
    fun `GIVEN account with custom icon WHEN transform THEN icon is propagated`() {
        // GIVEN
        val account = stubAccount(
            iconValue = CryptoPortfolioIcon.Icon.Star,
            iconColor = CryptoPortfolioIcon.Color.Azure,
        )
        val transformer = createTransformer(
            hasMultipleWallets = false,
            hasMultipleAccounts = true,
            account = account,
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        val title = result.topAppBarUM.titleState as TitleState.WithAccount
        val expectedIcon = AccountIconUM.CryptoPortfolio(
            value = CryptoPortfolioIcon.Icon.Star,
            color = CryptoPortfolioIcon.Color.Azure,
        )
        assertThat(title.accountIconUM).isEqualTo(expectedIcon)
    }

    private fun createTransformer(
        hasMultipleWallets: Boolean,
        hasMultipleAccounts: Boolean,
        account: Account.CryptoPortfolio?,
    ) = SetTopBarTitleTransformer(
        cryptoCurrency = cryptoCurrency,
        hasMultipleWallets = hasMultipleWallets,
        hasMultipleAccounts = hasMultipleAccounts,
        walletName = walletName,
        deviceIconUM = deviceIconUM,
        account = account,
    )

    private fun stubAccount(
        iconValue: CryptoPortfolioIcon.Icon = CryptoPortfolioIcon.Icon.Star,
        iconColor: CryptoPortfolioIcon.Color = CryptoPortfolioIcon.Color.Azure,
    ): Account.CryptoPortfolio {
        val icon: CryptoPortfolioIcon = mockk {
            every { value } returns iconValue
            every { color } returns iconColor
        }
        return mockk {
            every { accountName } returns AccountName.DefaultMain
            every { this@mockk.icon } returns icon
        }
    }

    private fun initialState(): TokenDetailsUM = TokenDetailsUM(
        topAppBarUM = TokenDetailsTopAppBarUM(
            titleState = TitleState.Simple(tokenName = ""),
            subtitle = stringReference(""),
            onBackClick = {},
            menuItems = persistentListOf(),
        ),
        balanceBlockUM = mockk<TokenDetailsBalanceBlockUM>(relaxed = true),
        notifications = persistentListOf(),
        marketPriceBlockState = mockk<MarketPriceBlockState>(relaxed = true),
        earnBlockState = null,
        pullToRefreshConfig = mockk<PullToRefreshConfig>(relaxed = true),
        isBalanceHidden = false,
        isMarketPriceAvailable = false,
        addFundsUM = AddFundsUM.Loading,
        transferUM = TransferUM.Loading,
        zeroBalanceActionsUM = ZeroBalanceActionsUM.Loading,
    )
}