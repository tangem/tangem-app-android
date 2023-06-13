package com.tangem.feature.wallet.presentation.common

import com.tangem.core.ui.R
import com.tangem.feature.wallet.presentation.common.state.NetworkGroupState
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.common.state.TokenItemState.TokenOptionsState
import com.tangem.feature.wallet.presentation.common.state.TokenListState
import com.tangem.feature.wallet.presentation.organizetokens.OrganizeTokensStateHolder
import com.tangem.feature.wallet.presentation.wallet.state.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID

internal object WalletPreviewData {

    val walletCardContent = WalletCardState.Content(
        id = UUID.randomUUID().toString(),
        title = "Wallet 1",
        balance = "8923,05 $",
        additionalInfo = "3 cards • Seed enabled",
        imageResId = R.drawable.ill_businessman_3d,
        onClick = {},
    )

    val walletCardLoading = WalletCardState.Loading(
        id = UUID.randomUUID().toString(),
        title = "Wallet 1",
        additionalInfo = "3 cards • Seed enabled",
        imageResId = R.drawable.ill_businessman_3d,
        onClick = {},
    )

    val walletCardHiddenContent = WalletCardState.HiddenContent(
        id = UUID.randomUUID().toString(),
        title = "Wallet 1",
        additionalInfo = "3 cards • Seed enabled",
        imageResId = R.drawable.ill_businessman_3d,
        onClick = {},
    )

    val walletCardError = WalletCardState.Error(
        id = UUID.randomUUID().toString(),
        title = "Wallet 1",
        additionalInfo = "3 cards • Seed enabled",
        imageResId = R.drawable.ill_businessman_3d,
        onClick = {},
    )

    val walletScreenState = WalletStateHolder(
        onBackClick = {},
        headerConfig = WalletStateHolder.HeaderConfig(
            wallets = persistentListOf(walletCardContent, walletCardLoading, walletCardHiddenContent, walletCardError),
            onScanCardClick = {},
            onMoreClick = {},
        ),
    )

    val tokenItemVisibleState = TokenItemState.Content(
        id = UUID.randomUUID().toString(),
        tokenIconUrl = null,
        tokenIconResId = R.drawable.img_polygon_22,
        networkIconResId = R.drawable.img_polygon_22,
        name = "Polygon",
        amount = "5,412 MATIC",
        hasPending = true,
        tokenOptions = TokenOptionsState.Visible(
            fiatAmount = "321 $",
            priceChange = TokenOptionsState.PriceChange(
                valuePercent = "2%",
                type = TokenOptionsState.PriceChange.Type.UP,
            ),
        ),
    )

    val tokenItemHiddenState = TokenItemState.Content(
        id = UUID.randomUUID().toString(),
        tokenIconUrl = null,
        tokenIconResId = R.drawable.img_polygon_22,
        networkIconResId = R.drawable.img_polygon_22,
        name = "Polygon",
        amount = "5,412 MATIC",
        hasPending = true,
        tokenOptions = TokenOptionsState.Hidden(
            priceChange = TokenOptionsState.PriceChange(
                valuePercent = "2%",
                type = TokenOptionsState.PriceChange.Type.UP,
            ),
        ),
    )

    val tokenItemDragState = TokenItemState.Draggable(
        id = UUID.randomUUID().toString(),
        tokenIconUrl = null,
        tokenIconResId = R.drawable.img_polygon_22,
        networkIconResId = R.drawable.img_polygon_22,
        name = "Polygon",
        fiatAmount = "3 172,14 $",
    )

    val tokenItemUnreachableState = TokenItemState.Unreachable(
        id = UUID.randomUUID().toString(),
        tokenIconUrl = null,
        tokenIconResId = R.drawable.img_polygon_22,
        networkIconResId = R.drawable.img_polygon_22,
        name = "Polygon",
    )

    val loadingTokenItemState = TokenItemState.Loading(id = UUID.randomUUID().toString())

    val organizeTokensState = OrganizeTokensStateHolder(
        tokens = TokenListState.GroupedByNetwork(
            groups = persistentListOf(),
        ),
        header = OrganizeTokensStateHolder.HeaderConfig(
            onSortByBalanceClick = {},
            onGroupByNetworkClick = {},
        ),
        actions = OrganizeTokensStateHolder.ActionsConfig(
            onApplyClick = {},
            onCancelClick = {},
        ),
    )

    private val draggableTokenList = persistentListOf(
        tokenItemDragState.copy(
            id = "token_1",
            name = "Ethereum",
            tokenIconResId = R.drawable.img_eth_22,
            networkIconResId = null,
            fiatAmount = "3 172,14 $",
        ),
        tokenItemDragState.copy(
            id = "token_2",
            networkIconResId = R.drawable.img_eth_22,
            fiatAmount = "803,65 $",
        ),
        tokenItemDragState.copy(
            id = "token_3",
            name = "USDT",
            tokenIconResId = R.drawable.img_arbitrum_22,
            networkIconResId = R.drawable.img_eth_22,
            fiatAmount = "88,01 $",
        ),
    )

    val networkGroup = NetworkGroupState.Content(
        id = UUID.randomUUID().toString(),
        networkName = "Ethereum",
        tokens = persistentListOf(
            tokenItemVisibleState.copy(
                id = "token_1",
                name = "Ethereum",
                tokenIconResId = R.drawable.img_eth_22,
                networkIconResId = null,
                amount = "1,89340821 ETH",
            ),
            tokenItemVisibleState.copy(
                id = "token_2",
                networkIconResId = R.drawable.img_eth_22,
                amount = "733,71097 MATIC",
            ),
            tokenItemVisibleState.copy(
                id = "token_3",
                name = "USDT",
                tokenIconResId = R.drawable.img_arbitrum_22,
                networkIconResId = R.drawable.img_eth_22,
                amount = "0,25404523 ARB",
            ),
        ),
    )

    val draggableNetworkGroup = NetworkGroupState.Draggable(
        id = UUID.randomUUID().toString(),
        networkName = "Ethereum",
        tokens = draggableTokenList,
    )
}