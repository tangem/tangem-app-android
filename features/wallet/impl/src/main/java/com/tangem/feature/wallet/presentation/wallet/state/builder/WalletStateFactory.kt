package com.tangem.feature.wallet.presentation.wallet.state.builder

import com.tangem.common.Provider
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import com.tangem.feature.wallet.presentation.wallet.domain.WalletImageResolver
import com.tangem.feature.wallet.presentation.wallet.state.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal class WalletStateFactory(
    private val routerProvider: Provider<InnerWalletRouter>,
    private val onScanCardClick: () -> Unit,
    private val onWalletChange: (Int) -> Unit,
    private val onRefreshSwipe: () -> Unit,
) {

    fun getInitialState(): WalletStateHolder = WalletStateHolder.Loading(onBackClick = ::onBackClick)

    fun getContentState(wallets: List<UserWallet>): WalletStateHolder {
        val cardTypeResolver = requireNotNull(wallets.firstOrNull()).scanResponse.cardTypesResolver

        return if (cardTypeResolver.isMultiwalletAllowed()) {
            createMultiCurrencyState(wallets)
        } else {
            createSingleCurrencyState(wallets)
        }
    }

    private fun createMultiCurrencyState(wallets: List<UserWallet>): WalletStateHolder.MultiCurrencyContent {
        return WalletStateHolder.MultiCurrencyContent(
            onBackClick = ::onBackClick,
            topBarConfig = createTopBarConfig(),
            walletsListConfig = createWalletsListConfig(wallets),
            pullToRefreshConfig = createPullToRefreshConfig(),
            contentItems = persistentListOf(),
            notifications = persistentListOf(),
            bottomSheet = WalletBottomSheetConfig(
// [REDACTED_TODO_COMMENT]
                isShow = false,
                onDismissRequest = {},
                content = WalletBottomSheetConfig.BottomSheetContentConfig.LikeTangemApp(
                    onRateTheAppClick = {},
                    onShareClick = {},
                ),
            ),
            onOrganizeTokensClick = routerProvider()::openOrganizeTokensScreen,
        )
    }

    private fun createSingleCurrencyState(wallets: List<UserWallet>): WalletStateHolder.SingleCurrencyContent {
        return WalletStateHolder.SingleCurrencyContent(
            onBackClick = ::onBackClick,
            topBarConfig = createTopBarConfig(),
            walletsListConfig = createWalletsListConfig(wallets),
            pullToRefreshConfig = createPullToRefreshConfig(),
            contentItems = persistentListOf(),
            notifications = persistentListOf(),
            bottomSheet = WalletBottomSheetConfig(
// [REDACTED_TODO_COMMENT]
                isShow = false,
                onDismissRequest = {},
                content = WalletBottomSheetConfig.BottomSheetContentConfig.LikeTangemApp(
                    onRateTheAppClick = {},
                    onShareClick = {},
                ),
            ),
            buttons = WalletPreviewData.singleWalletScreenState.buttons, // TODO: create buttons
// [REDACTED_TODO_COMMENT]
            marketPriceBlockState = WalletPreviewData.singleWalletScreenState.marketPriceBlockState,
        )
    }

    private fun onBackClick() = routerProvider().popBackStack()

    private fun createTopBarConfig(): WalletTopBarConfig {
        return WalletTopBarConfig(
            onScanCardClick = onScanCardClick,
            onMoreClick = routerProvider()::openDetailsScreen,
        )
    }

    private fun createWalletsListConfig(wallets: List<UserWallet>): WalletsListConfig {
        return WalletsListConfig(
            selectedWalletIndex = 0,
            wallets = wallets.map { wallet ->
                WalletCardState.Loading(
                    id = if (wallet.scanResponse.cardTypesResolver.isMultiwalletAllowed()) { // TODO
                        UserWalletId("123")
                    } else {
                        UserWalletId("321")
                    },
// [REDACTED_TODO_COMMENT]
                    title = wallet.name,
                    additionalInfo = "", // TODO
                    imageResId = WalletImageResolver.resolve(cardTypesResolver = wallet.scanResponse.cardTypesResolver),
                )
            }.toImmutableList(),
            onWalletChange = onWalletChange,
        )
    }

    private fun createPullToRefreshConfig(): WalletPullToRefreshConfig {
        return WalletPullToRefreshConfig(isRefreshing = false, onRefresh = onRefreshSwipe)
    }
}
