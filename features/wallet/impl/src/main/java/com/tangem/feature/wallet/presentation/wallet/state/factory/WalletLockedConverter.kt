package com.tangem.feature.wallet.presentation.wallet.state.factory

import com.tangem.common.Provider
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.domain.WalletAdditionalInfoFactory
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletManageButton
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTopBarConfig
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletsListConfig
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal class WalletLockedConverter(
    private val currentStateProvider: Provider<WalletState>,
    private val currentCardTypeResolverProvider: Provider<CardTypesResolver>,
    private val currentWalletProvider: Provider<UserWallet>,
    private val clickIntents: WalletClickIntents,
) : Converter<Unit, WalletState> {

    override fun convert(value: Unit): WalletState {
        return when (val state = currentStateProvider()) {
            is WalletState.ContentState -> {
                val cardTypeResolver = currentCardTypeResolverProvider()

                if (cardTypeResolver.isMultiwalletAllowed()) {
                    state.toMultiCurrencyLockedState(cardTypeResolver)
                } else {
                    state.toSingleCurrencyLockedState(cardTypeResolver)
                }
            }
            is WalletState.Initial -> state
        }
    }

    private fun WalletState.ContentState.toMultiCurrencyLockedState(
        cardTypeResolver: CardTypesResolver,
    ): WalletMultiCurrencyState.Locked {
        return WalletMultiCurrencyState.Locked(
            onBackClick = onBackClick,
            topBarConfig = createTopBarConfig(),
            walletsListConfig = createWalletsListConfig(cardTypeResolver),
            pullToRefreshConfig = pullToRefreshConfig,
            onUnlockWalletsNotificationClick = clickIntents::onUnlockWalletNotificationClick,
            onUnlockClick = clickIntents::onUnlockWalletClick,
            onScanClick = clickIntents::onScanCardClick,
        )
    }

    private fun WalletState.ContentState.toSingleCurrencyLockedState(
        cardTypeResolver: CardTypesResolver,
    ): WalletSingleCurrencyState.Locked {
        return WalletSingleCurrencyState.Locked(
            onBackClick = onBackClick,
            topBarConfig = createTopBarConfig(),
            walletsListConfig = createWalletsListConfig(cardTypeResolver),
            pullToRefreshConfig = pullToRefreshConfig,
            buttons = createButtons(),
            onUnlockWalletsNotificationClick = clickIntents::onUnlockWalletNotificationClick,
            onUnlockClick = clickIntents::onUnlockWalletClick,
            onScanClick = clickIntents::onScanCardClick,
            onExploreClick = clickIntents::onExploreClick,
        )
    }

    private fun WalletState.ContentState.createTopBarConfig(): WalletTopBarConfig {
        return topBarConfig.copy(onMoreClick = clickIntents::onUnlockWalletNotificationClick)
    }

    private fun WalletState.ContentState.createWalletsListConfig(
        cardTypeResolver: CardTypesResolver,
    ): WalletsListConfig {
        return walletsListConfig.copy(
            wallets = walletsListConfig.wallets
                .map { walletCardState ->
                    WalletCardState.LockedContent(
                        id = walletCardState.id,
                        title = walletCardState.title,
                        additionalInfo = if (cardTypeResolver.isMultiwalletAllowed()) {
                            WalletAdditionalInfoFactory.resolve(
                                cardTypesResolver = cardTypeResolver,
                                wallet = currentWalletProvider(),
                            )
                        } else {
                            null
                        },
                        imageResId = walletCardState.imageResId,
                        onRenameClick = walletCardState.onRenameClick,
                        onDeleteClick = walletCardState.onDeleteClick,
                    )
                }
                .toImmutableList(),
        )
    }

    private fun createButtons(): ImmutableList<WalletManageButton> {
        return persistentListOf(
            WalletManageButton.Buy(enabled = false, onClick = {}),
            WalletManageButton.Send(enabled = false, onClick = {}),
            WalletManageButton.Receive(onClick = {}),
            WalletManageButton.Sell(enabled = false, onClick = {}),
        )
    }
}