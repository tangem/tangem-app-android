package com.tangem.features.walletconnect.transaction.ui.chain

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.divider.DividerWithPadding
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.walletconnect.connections.ui.WcAppInfoItem
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.chain.WcAddEthereumChainItemUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionAppInfoContentUM
import com.tangem.features.walletconnect.transaction.ui.common.*

@Composable
internal fun WcAddEthereumChainModalBottomSheetContent(
    state: WcAddEthereumChainItemUM,
    onClickTransactionRequest: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = TangemTheme.dimens.spacing16),
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(TangemTheme.dimens.radius14))
                .background(color = TangemTheme.colors.background.action)
                .fillMaxWidth()
                .animateContentSize(),
        ) {
            WcSmallTitleItem(R.string.wc_request_from)
            WcAppInfoItem(
                iconUrl = state.appInfo.appIcon,
                title = state.appInfo.appName,
                subtitle = state.appInfo.appSubtitle,
                isVerified = state.appInfo.isVerified,
            )
            DividerWithPadding(start = 0.dp, end = 0.dp)
            WcTransactionRequestItem(
                iconRes = R.drawable.ic_doc_new_24,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClickTransactionRequest() }
                    .padding(TangemTheme.dimens.spacing12),
            )
        }
        Column(modifier = Modifier.padding(top = TangemTheme.dimens.spacing16)) {
            WcAddEthereumChainItems(state)
            WcTransactionRequestButtons(
                modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing16),
                onDismiss = state.onDismiss,
                onClickActiveButton = state.onSign,
                activeButtonText = resourceReference(R.string.common_sign),
                isLoading = state.isLoading,
            )
        }
    }
}

@Composable
private fun WcAddEthereumChainItems(state: WcAddEthereumChainItemUM) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(TangemTheme.dimens.radius14))
            .background(color = TangemTheme.colors.background.action)
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        DividerWithPadding(start = 0.dp, end = 0.dp)
        WcWalletItem(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TangemTheme.dimens.spacing12),
            walletName = state.walletName,
        )
        DividerWithPadding(start = TangemTheme.dimens.spacing40, end = TangemTheme.dimens.spacing12)
    }
}

@Composable
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun WcAddEthereumChainBottomSheetPreview(
    @PreviewParameter(WcAddEthereumChainStateProvider::class) state: WcAddEthereumChainItemUM,
) {
    TangemThemePreview {
        TangemModalBottomSheet<WcAddEthereumChainItemUM>(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = state,
            ),
            containerColor = TangemTheme.colors.background.tertiary,
            title = {
                TangemModalBottomSheetTitle(
                    title = resourceReference(R.string.wallet_connect_title),
                    endIconRes = R.drawable.ic_close_24,
                    onEndClick = {},
                    startIconRes = null,
                    onStartClick = {},
                )
            },
            content = {
                WcAddEthereumChainModalBottomSheetContent(state = state, onClickTransactionRequest = {})
            },
        )
    }
}

private class WcAddEthereumChainStateProvider : CollectionPreviewParameterProvider<WcAddEthereumChainItemUM>(
    listOf(
        WcAddEthereumChainItemUM(
            onDismiss = {},
            onSign = {},
            appInfo = WcTransactionAppInfoContentUM(
                appName = "React App",
                appIcon = "",
                isVerified = true,
                appSubtitle = "react-app.walletconnect.com",
            ),
            walletName = "Tangem 2.0",
        ),
    ),
)