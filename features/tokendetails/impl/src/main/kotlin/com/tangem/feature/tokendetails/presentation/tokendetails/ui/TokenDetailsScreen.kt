package com.tangem.feature.tokendetails.presentation.tokendetails.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.compose.collectAsLazyPagingItems
import com.tangem.core.ui.components.marketprice.MarketPriceBlock
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.components.transactions.txHistoryItems
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tokendetails.presentation.tokendetails.TokenDetailsPreviewData
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.TokenDetailsBalanceBlock
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.TokenDetailsTopAppBar
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.TokenInfoBlock

@Composable
internal fun TokenDetailsScreen(state: TokenDetailsState) {
    Scaffold(
        topBar = { TokenDetailsTopAppBar(config = state.topAppBarConfig) },
        containerColor = TangemTheme.colors.background.secondary,
    ) { scaffoldPaddings ->
        val txHistoryItems = if (state.txHistoryState is TxHistoryState.Content) {
            state.txHistoryState.contentItems.collectAsLazyPagingItems()
        } else {
            null
        }
        val betweenItemsPadding = TangemTheme.dimens.spacing12
        val horizontalPadding = TangemTheme.dimens.spacing16
        val itemModifier = Modifier
            .padding(top = betweenItemsPadding)
            .padding(horizontal = horizontalPadding)
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues = scaffoldPaddings)
                .fillMaxSize(),
        ) {
            item {
                TokenInfoBlock(
                    modifier = Modifier
                        .padding(top = TangemTheme.dimens.spacing4)
                        .padding(horizontal = horizontalPadding),
                    state = state.tokenInfoBlockState,
                )
            }
            item { TokenDetailsBalanceBlock(modifier = itemModifier, state = state.tokenBalanceBlockState) }
            item(
                key = MarketPriceBlockState::class.java,
                contentType = MarketPriceBlockState::class.java,
                content = { MarketPriceBlock(modifier = itemModifier, state = state.marketPriceBlockState) },
            )
            txHistoryItems(state = state.txHistoryState, txHistoryItems = txHistoryItems)
        }
    }
}

@Preview
@Composable
private fun Preview_TokenDetailsScreen_LightTheme() {
    TangemTheme(isDark = false) {
        TokenDetailsScreen(state = TokenDetailsPreviewData.tokenDetailsState)
    }
}

@Preview
@Composable
private fun Preview_TokenDetailsScreen_DarkTheme() {
    TangemTheme(isDark = true) {
        TokenDetailsScreen(state = TokenDetailsPreviewData.tokenDetailsState)
    }
}