package com.tangem.feature.wallet.presentation.wallet.state.model.holder

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.pullToRefresh.PullToRefreshConfig
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletManageButton
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

internal interface WalletStateHolder {

    val pullToRefreshConfig: PullToRefreshConfig
    val walletCardState: WalletCardState
    val buttons: PersistentList<WalletManageButton>
    val warnings: ImmutableList<WalletNotification>
    val bottomSheetConfig: TangemBottomSheetConfig?
}

internal class LockedWalletStateHolder(
    override val walletCardState: WalletCardState,
    override val buttons: PersistentList<WalletManageButton>,
    override val bottomSheetConfig: TangemBottomSheetConfig?,
    onUnlockNotificationClick: () -> Unit,
) : WalletStateHolder {

    override val pullToRefreshConfig: PullToRefreshConfig
        get() = PullToRefreshConfig(isRefreshing = false, onRefresh = {})

    override val warnings: ImmutableList<WalletNotification> = persistentListOf(
        WalletNotification.UnlockWallets(onUnlockNotificationClick),
    )
}