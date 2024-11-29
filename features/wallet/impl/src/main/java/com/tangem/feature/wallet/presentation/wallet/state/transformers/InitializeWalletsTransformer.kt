package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.domain.WalletAdditionalInfoFactory
import com.tangem.feature.wallet.presentation.wallet.domain.WalletImageResolver
import com.tangem.feature.wallet.presentation.wallet.state.model.*
import com.tangem.feature.wallet.presentation.wallet.state.utils.WalletLoadingStateFactory
import com.tangem.feature.wallet.presentation.wallet.state.utils.createStateByWalletType
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents
import com.tangem.features.wallet.featuretoggles.WalletFeatureToggles
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal class InitializeWalletsTransformer(
    private val selectedWalletIndex: Int,
    private val wallets: List<UserWallet>,
    private val clickIntents: WalletClickIntents,
    private val walletImageResolver: WalletImageResolver,
    private val walletFeatureToggles: WalletFeatureToggles,
) : WalletScreenStateTransformer {

    private val walletLoadingStateFactory by lazy {
        WalletLoadingStateFactory(
            clickIntents = clickIntents,
            walletImageResolver = walletImageResolver,
            walletFeatureToggles = walletFeatureToggles,
        )
    }

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
            onDismissMarketsOnboarding = clickIntents::onDismissMarketsOnboarding,
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
                    buttons = createMultiWalletEnabledButtons(),
                    bottomSheetConfig = null,
                    onUnlockNotificationClick = clickIntents::onOpenUnlockWalletsBottomSheetClick,
                )
            },
            singleCurrencyCreator = {
                WalletState.SingleCurrency.Locked(
                    walletCardState = userWallet.toLockedWalletCardState(),
                    bottomSheetConfig = null,
                    buttons = createSingleWalletDisabledButtons(),
                    onUnlockNotificationClick = clickIntents::onOpenUnlockWalletsBottomSheetClick,
                    onExploreClick = clickIntents::onExploreClick,
                )
            },
            visaWalletCreator = {
                WalletState.Visa.Locked(
                    walletCardState = userWallet.toLockedWalletCardState(),
                    buttons = createMultiWalletEnabledButtons(),
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
            imageResId = walletImageResolver.resolve(userWallet = this),
            onRenameClick = clickIntents::onRenameBeforeConfirmationClick,
            onDeleteClick = clickIntents::onDeleteBeforeConfirmationClick,
        )
    }

    private fun createMultiWalletEnabledButtons(): PersistentList<WalletManageButton> {
        if (!walletFeatureToggles.isMainActionButtonsEnabled) return persistentListOf()

        return persistentListOf(
            WalletManageButton.Buy(enabled = false, dimContent = false, onClick = {}),
            WalletManageButton.Swap(enabled = false, dimContent = false, onClick = {}),
            WalletManageButton.Sell(enabled = false, dimContent = false, onClick = {}),
        )
    }

    private fun createSingleWalletDisabledButtons(): PersistentList<WalletManageButton> {
        return persistentListOf(
            WalletManageButton.Receive(enabled = false, dimContent = false, onClick = {}, onLongClick = null),
            WalletManageButton.Send(enabled = false, dimContent = false, onClick = {}),
            WalletManageButton.Buy(enabled = false, dimContent = false, onClick = {}),
            WalletManageButton.Sell(enabled = false, dimContent = false, onClick = {}),
        )
    }
}
