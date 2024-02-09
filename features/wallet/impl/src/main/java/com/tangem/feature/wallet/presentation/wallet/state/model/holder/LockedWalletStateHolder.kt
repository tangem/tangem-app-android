package com.tangem.feature.wallet.presentation.wallet.state.model.holder

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletBottomSheetConfig
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletPullToRefreshConfig
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal interface WalletStateHolder {

    val pullToRefreshConfig: WalletPullToRefreshConfig
    val walletCardState: WalletCardState
    val warnings: ImmutableList<WalletNotification>
    val bottomSheetConfig: TangemBottomSheetConfig?
}

internal class LockedWalletStateHolder(
    override val walletCardState: WalletCardState,
    onUnlockNotificationClick: () -> Unit,
    isBottomSheetShow: Boolean,
    onBottomSheetDismiss: () -> Unit,
    onUnlockClick: () -> Unit,
    onScanClick: () -> Unit,
) : WalletStateHolder {

    override val pullToRefreshConfig: WalletPullToRefreshConfig
        get() = WalletPullToRefreshConfig(isRefreshing = false, onRefresh = {})

    override val warnings: ImmutableList<WalletNotification> = persistentListOf(
        WalletNotification.UnlockWallets(onUnlockNotificationClick),
    )

    override val bottomSheetConfig = TangemBottomSheetConfig(
        isShow = isBottomSheetShow,
        onDismissRequest = onBottomSheetDismiss,
        content = WalletBottomSheetConfig.UnlockWallets(
            onUnlockClick = onUnlockClick,
            onScanClick = onScanClick,
        ),
    )
}