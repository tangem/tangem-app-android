package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.card.CardTypesResolver
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.tokendetails.presentation.tokendetails.state.AddFundsUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM.TitleState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TransferUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.ZeroBalanceActionsUM
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UpdateTopBarMenuTransformerTest {

    private val coldWallet: UserWallet.Cold = mockk(relaxed = true)
    private val hotWallet: UserWallet.Hot = mockk(relaxed = true)
    private val cardTypesResolver: CardTypesResolver = mockk(relaxed = true)
    private val onGenerateExtendedKey: () -> Unit = mockk(relaxed = true)
    private val onHideClick: () -> Unit = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        mockkStatic(UserWallet.Cold::cardTypesResolver)
        every { coldWallet.cardTypesResolver } returns cardTypesResolver
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(UserWallet.Cold::cardTypesResolver)
    }

    @Test
    fun `GIVEN cold wallet AND single wallet with token WHEN transform THEN menu is empty`() {
        // GIVEN
        every { cardTypesResolver.isSingleWalletWithToken() } returns true
        val transformer = createTransformer(
            userWallet = coldWallet,
            hasDerivations = true,
            isXPubSupported = true,
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.topAppBarUM.menuItems).isEmpty()
    }

    @Test
    fun `GIVEN cold wallet AND multi-wallet WHEN transform THEN Hide item is the only one`() {
        // GIVEN
        every { cardTypesResolver.isSingleWalletWithToken() } returns false
        val transformer = createTransformer(
            userWallet = coldWallet,
            hasDerivations = false,
            isXPubSupported = false,
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.topAppBarUM.menuItems).hasSize(1)

        result.topAppBarUM.menuItems.single().onClick()
        verify(exactly = 1) { onHideClick.invoke() }
        verify(exactly = 0) { onGenerateExtendedKey.invoke() }
    }

    @Test
    fun `GIVEN hot wallet WHEN transform THEN Hide item is shown regardless of single-wallet flag`() {
        // GIVEN — flag is read only for cold wallets, so no stub needed for hotWallet
        val transformer = createTransformer(
            userWallet = hotWallet,
            hasDerivations = false,
            isXPubSupported = false,
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.topAppBarUM.menuItems).hasSize(1)
    }

    @Test
    fun `GIVEN xPub supported AND derivations exist WHEN transform THEN both items are shown`() {
        // GIVEN
        every { cardTypesResolver.isSingleWalletWithToken() } returns false
        val transformer = createTransformer(
            userWallet = coldWallet,
            hasDerivations = true,
            isXPubSupported = true,
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN — Generate xPub first, Hide token second
        assertThat(result.topAppBarUM.menuItems).hasSize(2)
    }

    @Test
    fun `GIVEN xPub supported but no derivations WHEN transform THEN xPub item is hidden`() {
        // GIVEN
        every { cardTypesResolver.isSingleWalletWithToken() } returns false
        val transformer = createTransformer(
            userWallet = coldWallet,
            hasDerivations = false,
            isXPubSupported = true,
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.topAppBarUM.menuItems).hasSize(1)
    }

    @Test
    fun `GIVEN derivations exist but xPub unsupported WHEN transform THEN xPub item is hidden`() {
        // GIVEN
        every { cardTypesResolver.isSingleWalletWithToken() } returns false
        val transformer = createTransformer(
            userWallet = coldWallet,
            hasDerivations = true,
            isXPubSupported = false,
        )

        // WHEN
        val result = transformer.transform(initialState())

        // THEN
        assertThat(result.topAppBarUM.menuItems).hasSize(1)
    }

    @Test
    fun `GIVEN any state WHEN transform THEN unrelated state fields are preserved`() {
        // GIVEN
        every { cardTypesResolver.isSingleWalletWithToken() } returns false
        val state = initialState()
        val transformer = createTransformer(
            userWallet = coldWallet,
            hasDerivations = false,
            isXPubSupported = false,
        )

        // WHEN
        val result = transformer.transform(state)

        // THEN — only menuItems is touched
        assertThat(result.topAppBarUM.titleState).isEqualTo(state.topAppBarUM.titleState)
        assertThat(result.topAppBarUM.subtitle).isEqualTo(state.topAppBarUM.subtitle)
        assertThat(result.balanceBlockUM).isSameInstanceAs(state.balanceBlockUM)
        assertThat(result.marketPriceBlockState).isSameInstanceAs(state.marketPriceBlockState)
    }

    @Test
    fun `GIVEN callbacks WHEN menu items invoked THEN callbacks are dispatched`() {
        // GIVEN
        every { cardTypesResolver.isSingleWalletWithToken() } returns false
        val transformer = createTransformer(
            userWallet = coldWallet,
            hasDerivations = true,
            isXPubSupported = true,
        )

        // WHEN
        val result = transformer.transform(initialState())
        result.topAppBarUM.menuItems.forEach { it.onClick() }

        // THEN
        verify(exactly = 1) { onGenerateExtendedKey.invoke() }
        verify(exactly = 1) { onHideClick.invoke() }
    }

    private fun createTransformer(
        userWallet: UserWallet,
        hasDerivations: Boolean,
        isXPubSupported: Boolean,
    ) = UpdateTopBarMenuTransformer(
        userWallet = userWallet,
        hasDerivations = hasDerivations,
        isXPubSupported = isXPubSupported,
        onGenerateExtendedKey = onGenerateExtendedKey,
        onHideClick = onHideClick,
    )

    private fun initialState(): TokenDetailsUM = TokenDetailsUM(
        topAppBarUM = TokenDetailsTopAppBarUM(
            titleState = TitleState.Simple(tokenName = "Tether"),
            subtitle = stringReference("ERC-20 in Ethereum network"),
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