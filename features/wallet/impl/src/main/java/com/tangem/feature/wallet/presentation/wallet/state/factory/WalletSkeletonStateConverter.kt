package com.tangem.feature.wallet.presentation.wallet.state.factory

import androidx.paging.PagingData
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.wallet.domain.WalletAdditionalInfoFactory
import com.tangem.feature.wallet.presentation.wallet.domain.WalletImageResolver
import com.tangem.feature.wallet.presentation.wallet.state.*
import com.tangem.feature.wallet.presentation.wallet.state.content.WalletTokensListState
import com.tangem.feature.wallet.presentation.wallet.state.content.WalletTxHistoryState
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.flow

/**
 * Converter from loaded list of [UserWallet] to skeleton state of screen [WalletStateHolder]
 *
 * @property clickIntents screen click intents
 *
* [REDACTED_AUTHOR]
 */
internal class WalletSkeletonStateConverter(
    private val clickIntents: WalletClickIntents,
) : Converter<List<UserWallet>, WalletStateHolder> {

    override fun convert(value: List<UserWallet>): WalletStateHolder {
        val cardTypeResolver = requireNotNull(value.firstOrNull()).scanResponse.cardTypesResolver

        return if (cardTypeResolver.isMultiwalletAllowed()) {
            createMultiCurrencyState(value)
        } else {
            createSingleCurrencyState(value, cardTypeResolver)
        }
    }

    private fun createMultiCurrencyState(wallets: List<UserWallet>): WalletStateHolder.MultiCurrencyContent {
        return WalletStateHolder.MultiCurrencyContent(
            onBackClick = clickIntents::onBackClick,
            topBarConfig = createTopBarConfig(),
            walletsListConfig = createWalletsListConfig(wallets),
            pullToRefreshConfig = createPullToRefreshConfig(),
            tokensListState = WalletTokensListState.Content(
                items = persistentListOf(),
                onOrganizeTokensClick = clickIntents::onOrganizeTokensClick,
            ),
            notifications = persistentListOf(),
            bottomSheet = null,
        )
    }

    private fun createSingleCurrencyState(
        wallets: List<UserWallet>,
        cardTypeResolver: CardTypesResolver,
    ): WalletStateHolder.SingleCurrencyContent {
        return WalletStateHolder.SingleCurrencyContent(
            onBackClick = clickIntents::onBackClick,
            topBarConfig = createTopBarConfig(),
            walletsListConfig = createWalletsListConfig(wallets),
            pullToRefreshConfig = createPullToRefreshConfig(),
            notifications = persistentListOf(),
            bottomSheet = null,
            buttons = WalletPreviewData.singleWalletScreenState.buttons, // TODO: create buttons
            marketPriceBlockState = MarketPriceBlockState.Loading(
                currencyName = cardTypeResolver.getBlockchain().currency,
            ),
            txHistoryState = WalletTxHistoryState.Content(
                items = flow { PagingData.empty<WalletTxHistoryState.TxHistoryItemState>() },
            ),
        )
    }

    private fun createTopBarConfig(): WalletTopBarConfig {
        return WalletTopBarConfig(
            onScanCardClick = clickIntents::onScanCardClick,
            onMoreClick = clickIntents::onDetailsClick,
        )
    }

    private fun createWalletsListConfig(wallets: List<UserWallet>): WalletsListConfig {
        return WalletsListConfig(
            selectedWalletIndex = 0,
            wallets = wallets.map { wallet ->
                val cardTypeResolver = wallet.scanResponse.cardTypesResolver
                WalletCardState.Loading(
                    id = wallet.walletId,
                    title = wallet.name,
                    additionalInfo = WalletAdditionalInfoFactory.resolve(
                        cardTypesResolver = cardTypeResolver,
                        isLocked = wallet.isLocked,
                    ),
                    imageResId = WalletImageResolver.resolve(cardTypesResolver = cardTypeResolver),
                )
            }.toImmutableList(),
            onWalletChange = clickIntents::onWalletChange,
        )
    }

    private fun createPullToRefreshConfig(): WalletPullToRefreshConfig {
        return WalletPullToRefreshConfig(isRefreshing = false, onRefresh = clickIntents::onRefreshSwipe)
    }
}
