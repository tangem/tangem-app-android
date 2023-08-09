package com.tangem.feature.wallet.presentation.wallet.state.factory

import androidx.paging.PagingData
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.domain.WalletAdditionalInfoFactory
import com.tangem.feature.wallet.presentation.wallet.domain.WalletImageResolver
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import com.tangem.feature.wallet.presentation.wallet.state.components.*
import com.tangem.feature.wallet.presentation.wallet.state.factory.WalletSkeletonStateConverter.SkeletonModel
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
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
) : Converter<SkeletonModel, WalletStateHolder> {

    override fun convert(value: SkeletonModel): WalletStateHolder {
        val wallet = requireNotNull(value.wallets.getOrNull(value.selectedWalletIndex)) { "Empty wallet list" }
        val cardTypeResolver = wallet.scanResponse.cardTypesResolver

        return when {
            cardTypeResolver.isMultiwalletAllowed() -> createMultiCurrencyState(value)
            !cardTypeResolver.isMultiwalletAllowed() -> createSingleCurrencyState(value, cardTypeResolver)
            else -> error("Illegal wallet state: $wallet")
        }
    }

    /**
     * Create [WalletMultiCurrencyState.Content].
     * Tokens and notifications are updated asynchronously.
     *
     * @param value converted value
     */
    private fun createMultiCurrencyState(value: SkeletonModel): WalletMultiCurrencyState.Content {
        return WalletMultiCurrencyState.Content(
            onBackClick = clickIntents::onBackClick,
            topBarConfig = createTopBarConfig(),
            walletsListConfig = createWalletsListConfig(value),
            pullToRefreshConfig = createPullToRefreshConfig(),
            tokensListState = WalletTokensListState.Content(
                items = persistentListOf(),
                onOrganizeTokensClick = null,
            ),
            notifications = persistentListOf(),
            bottomSheetConfig = null,
        )
    }

    /**
     * Create [WalletSingleCurrencyState.Content].
     * Transactions, notifications and market price are updated asynchronously.
     *
     * @param value            converted value
     * @param cardTypeResolver card type resolver
     */
    private fun createSingleCurrencyState(
        value: SkeletonModel,
        cardTypeResolver: CardTypesResolver,
    ): WalletSingleCurrencyState.Content {
        return WalletSingleCurrencyState.Content(
            onBackClick = clickIntents::onBackClick,
            topBarConfig = createTopBarConfig(),
            walletsListConfig = createWalletsListConfig(value),
            pullToRefreshConfig = createPullToRefreshConfig(),
            notifications = persistentListOf(),
            bottomSheetConfig = null,
            buttons = getButtons(),
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

    private fun createWalletsListConfig(value: SkeletonModel): WalletsListConfig {
        return WalletsListConfig(
            selectedWalletIndex = value.selectedWalletIndex,
            wallets = value.wallets.map { wallet ->
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
// [REDACTED_TODO_COMMENT]
    private fun getButtons(): ImmutableList<WalletManageButton> {
        return persistentListOf(
            WalletManageButton.Buy(onClick = {}),
            WalletManageButton.Send(onClick = {}),
            WalletManageButton.Receive(onClick = {}),
            WalletManageButton.Exchange(onClick = {}),
            WalletManageButton.CopyAddress(onClick = {}),
        )
    }

    data class SkeletonModel(
        val wallets: List<UserWallet>,
        val selectedWalletIndex: Int,
    )
}
