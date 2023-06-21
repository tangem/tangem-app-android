package com.tangem.feature.wallet.presentation.wallet.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.RoundedActionButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.common.component.NetworkGroupItem
import com.tangem.feature.wallet.presentation.common.component.TokenItem
import com.tangem.feature.wallet.presentation.wallet.state.WalletContentItemState
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import com.tangem.feature.wallet.presentation.wallet.ui.components.WalletCardsList
import com.tangem.feature.wallet.presentation.wallet.ui.components.WalletTopBar

/**
 * Wallet screen
 *
 * @param state screen state
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun WalletScreen(state: WalletStateHolder) {
    BackHandler(onBack = state.onBackClick)

    Scaffold(
        topBar = { WalletTopBar(config = state.topBarConfig) },
        containerColor = TangemTheme.colors.background.secondary,
    ) { scaffoldPaddings ->
        val lastContentItemIndex = remember(state.contentItems) {
            state.contentItems.lastIndex
        }

        LazyColumn(
            modifier = Modifier
                .padding(scaffoldPaddings)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { WalletCardsList(wallets = state.wallets) }

            itemsIndexed(
                items = state.contentItems,
                key = { _, item ->
                    when (item) {
                        is WalletContentItemState.NetworkGroupTitle -> item.networkName
                        is WalletContentItemState.Token -> item.tokenItemState.id
                    }
                },
            ) { index, item ->
                val itemModifier = Modifier
                    .padding(horizontal = TangemTheme.dimens.spacing16)
                    .clipFirstAndLastItems(index, lastContentItemIndex)

                when (item) {
                    is WalletContentItemState.NetworkGroupTitle -> {
                        NetworkGroupItem(
                            networkName = item.networkName,
                            modifier = itemModifier,
                        )
                    }
                    is WalletContentItemState.Token -> {
                        TokenItem(
                            state = item.tokenItemState,
                            modifier = itemModifier,
                        )
                    }
                }
            }

            item {
                RoundedActionButton(
                    text = stringResource(id = R.string.organize_tokens_title),
                    iconResId = R.drawable.ic_filter_24,
                    onClick = state.onOrganizeTokensClick,
                    modifier = Modifier.padding(top = TangemTheme.dimens.spacing14),
                )
            }
        }
    }
}

private fun Modifier.clipFirstAndLastItems(index: Int, lastItemIndex: Int): Modifier = composed {
    when (index) {
        0 -> {
            this
                .padding(top = TangemTheme.dimens.spacing14)
                .clip(
                    RoundedCornerShape(
                        topStart = TangemTheme.dimens.radius16,
                        topEnd = TangemTheme.dimens.radius16,
                    ),
                )
        }
        lastItemIndex -> {
            this
                .clip(
                    RoundedCornerShape(
                        bottomStart = TangemTheme.dimens.radius16,
                        bottomEnd = TangemTheme.dimens.radius16,
                    ),
                )
        }
        else -> this
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Composable
private fun WalletScreenPreview_Light(
    @PreviewParameter(WalletScreenParameterProvider::class) state: WalletStateHolder,
) {
    TangemTheme {
        WalletScreen(state)
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun WalletScreenPreview_Dark(
    @PreviewParameter(WalletScreenParameterProvider::class) state: WalletStateHolder,
) {
    TangemTheme(isDark = true) {
        WalletScreen(state)
    }
}

private class WalletScreenParameterProvider : CollectionPreviewParameterProvider<WalletStateHolder>(
    collection = listOf(WalletPreviewData.groupedWalletScreenState),
)
// endregion Preview