package com.tangem.features.walletconnect.transaction.ui.approve

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.approve.SpendAllowanceUM
import com.tangem.features.walletconnect.transaction.entity.approve.WcApproveTransactionUM
import com.tangem.features.walletconnect.transaction.entity.common.WcNetworkInfoUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionActionsUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionUM
import com.tangem.features.walletconnect.transaction.ui.common.WcTransactionRequestInfoContent
import com.tangem.features.walletconnect.transaction.ui.sign.WcTransactionModalBottomSheetContent
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun WcApproveTransactionModalBottomSheet(config: TangemBottomSheetConfig) {
    TangemModalBottomSheet<WcApproveTransactionUM>(
        config = config,
        containerColor = TangemTheme.colors.background.tertiary,
        title = { state ->
            TangemModalBottomSheetTitle(
                title = when (state.state) {
                    WcApproveTransactionUM.State.TRANSACTION -> {
                        resourceReference(R.string.wallet_connect_title)
                    }
                    WcApproveTransactionUM.State.CUSTOM_ALLOWANCE -> {
                        resourceReference(R.string.wc_custom_allowance_title)
                    }
                    WcApproveTransactionUM.State.TRANSACTION_REQUEST_INFO -> {
                        resourceReference(R.string.wc_transaction_request_title)
                    }
                },
                endIconRes = R.drawable.ic_close_24.takeIf {
                    state.state == WcApproveTransactionUM.State.TRANSACTION
                },
                onEndClick = state.actions.onDismiss,
                startIconRes = R.drawable.ic_back_24.takeIf {
                    state.state != WcApproveTransactionUM.State.TRANSACTION
                },
                onStartClick = state.actions.onBack,
            )
        },
        content = { state ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
            ) {
                when (state.state) {
                    WcApproveTransactionUM.State.TRANSACTION -> {
                        WcTransactionModalBottomSheetContent(state.transaction, state.actions)
                    }
                    WcApproveTransactionUM.State.CUSTOM_ALLOWANCE -> {
                        TODO("Will be done in the second part of the PR")
                    }
                    WcApproveTransactionUM.State.TRANSACTION_REQUEST_INFO -> {
                        WcTransactionRequestInfoContent(state.transactionRequestInfo, state.actions)
                    }
                }
            }
        },
    )
}

@Composable
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun WcApproveTransactionBottomSheetPreview(
    @PreviewParameter(WcApproveTransactionStateProvider::class) state: WcApproveTransactionUM,
) {
    TangemThemePreview {
        WcApproveTransactionModalBottomSheet(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = state,
            ),
        )
    }
}

private class WcApproveTransactionStateProvider : CollectionPreviewParameterProvider<WcApproveTransactionUM>(
    listOf(
        WcApproveTransactionUM(
            state = WcApproveTransactionUM.State.TRANSACTION,
            transaction = WcTransactionUM(
                appName = "React App",
                appIcon = "",
                isVerified = true,
                appSubtitle = "react-app.walletconnect.com",
                walletName = "Tangem 2.0",
                activeButtonText = resourceReference(R.string.common_send),
                networkInfo = WcNetworkInfoUM(name = "Ethereum", iconRes = R.drawable.img_eth_22),
                networkFee = "~ 0.22 $",
                spendAllowance = SpendAllowanceUM(amountText = "Unlimited USDT", tokenImageUrl = ""),
            ),
            transactionRequestInfo = WcTransactionRequestInfoUM(persistentListOf()),
            actions = WcTransactionActionsUM(
                onDismiss = {},
                onBack = {},
                activeButtonOnClick = {},
                onCopy = {},
                transactionRequestOnClick = {},
            ),
        ),
    ),
)