package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.common.ui.userwallet.converter.WalletIconUMConverter
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.topbar.TangemTopBarActionUM
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.domain.WalletAdditionalInfoFactory
import com.tangem.feature.wallet.presentation.wallet.domain.WalletImageResolver
import com.tangem.feature.wallet.presentation.wallet.state.model.*
import com.tangem.feature.wallet.presentation.wallet.state.utils.WalletLoadingStateFactory
import com.tangem.feature.wallet.presentation.wallet.state.utils.createStateByWalletType
import com.tangem.feature.wallet.presentation.wallet.state.utils.isSingleWallet
import com.tangem.utils.extensions.addIf
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import com.tangem.core.ui.R as CoreUiR

internal class InitializeWalletsTransformer(
    private val selectedWalletIndex: Int,
    private val wallets: List<UserWallet>,
    private val clickIntents: WalletClickIntents,
    private val walletImageResolver: WalletImageResolver,
    private val getWalletIconUseCase: GetWalletIconUseCase,
    private val isMainScreenQrScanningEnabled: Boolean = false,
) : WalletScreenStateTransformer {

    private val walletLoadingStateFactory by lazy {
        WalletLoadingStateFactory(
            clickIntents = clickIntents,
            walletImageResolver = walletImageResolver,
            getWalletIconUseCase = getWalletIconUseCase,
        )
    }

    override fun transform(prevState: WalletScreenState): WalletScreenState {
        return prevState.copy(
            topBarConfig = createTopBarConfig(),
            selectedWalletIndex = selectedWalletIndex,
            wallets = wallets
                .map { userWallet ->
                    if (userWallet.isLocked) {
                        createLockedState(userWallet)
                    } else {
                        walletLoadingStateFactory.create(
                            userWallet = userWallet,
                        )
                    }
                }
                .toImmutableList(),
            wallets2 = wallets
                .map(::createInitState)
                .toImmutableList(),
            onWalletChange = clickIntents::onWalletChange,
            onDismissMarketsTooltip = clickIntents::onDismissMarketsTooltip,
        )
    }

    private fun createTopBarConfig(): WalletTopBarConfig {
        return WalletTopBarConfig(
            endActions = listOfNotNull(
                if (isMainScreenQrScanningEnabled) {
                    TangemTopBarActionUM(
                        iconRes = CoreUiR.drawable.ic_qrcode_scaner_24,
                        onClick = clickIntents::onScanQrClick,
                    )
                } else {
                    null
                },
                TangemTopBarActionUM(
                    iconRes = CoreUiR.drawable.ic_more_default_24,
                    onClick = clickIntents::onDetailsClick,
                ),
            ).toPersistentList(),
        )
    }

    private fun createLockedState(userWallet: UserWallet): WalletState {
        return userWallet.createStateByWalletType(
            multiCurrencyCreator = {
                WalletState.MultiCurrency.Locked(
                    walletCardState = userWallet.toLockedWalletCardState(),
                    buttons = createMultiWalletEnabledButtons(userWallet),
                    bottomSheetConfig = null,
                    onUnlockNotificationClick = clickIntents::onOpenUnlockWalletsBottomSheetClick,
                    type = when (userWallet) {
                        is UserWallet.Cold -> WalletType.Cold
                        is UserWallet.Hot -> WalletType.Hot
                    },
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
        )
    }

    private fun createInitState(userWallet: UserWallet): WalletUM {
        return if (userWallet.isLocked) {
            userWallet.toLockedWalletUM()
        } else {
            walletLoadingStateFactory.create2(
                userWallet = userWallet,
            )
        }
    }

    private fun UserWallet.toLockedWalletCardState(): WalletCardState {
        return WalletCardState.LockedContent(
            id = walletId,
            title = name,
            additionalInfo = WalletAdditionalInfoFactory.resolve(wallet = this),
            imageResId = walletImageResolver.resolve(userWallet = this),
            dropDownItems = persistentListOf(),
        )
    }

    private fun UserWallet.toLockedWalletUM(): WalletUM.Locked {
        return WalletUM.Locked(
            walletsBalanceUM = WalletBalanceUM.Empty(
                id = walletId,
                name = name,
                deviceIcon = getWalletIconUseCase.invoke(userWallet = this)
                    .let { WalletIconUMConverter().convert(it) },
            ),
            buttons = createWalletActions(userWallet = this),
            type = when (this) {
                is UserWallet.Cold -> WalletType.Cold
                is UserWallet.Hot -> WalletType.Hot
            },
            notifications = persistentListOf(
                WalletNotificationUM.UnlockWallets(
                    onClick = clickIntents::onOpenUnlockWalletsBottomSheetClick,
                ),
            ),
        )
    }

    private fun createMultiWalletEnabledButtons(userWallet: UserWallet): PersistentList<WalletManageButton> {
        val isSingleWalletWithToken = userWallet is UserWallet.Cold &&
            userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()
        if (isSingleWalletWithToken) return persistentListOf()

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

    private fun createWalletActions(userWallet: UserWallet): PersistentList<TangemButtonUM> {
        return buildList {
            add(
                WalletActionButtons.Buy(
                    isEnabled = false,
                    onClick = {},
                ).buttonUM,
            )
            addIf(
                condition = !userWallet.isSingleWallet(),
                element = WalletActionButtons.Swap(
                    isEnabled = false,
                    onClick = {},
                ).buttonUM,
            )
            add(
                WalletActionButtons.Sell(
                    isEnabled = false,
                    onClick = {},
                ).buttonUM,
            )
        }.toPersistentList()
    }
}