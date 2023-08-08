package com.tangem.feature.wallet.presentation.wallet.state

import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.components.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Loading wallet state
 *
 * @property onBackClick Lambda be invoked when back button is clicked
 *
[REDACTED_AUTHOR]
 */
internal data class WalletLoading(override val onBackClick: () -> Unit) : WalletStateHolder() {

    override val topBarConfig = WalletTopBarConfig(onScanCardClick = {}, onMoreClick = {})

    override val walletsListConfig = WalletsListConfig(
        selectedWalletIndex = 0,
        wallets = persistentListOf(
            WalletCardState.Loading(
                id = UserWalletId(stringValue = ""),
                title = "",
                additionalInfo = "",
                imageResId = null,
            ),
        ),
        onWalletChange = {},
    )

    override val pullToRefreshConfig = WalletPullToRefreshConfig(isRefreshing = false, onRefresh = {})

    override val notifications: ImmutableList<WalletNotification> = persistentListOf()

    override val bottomSheetConfig = null
}