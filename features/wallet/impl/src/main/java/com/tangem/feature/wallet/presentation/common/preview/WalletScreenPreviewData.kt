package com.tangem.feature.wallet.presentation.common.preview

import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.internal.TangemRowTailUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.WalletPreviewDataLegacy.topBarConfig
import com.tangem.feature.wallet.presentation.preview.WalletBalancePreview
import com.tangem.feature.wallet.presentation.preview.WalletPreviewData
import com.tangem.feature.wallet.presentation.wallet.state.model.*
import kotlinx.collections.immutable.persistentListOf

internal object WalletScreenPreviewData {

    private val tokenRowDefault = TangemTokenRowUM.Content(
        id = "1",
        headIconUM = TangemIconUM.Currency(CurrencyIconState.Loading),
        titleUM = TangemTokenRowUM.TitleUM.Content(
            text = stringReference("Bitcoin"),
        ),
        subtitleUM = TangemTokenRowUM.SubtitleUM.Content(
            text = stringReference("Bitcoin"),
        ),
        topEndContentUM = TangemTokenRowUM.EndContentUM.Content(
            text = stringReference("1 234,56 \$"),
        ),
        bottomEndContentUM = TangemTokenRowUM.EndContentUM.Content(
            text = stringReference("0,12345678 BTC"),
        ),
        promoBannerUM = TangemTokenRowUM.PromoBannerUM.Empty,
        tailUM = TangemRowTailUM.Empty,
        onItemClick = {},
        onItemLongClick = {},
    )

    private val tokenListDefault = WalletTokensListUM.Content(
        tokenList = persistentListOf(
            TokensListItemUM2.Token(tokenRowDefault.copy(id = "0")),
            TokensListItemUM2.Token(tokenRowDefault.copy(id = "1")),
            TokensListItemUM2.Token(tokenRowDefault.copy(id = "2")),
        ),
        organizeButtonUM = TangemButtonUM(
            text = resourceReference(R.string.organize_tokens_title),
            type = TangemButtonType.Secondary,
            onClick = {},
        ),
    )

    private val walletLocked = WalletUM.Locked(
        walletsBalanceUM = WalletBalancePreview.content,
        buttons = WalletPreviewData.actionButtons,
        type = WalletType.Cold,
        notifications = persistentListOf(),
    )

    private val walletDefault = WalletUM.Content(
        walletsBalanceUM = WalletBalancePreview.content,
        buttons = WalletPreviewData.actionButtons,
        type = WalletType.Cold,
        pullToRefreshConfig = PullToRefreshConfig(
            isRefreshing = false,
            onRefresh = {},
        ),
        notifications = persistentListOf(),
        notificationsCarousel = persistentListOf(),
        tokensListUM = tokenListDefault,
        nftState = WalletNFTItemUM.Content(
            previews = persistentListOf(),
            collectionsCount = 0,
            allAssetsCount = 0,
            noCollectionAssetsCount = 0,
            isFlickering = false,
            onItemClick = {},
        ),
        tangemPayState = TangemPayState.Loading,
    )

    internal val defaultState = WalletScreenState(
        topBarConfig = topBarConfig,
        selectedWalletIndex = 0,
        wallets = persistentListOf(),
        wallets2 = persistentListOf(
            walletLocked,
            walletDefault,
        ),
        onWalletChange = { _, _ -> },
        event = consumedEvent(),
        isHidingMode = false,
        showMarketsOnboarding = false,
        onDismissMarketsTooltip = {},
        isRedesignEnabled = true,
    )
}