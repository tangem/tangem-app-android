package com.tangem.features.walletconnect.transaction.ui.send

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
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetWithFooter
import com.tangem.core.ui.components.divider.DividerWithPadding
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.send.v2.api.FeeSelectorBlockComponent
import com.tangem.features.walletconnect.connections.entity.VerifiedDAppState
import com.tangem.features.walletconnect.connections.ui.WcAppInfoItem
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.components.PreviewFeeSelectorBlockComponent
import com.tangem.features.walletconnect.transaction.entity.blockaid.WcEstimatedWalletChangeUM
import com.tangem.features.walletconnect.transaction.entity.blockaid.WcEstimatedWalletChangesUM
import com.tangem.features.walletconnect.transaction.entity.blockaid.WcSendReceiveTransactionCheckResultsUM
import com.tangem.features.walletconnect.transaction.entity.common.WcNetworkInfoUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionAppInfoContentUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionFeeState
import com.tangem.features.walletconnect.transaction.entity.send.WcSendTransactionItemUM
import com.tangem.features.walletconnect.transaction.ui.blockaid.TransactionCheckResultsItem
import com.tangem.features.walletconnect.transaction.ui.common.WcSendTransactionItems
import com.tangem.features.walletconnect.transaction.ui.common.WcSmallTitleItem
import com.tangem.features.walletconnect.transaction.ui.common.WcTransactionRequestButtons
import com.tangem.features.walletconnect.transaction.ui.common.WcTransactionRequestItem
import kotlinx.collections.immutable.persistentListOf

@Suppress("LongParameterList", "LongMethod")
@Composable
internal fun WcSendTransactionModalBottomSheet(
    state: WcSendTransactionItemUM,
    feeSelectorBlockComponent: FeeSelectorBlockComponent?,
    onClickTransactionRequest: () -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onClickAllowToSpend: () -> Unit,
) {
    TangemModalBottomSheetWithFooter<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = onBack,
        containerColor = TangemTheme.colors.background.tertiary,
        title = { config ->
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
                    if (state.estimatedWalletChanges != null) {
                        TransactionCheckResultsItem(state.estimatedWalletChanges, onClickAllowToSpend)
                    }
                    Spacer(Modifier.height(16.dp))
                    WcSendTransactionItems(
                        walletName = state.walletName,
                        networkInfo = state.networkInfo,
                        feeState = state.feeState,
                        feeSelectorBlockComponent = feeSelectorBlockComponent,
                        address = state.address,
                    )
                }
            }
        },
        footer = {
            WcTransactionRequestButtons(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                onDismiss = state.onDismiss,
                onClickActiveButton = state.onSend,
                activeButtonText = resourceReference(R.string.common_send),
                isLoading = state.isLoading,
            )
        },
    )
}

@Composable
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun WcSendTransactionBottomSheetPreview(
    @PreviewParameter(WcSendTransactionStateProvider::class) state: WcSendTransactionItemUM,
) {
    TangemThemePreview {
        TangemModalBottomSheet<WcSendTransactionItemUM>(
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
                WcSendTransactionModalBottomSheet(
                    state = state,
                    feeSelectorBlockComponent = PreviewFeeSelectorBlockComponent(),
                    onClickTransactionRequest = {},
                    onBack = {},
                    onDismiss = {},
                    onClickAllowToSpend = {},
                )
            },
        )
    }
}

private class WcSendTransactionStateProvider : CollectionPreviewParameterProvider<WcSendTransactionItemUM>(
    listOf(
        WcSendTransactionItemUM(
            onDismiss = {},
            onSend = {},
            appInfo = WcTransactionAppInfoContentUM(
                appName = "React App",
                appIcon = "",
                verifiedState = VerifiedDAppState.Verified {},
                appSubtitle = "react-app.walletconnect.com",
            ),
            estimatedWalletChanges = WcSendReceiveTransactionCheckResultsUM(
                notificationText = TextReference.Str(
                    "The transaction approves erc20 tokens to a known malicious address",
                ),
                estimatedWalletChanges = WcEstimatedWalletChangesUM(
                    items = persistentListOf(
                        WcEstimatedWalletChangeUM(
                            iconRes = R.drawable.ic_send_new_24,
                            title = resourceReference(R.string.common_send),
                            description = "- 42 USDT",
                            tokenIconUrl = "https://tangem.com",
                        ),
                        WcEstimatedWalletChangeUM(
                            iconRes = R.drawable.ic_receive_new_24,
                            title = resourceReference(R.string.common_receive),
                            description = "+ 1,131.46 MATIC",
                            tokenIconUrl = "https://tangem.com",
                        ),
                    ),
                ),
            ),
            walletName = "Tangem 2.0",
            networkInfo = WcNetworkInfoUM(name = "Ethereum", iconRes = R.drawable.img_eth_22),
            feeState = WcTransactionFeeState.Success(null, {}),
            address = "0x345FF...34FA",
        ),
    ),
)