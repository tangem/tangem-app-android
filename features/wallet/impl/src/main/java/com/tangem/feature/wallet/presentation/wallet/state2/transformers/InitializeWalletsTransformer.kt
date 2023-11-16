package com.tangem.feature.wallet.presentation.wallet.state2.transformers

import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.domain.WalletAdditionalInfoFactory
import com.tangem.feature.wallet.presentation.wallet.domain.WalletImageResolver
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletManageButton
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTopBarConfig
import com.tangem.feature.wallet.presentation.wallet.state2.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state2.WalletState
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletsUpdateActionResolverV2
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
[REDACTED_AUTHOR]
 */
internal class InitializeWalletsTransformer(
    private val action: WalletsUpdateActionResolverV2.Action.InitializeWallets,
    private val clickIntents: WalletClickIntentsV2,
) : WalletScreenStateTransformer {

    private val walletStateFactory by lazy { WalletStateFactory(clickIntents = clickIntents) }

    override fun transform(prevState: WalletScreenState): WalletScreenState {
        return prevState.copy(
            onBackClick = clickIntents::onBackClick,
            topBarConfig = createTopBarConfig(userWallet = action.selectedWallet),
            selectedWalletIndex = action.selectedWalletIndex,
            wallets = action.wallets
                .map { userWallet ->
                    if (userWallet.isLocked) {
                        createLockedState(userWallet)
                    } else {
                        walletStateFactory.createLoadingState(userWallet)
                    }
                }
                .toImmutableList(),
            onWalletChange = clickIntents::onWalletChange,
        )
    }

    private fun createTopBarConfig(userWallet: UserWallet): WalletTopBarConfig {
        return WalletTopBarConfig(
            onDetailsClick = if (userWallet.isLocked) {
                clickIntents::onOpenUnlockWalletsBottomSheetClick
            } else {
                clickIntents::onDetailsClick
            },
        )
    }

    private fun createLockedState(userWallet: UserWallet): WalletState {
        return walletStateFactory.createStateByWalletType(
            userWallet = userWallet,
            multiCurrencyCreator = {
                WalletState.MultiCurrency.Locked(
                    walletCardState = userWallet.toLockedWalletCardState(),
                    onUnlockNotificationClick = clickIntents::onOpenUnlockWalletsBottomSheetClick,
                    onUnlockClick = clickIntents::onUnlockWalletClick,
                    onScanClick = clickIntents::onScanToUnlockWalletClick,
                )
            },
            singleCurrencyCreator = {
                WalletState.SingleCurrency.Locked(
                    walletCardState = userWallet.toLockedWalletCardState(),
                    buttons = createDisabledButtons(),
                    onUnlockNotificationClick = clickIntents::onOpenUnlockWalletsBottomSheetClick,
                    onUnlockClick = clickIntents::onUnlockWalletClick,
                    onScanClick = clickIntents::onScanToUnlockWalletClick,
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
            onRenameClick = clickIntents::onRenameClick,
            onDeleteClick = clickIntents::onDeleteBeforeConfirmationClick,
        )
    }

    private fun createDisabledButtons(): PersistentList<WalletManageButton> {
        return persistentListOf(
            WalletManageButton.Buy(enabled = false, onClick = {}),
            WalletManageButton.Send(enabled = false, onClick = {}),
            WalletManageButton.Receive(enabled = false, onClick = {}),
            WalletManageButton.Sell(enabled = false, onClick = {}),
        )
    }
}