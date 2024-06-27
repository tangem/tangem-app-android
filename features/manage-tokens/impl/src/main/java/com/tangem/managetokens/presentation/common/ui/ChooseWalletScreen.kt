package com.tangem.managetokens.presentation.common.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerW12
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.managetokens.impl.R
import com.tangem.managetokens.presentation.common.state.ChooseWalletState
import com.tangem.managetokens.presentation.common.state.WalletState
import com.tangem.managetokens.presentation.common.state.previewdata.ChooseWalletStatePreviewData

@Composable
internal fun ChooseWalletScreen(state: ChooseWalletState.Choose, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .background(TangemTheme.colors.background.tertiary)
            .padding(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing16,
            ),
    ) {
        item {
            Box(
                modifier = Modifier.defaultMinSize(minHeight = TangemTheme.dimens.size44),
            ) {
                IconButton(
                    onClick = state.onCloseChoosingWalletClick,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clickable { state.onCloseChoosingWalletClick() },
                ) {
                    Icon(
                        painterResource(id = R.drawable.ic_back_24),
                        contentDescription = null,
                        tint = TangemTheme.colors.icon.primary1,
                    )
                }
                Text(
                    text = stringResource(id = R.string.manage_tokens_wallet_selector_title),
                    color = TangemTheme.colors.text.primary1,
                    style = TangemTheme.typography.subtitle1,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                )
            }
        }
        items(
            count = state.wallets.count(),
            key = { index -> state.wallets[index].walletId },
        ) { index ->
            WalletItem(
                wallet = state.wallets[index],
                selectedWallet = state.selectedWallet,
                modifier = Modifier
                    .roundedShapeItemDecoration(
                        currentIndex = index,
                        lastIndex = state.wallets.lastIndex,
                        addDefaultPadding = false,
                    ),
            )
        }
        item {
            SpacerH(height = TangemTheme.dimens.spacing16)
        }
    }
}

@Composable
private fun WalletItem(wallet: WalletState, selectedWallet: WalletState?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clickable { wallet.onSelected(wallet.walletId) }
            .background(TangemTheme.colors.background.action)
            .defaultMinSize(minHeight = TangemTheme.dimens.size72)
            .padding(horizontal = TangemTheme.dimens.spacing16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SubcomposeAsyncImage(
            modifier = Modifier.size(height = TangemTheme.dimens.size30, width = TangemTheme.dimens.size50),

            model = ImageRequest.Builder(context = LocalContext.current)
                .data(wallet.artworkUrl)
                .crossfade(enable = true)
                .build(),
            loading = {
                Image(
                    painter = painterResource(R.drawable.card_placeholder_black),
                    contentDescription = null,
                )
            },
            error = {
                Image(
                    painter = painterResource(R.drawable.card_placeholder_black),
                    contentDescription = null,
                )
            },
            contentDescription = null,
        )
        SpacerW12()
        Text(
            text = wallet.walletName,
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
        )
        SpacerWMax()
        if (selectedWallet == wallet) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.accent,
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ChooseWalletScreen() {
    TangemThemePreview {
        ChooseWalletScreen(
            state = ChooseWalletStatePreviewData.state,
        )
    }
}
