package com.tangem.managetokens.presentation.managetokens.state.previewdata

import androidx.compose.ui.graphics.Color
import com.tangem.managetokens.presentation.managetokens.state.*
import kotlinx.collections.immutable.persistentListOf

internal object TokenItemStatePreviewData {

    val tokenLoading: TokenItemState
        get() = TokenItemState.Loading("id")

    val loadedPriceDown: TokenItemState
        get() = TokenItemState.Loaded(
            id = "BTC",
            name = "Bitcoin Bitcoin Bitcoin Bitcoin Bitcoin Bitcoin Bitcoin Bitcoin",
            currencyId = "BTC",
            tokenIcon = tokenIconState,
            quotes = QuotesState.Content(
                priceChange = "0.43%",
                changeType = PriceChangeType.DOWN,
                chartData = persistentListOf(10f, 2f, 5f, 3f, 4f, 8f, 9f, 7f, 4f),
            ),
            rate = "31 285.72$",
            availableAction = TokenButtonType.ADD,
            onButtonClick = {},
        )

    val loadedPriceUp: TokenItemState
        get() = TokenItemState.Loaded(
            id = "BTC",
            name = "Bitcoin",
            currencyId = "BTC",
            tokenIcon = tokenIconState,
            quotes = QuotesState.Content(
                priceChange = "0.43%",
                changeType = PriceChangeType.UP,
                chartData = persistentListOf(1f, 3f, 4f, 8f, 12f, 10f, 8f, 3f, 5f, 7f),
            ),
            rate = "31 285.72$",
            availableAction = TokenButtonType.NOT_AVAILABLE,
            onButtonClick = {},
        )

    private val tokenIconState: TokenIconState
        get() = TokenIconState(
            iconReference = null,
            placeholderTint = Color.White,
            placeholderBackground = Color.Black,
        )
}
