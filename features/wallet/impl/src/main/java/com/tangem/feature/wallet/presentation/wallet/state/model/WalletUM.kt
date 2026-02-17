package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.ds.button.TangemButtonUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal sealed interface WalletUM {

    val pullToRefreshConfig: PullToRefreshConfig
    val walletsBalanceUM: WalletBalanceUM

    val buttons: PersistentList<TangemButtonUM>
    val notifications: ImmutableList<WalletNotificationUM>
    val notificationsCarousel: ImmutableList<WalletNotificationUM>

    val tokensListUM: WalletTokensListUM

    val nftState: WalletNFTItemUM

    val type: WalletType

    val tangemPayState: TangemPayState

    data class Content(
        override val pullToRefreshConfig: PullToRefreshConfig,
        override val walletsBalanceUM: WalletBalanceUM,
        override val buttons: PersistentList<TangemButtonUM>,
        override val notifications: ImmutableList<WalletNotificationUM>,
        override val notificationsCarousel: ImmutableList<WalletNotificationUM>,
        override val tokensListUM: WalletTokensListUM,
        override val nftState: WalletNFTItemUM,
        override val type: WalletType,
        override val tangemPayState: TangemPayState,
    ) : WalletUM

    data class Locked(
        override val walletsBalanceUM: WalletBalanceUM,
        override val buttons: PersistentList<TangemButtonUM>,
        override val type: WalletType,
        override val notifications: ImmutableList<WalletNotificationUM> = persistentListOf(),
    ) : WalletUM {
        override val notificationsCarousel: ImmutableList<WalletNotificationUM> = persistentListOf()
        override val pullToRefreshConfig = PullToRefreshConfig(false, {})
        override val tokensListUM: WalletTokensListUM = WalletTokensListUM.Empty // todo redesign main locked state
        override val nftState: WalletNFTItemUM = WalletNFTItemUM.Hidden
        override val tangemPayState: TangemPayState = TangemPayState.Empty
    }
}