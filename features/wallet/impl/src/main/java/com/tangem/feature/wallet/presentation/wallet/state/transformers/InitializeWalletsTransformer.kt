package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.domain.WalletAdditionalInfoFactory
import com.tangem.feature.wallet.presentation.wallet.domain.WalletImageResolver
import com.tangem.feature.wallet.presentation.wallet.state.model.*
import com.tangem.feature.wallet.presentation.wallet.state.utils.WalletLoadingStateFactory
import com.tangem.feature.wallet.presentation.wallet.state.utils.createStateByWalletType
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal class InitializeWalletsTransformer(
    private val selectedWalletIndex: Int,
    private val wallets: List<UserWallet>,
    private val clickIntents: WalletClickIntents,
) : WalletScreenStateTransformer {

    private val walletLoadingStateFactory by lazy { WalletLoadingStateFactory(clickIntents = clickIntents) }

    override fun transform(prevState: WalletScreenState): WalletScreenState {
        return prevState.copy(
            onBackClick = clickIntents::onBackClick,
            topBarConfig = createTopBarConfig(),
            selectedWalletIndex = selectedWalletIndex,
            wallets = wallets
                .map { userWallet ->
                    if (userWallet.isLocked) {
                        createLockedState(userWallet)
                    } else {
                        walletLoadingStateFactory.create(userWallet)
                    }
                }
                .toImmutableList(),
            onWalletChange = clickIntents::onWalletChange,
        )
    }

    private fun createTopBarConfig(): WalletTopBarConfig {
        return WalletTopBarConfig(
            onDetailsClick = clickIntents::onDetailsClick,
        )
    }

    private fun createLockedState(userWallet: UserWallet): WalletState {
        return userWallet.createStateByWalletType(
            multiCurrencyCreator = {
                WalletState.MultiCurrency.Locked(
                    walletCardState = userWallet.toLockedWalletCardState(),
                    bottomSheetConfig = null,
                    onUnlockNotificationClick = clickIntents::onOpenUnlockWalletsBottomSheetClick,
                )
            },
            singleCurrencyCreator = {
                WalletState.SingleCurrency.Locked(
                    walletCardState = userWallet.toLockedWalletCardState(),
                    bottomSheetConfig = null,
                    buttons = createDisabledButtons(),
                    onUnlockNotificationClick = clickIntents::onOpenUnlockWalletsBottomSheetClick,
                    onExploreClick = clickIntents::onExploreClick,
                )
            },
            visaWalletCreator = {
                WalletState.Visa.Locked(
                    walletCardState = userWallet.toLockedWalletCardState(),
                    bottomSheetConfig = null,
                    onUnlockNotificationClick = clickIntents::onOpenUnlockWalletsBottomSheetClick,
                    onExploreClick = clickIntents::onExploreClick,
                )
            },
        )
    }

    private fun UserWallet.toLockedWalletCardState(): WalletCardState {
        return WalletCardState.LockedContent(
            id = walletId,
            title = name,
            additionalInfo = WalletAdditionalInfoFactory.resolve(wallet = this),
            imageResId = WalletImageResolver.resolve(userWallet = this),
            onRenameClick = clickIntents::onRenameBeforeConfirmationClick,
            onDeleteClick = clickIntents::onDeleteBeforeConfirmationClick,
        )
    }

    private fun createDisabledButtons(): PersistentList<WalletManageButton> {
        return persistentListOf(
            WalletManageButton.Receive(enabled = false, dimContent = false, onClick = {}, onLongClick = null),
            WalletManageButton.Send(enabled = false, dimContent = false, onClick = {}),
            WalletManageButton.Buy(enabled = false, dimContent = false, onClick = {}),
            WalletManageButton.Sell(enabled = false, dimContent = false, onClick = {}),
        )
    }
}
