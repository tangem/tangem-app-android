package com.tangem.feature.wallet.presentation.wallet.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.components.RoundedActionButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.impl.R
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
 * @author Andrew Khokhlov on 29/05/2023
 */
@Composable
internal fun WalletScreen(state: WalletStateHolder) {
    BackHandler(onBack = state.onBackClick)

    Scaffold(
        topBar = { WalletTopBar(config = state.topBarConfig) },
        containerColor = TangemTheme.colors.background.secondary,
    ) { scaffoldPaddings ->
        LazyColumn(
            modifier = Modifier
                .padding(scaffoldPaddings)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { WalletCardsList(wallets = state.wallets) }

            items(
                count = state.contentItems.size,
                key = {
                    when (val item = state.contentItems[it]) {
                        is WalletContentItemState.NetworkGroupTitle -> item.text
                        is WalletContentItemState.Token -> item.tokenItemState.id
                    }
                },
            ) { index: Int ->
                if (index == 0) NetworkGroupHeader()

                when (val item = state.contentItems[index]) {
                    is WalletContentItemState.NetworkGroupTitle -> {
                        Text(
                            text = stringResource(id = R.string.wallet_network_group_title, item.text),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = TangemTheme.dimens.spacing16)
                                .background(color = TangemTheme.colors.background.primary)
                                .padding(
                                    start = TangemTheme.dimens.spacing14,
                                    top = if (index != 0) TangemTheme.dimens.spacing12 else TangemTheme.dimens.size0,
                                    end = TangemTheme.dimens.spacing14,
                                    bottom = TangemTheme.dimens.spacing12,
                                ),
                            color = TangemTheme.colors.text.tertiary,
                            style = TangemTheme.typography.subtitle2,
                        )
                    }
                    is WalletContentItemState.Token -> {
                        TokenItem(
                            state = item.tokenItemState,
                            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16),
                        )
                    }
                }

                if (index == state.contentItems.size - 1) NetworkGroupFooter()
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

@Composable
private fun NetworkGroupHeader() {
    Box(
        modifier = Modifier
            .padding(top = TangemTheme.dimens.spacing14)
            .padding(horizontal = TangemTheme.dimens.spacing16)
            .fillMaxWidth()
            .height(TangemTheme.dimens.spacing12)
            .background(
                color = TangemTheme.colors.background.primary,
                shape = RoundedCornerShape(
                    topStart = TangemTheme.dimens.radius16,
                    topEnd = TangemTheme.dimens.radius16,
                ),
            ),
    )
}

@Composable
private fun NetworkGroupFooter() {
    Box(
        modifier = Modifier
            .padding(horizontal = TangemTheme.dimens.spacing16)
            .fillMaxWidth()
            .height(TangemTheme.dimens.spacing12)
            .background(
                color = TangemTheme.colors.background.primary,
                shape = RoundedCornerShape(
                    bottomStart = TangemTheme.dimens.radius16,
                    bottomEnd = TangemTheme.dimens.radius16,
                ),
            ),
    )
}
