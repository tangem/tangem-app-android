package com.tangem.managetokens.presentation.managetokens.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.WarningCardTitleOnly
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.managetokens.impl.R
import com.tangem.managetokens.presentation.common.state.ChooseWalletState
import com.tangem.managetokens.presentation.common.state.previewdata.ChooseWalletStatePreviewData
import com.tangem.managetokens.presentation.common.ui.components.NetworkItem
import com.tangem.managetokens.presentation.common.ui.components.SimpleSelectionBlock
import com.tangem.managetokens.presentation.managetokens.state.ChooseNetworkState
import com.tangem.managetokens.presentation.managetokens.state.TokenItemState
import com.tangem.managetokens.presentation.managetokens.state.previewdata.TokenItemStatePreviewData

@Composable
internal fun ChooseNetworkScreen(
    state: TokenItemState.Loaded,
    walletState: ChooseWalletState,
    modifier: Modifier = Modifier,
) {
    val networkState = state.chooseNetworkState
    LazyColumn(
        contentPadding = PaddingValues(
            start = TangemTheme.dimens.spacing16,
            end = TangemTheme.dimens.spacing16,
            bottom = TangemTheme.dimens.spacing16,
        ),
        modifier = modifier
            .background(TangemTheme.colors.background.tertiary),
    ) {
        item {
            Text(
                text = stringResource(id = R.string.manage_tokens_network_selector_title),
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.subtitle1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth(),
            )
        }

        when (walletState) {
            is ChooseWalletState.Choose -> {
                item {
                    SpacerH(height = TangemTheme.dimens.spacing10)
                }
                item {
                    SimpleSelectionBlock(
                        title = stringResource(id = R.string.manage_tokens_network_selector_wallet),
                        subtitle = walletState.selectedWallet?.walletName ?: "",
                        onClick = walletState.onChooseWalletClick,
                    )
                }
            }
            ChooseWalletState.NoSelection -> Unit
            is ChooseWalletState.Warning -> {
                item {
                    SpacerH(height = TangemTheme.dimens.spacing10)
                }
                item {
                    WarningCardTitleOnly(
                        title = stringResource(id = R.string.manage_tokens_wallet_support_only_one_network_title),
                    )
                }
            }
        }

        item {
            SpacerH(height = TangemTheme.dimens.spacing16)
        }

        if (networkState.nativeNetworks.isNotEmpty()) {
            this@LazyColumn.nativeNetworks(networkState = networkState, tokenState = state)
        }

        if (networkState.nonNativeNetworks.isNotEmpty()) {
            this@LazyColumn.nonNativeNetworks(networkState = networkState, tokenState = state)
        }
    }
}

private fun LazyListScope.nativeNetworks(networkState: ChooseNetworkState, tokenState: TokenItemState.Loaded) {
    item {
        Text(
            text = stringResource(id = R.string.manage_tokens_network_selector_native_title),
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.caption1,
        )
        SpacerH(height = TangemTheme.dimens.spacing2)
        Text(
            text = stringResource(id = R.string.manage_tokens_network_selector_native_subtitle),
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.caption2,
        )
        SpacerH(height = TangemTheme.dimens.spacing8)
    }

    items(
        count = networkState.nativeNetworks.count(),
        key = { index -> networkState.nativeNetworks[index].id },
    ) { index ->
        NetworkItem(
            state = networkState.nativeNetworks[index],
            tokenState = tokenState,
            modifier = Modifier
                .roundedShapeItemDecoration(
                    currentIndex = index,
                    lastIndex = networkState.nativeNetworks.lastIndex,
                    addDefaultPadding = false,
                ),
        )
    }

    item {
        SpacerH(height = TangemTheme.dimens.spacing16)
    }
}

private fun LazyListScope.nonNativeNetworks(networkState: ChooseNetworkState, tokenState: TokenItemState.Loaded) {
    item {
        NonNativeNetworksHeader(networkState.onNonNativeNetworkHintClick)
        SpacerH(height = TangemTheme.dimens.spacing8)
    }

    items(
        count = networkState.nonNativeNetworks.count(),
        key = { index -> networkState.nonNativeNetworks[index].id },
    ) { index ->
        NetworkItem(
            state = networkState.nonNativeNetworks[index],
            tokenState = tokenState,
            modifier = Modifier
                .roundedShapeItemDecoration(
                    currentIndex = index,
                    lastIndex = networkState.nonNativeNetworks.lastIndex,
                    addDefaultPadding = false,
                ),
        )
    }

    item {
        SpacerH(height = TangemTheme.dimens.spacing16)
    }
}

@Composable
private fun NonNativeNetworksHeader(onNonNativeNetworkHintClick: () -> Unit) {
    Column {
        Row {
            Text(
                text = stringResource(id = R.string.manage_tokens_network_selector_non_native_title),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.caption1,
            )
            SpacerW(width = TangemTheme.dimens.spacing2)
            Icon(
                painter = painterResource(id = R.drawable.ic_information_24),
                tint = TangemTheme.colors.icon.inactive,
                contentDescription = null,
                modifier = Modifier
                    .size(TangemTheme.dimens.size16)
                    .clickable { onNonNativeNetworkHintClick() },
            )
        }
        SpacerH(height = TangemTheme.dimens.spacing2)
        Text(
            text = stringResource(id = R.string.manage_tokens_network_selector_non_native_subtitle),
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.caption2,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ChooseNetworkScreen() {
    TangemThemePreview {
        ChooseNetworkScreen(
            state = TokenItemStatePreviewData.loadedPriceDown as TokenItemState.Loaded,
            walletState = ChooseWalletStatePreviewData.state,
        )
    }
}
