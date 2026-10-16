package com.tangem.feature.wallet.presentation.wallet.state.model

import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.collectibles.api.walletblock.CollectiblesWalletBlockUM
import com.tangem.features.moneyaccount.main.entity.MoneyAccountMainUM
import com.tangem.features.polymarket.api.walletblock.PolymarketWalletBlockUM
import com.tangem.features.tangempay.entity.TangemPayMainUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal fun createWalletContentUM(
    userWalletId: UserWalletId = UserWalletId("01"),
    isRefreshing: Boolean = false,
    notifications: ImmutableList<WalletNotificationUM> = persistentListOf(),
    notificationsCarousel: ImmutableList<WalletNotificationUM> = persistentListOf(),
    isMarketingBlockVisible: Boolean = false,
    isTangemPayPromoVisible: Boolean = false,
    promoCardsCheck: PromoCardsCheck = PromoCardsCheck.OnOpen,
): WalletUM.Content = WalletUM.Content(
    pullToRefreshConfig = PullToRefreshConfig(isRefreshing = isRefreshing, onRefresh = PULL_TO_REFRESH_CALLBACK),
    walletsBalanceUM = WalletBalanceUM.Loading(id = userWalletId, name = "test", deviceIcon = DeviceIconUM.Mobile),
    buttons = persistentListOf(),
    notifications = notifications,
    notificationsCarousel = notificationsCarousel,
    tokensListUM = WalletTokensListUM.Loading,
    nftState = WalletNFTItemUM.Hidden,
    type = WalletType.Hot,
    tangemPayMainUM = TangemPayMainUM.Empty,
    moneyAccountMainUM = MoneyAccountMainUM.Empty,
    polymarketWalletBlockUM = PolymarketWalletBlockUM.Hidden,
    collectiblesWalletBlockUM = CollectiblesWalletBlockUM.Hidden,
    isMarketingBlockVisible = isMarketingBlockVisible,
    isTangemPayPromoVisible = isTangemPayPromoVisible,
    promoCardsCheck = promoCardsCheck,
)

/** Shared so that two states built with the same arguments are equal */
private val PULL_TO_REFRESH_CALLBACK: (PullToRefreshConfig.ShowRefreshState) -> Unit = {}