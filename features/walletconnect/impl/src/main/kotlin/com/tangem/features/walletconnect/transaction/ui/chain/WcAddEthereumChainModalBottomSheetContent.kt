package com.tangem.features.walletconnect.transaction.ui.chain

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetWithFooter
import com.tangem.core.ui.components.divider.DividerWithPadding
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.walletconnect.connections.entity.VerifiedDAppState
import com.tangem.features.walletconnect.connections.ui.WcAppInfoItem
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.chain.WcAddEthereumChainItemUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionAppInfoContentUM
import com.tangem.features.walletconnect.transaction.ui.common.WcSmallTitleItem
import com.tangem.features.walletconnect.transaction.ui.common.WcTransactionRequestButtons
import com.tangem.features.walletconnect.transaction.ui.common.WcTransactionRequestItem
import com.tangem.features.walletconnect.transaction.ui.common.WcWalletItem

@Composable
internal fun WcAddEthereumChainModalBottomSheetContent(
    state: WcAddEthereumChainItemUM,
    onClickTransactionRequest: () -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    TangemModalBottomSheetWithFooter<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        containerColor = TangemTheme.colors.background.tertiary,
        onBack = onBack,
        title = {
            TangemModalBottomSheetTitle(
                title = resourceReference(R.string.wc_wallet_connect),
                endIconRes = R.drawable.ic_close_24,
                onEndClick = onDismiss,
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(color = TangemTheme.colors.background.action)
                        .fillMaxWidth()
                        .animateContentSize(),
                ) {
                    WcSmallTitleItem(R.string.wc_request_from)
                    WcAppInfoItem(
                        iconUrl = state.appInfo.appIcon,
                        title = state.appInfo.appName,
                        subtitle = state.appInfo.appSubtitle,
                        verifiedDAppState = state.appInfo.verifiedState,
                    )
                    DividerWithPadding(start = 0.dp, end = 0.dp)
                    WcTransactionRequestItem(
                        iconRes = R.drawable.ic_doc_new_24,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClickTransactionRequest() }
                            .padding(12.dp),
                    )
                }
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    WcAddEthereumChainItems(state)
                }
            }
        },
        footer = {
            WcTransactionRequestButtons(
                modifier = Modifier.padding(16.dp),
                onDismiss = state.onDismiss,
                onClickActiveButton = state.onSign,
                activeButtonText = resourceReference(R.string.common_sign),
                isLoading = state.isLoading,
            )
        },
    )
}

@Composable
private fun WcAddEthereumChainItems(state: WcAddEthereumChainItemUM) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color = TangemTheme.colors.background.action)
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        DividerWithPadding(start = 0.dp, end = 0.dp)
        WcWalletItem(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            walletName = state.walletName,
        )
        DividerWithPadding(start = 40.dp, end = 12.dp)
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
                WcAddEthereumChainModalBottomSheetContent(state, {}, {}, {})
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
                verifiedState = VerifiedDAppState.Verified {},
                appSubtitle = "react-app.walletconnect.com",
            ),
            walletName = "Tangem 2.0",
        ),
    ),
)