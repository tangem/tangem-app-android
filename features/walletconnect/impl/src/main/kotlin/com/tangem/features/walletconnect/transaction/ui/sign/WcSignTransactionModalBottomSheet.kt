package com.tangem.features.walletconnect.transaction.ui.sign

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
import com.tangem.features.walletconnect.transaction.entity.*
import com.tangem.features.walletconnect.transaction.ui.common.TransactionRequestInfoContent
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun WcSignTransactionModalBottomSheet(config: TangemBottomSheetConfig) {
    TangemModalBottomSheet<WcSignTransactionUM>(
        config = config,
        containerColor = TangemTheme.colors.background.tertiary,
        title = { state ->
            TangemModalBottomSheetTitle(
                title = when (state.state) {
                    WcSignTransactionUM.State.TRANSACTION -> {
                        resourceReference(R.string.wallet_connect_title)
                    }
                    WcSignTransactionUM.State.TRANSACTION_REQUEST_INFO -> {
                        resourceReference(R.string.wc_transaction_request_title)
                    }
                },
                endIconRes = state.endIconRes.takeIf {
                    state.state == WcSignTransactionUM.State.TRANSACTION
                },
                onEndClick = state.actions.onDismiss,
                startIconRes = state.startIconRes.takeIf {
                    state.state == WcSignTransactionUM.State.TRANSACTION_REQUEST_INFO
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
                    WcSignTransactionUM.State.TRANSACTION -> {
                        WcSignTransactionModalBottomSheetContent(state)
                    }
                    WcSignTransactionUM.State.TRANSACTION_REQUEST_INFO -> {
                        TransactionRequestInfoContent(state)
                    }
                }
            }
        },
    )
}

@Composable
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun WcSignTransactionBottomSheetPreview(
    @PreviewParameter(WcSignTransactionStateProvider::class) state: WcSignTransactionUM,
) {
    TangemThemePreview {
        WcSignTransactionModalBottomSheet(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = state,
            ),
        )
    }
}

private class WcSignTransactionStateProvider : CollectionPreviewParameterProvider<WcSignTransactionUM>(
    listOf(
        WcSignTransactionUM(
            startIconRes = R.drawable.ic_back_24,
            endIconRes = R.drawable.ic_close_24,
            transactionIconRes = R.drawable.ic_doc_new_24,
            state = WcSignTransactionUM.State.TRANSACTION,
            transaction = WcTransactionUM(
                appName = "React App",
                appIcon = "",
                isVerified = true,
                appSubtitle = "react-app.walletconnect.com",
                walletName = "Tangem 2.0",
                networkInfo = WcNetworkInfoUM(name = "Ethereum", iconRes = R.drawable.img_eth_22),
            ),
            transactionRequestInfo = WcTransactionRequestInfoUM(
                persistentListOf(
                    WcTransactionRequestInfoItemUM(
                        title = resourceReference(R.string.wc_signature_type),
                        description = "personal_sign",
                    ),
                    WcTransactionRequestInfoItemUM(
                        title = resourceReference(R.string.wc_contents),
                        description = "Hello! My name is John Dow. test@tange.com",
                    ),
                ),
            ),
            actions = WcTransactionActionsUM(
                onDismiss = {},
                onBack = {},
                onSign = {},
                onCopy = {},
                transactionRequestOnClick = {},
            ),
        ),
        WcSignTransactionUM(
            startIconRes = R.drawable.ic_back_24,
            endIconRes = R.drawable.ic_close_24,
            transactionIconRes = R.drawable.ic_doc_new_24,
            state = WcSignTransactionUM.State.TRANSACTION_REQUEST_INFO,
            transaction = WcTransactionUM(
                appName = "React App",
                appIcon = "",
                isVerified = true,
                appSubtitle = "react-app.walletconnect.com",
                walletName = "Tangem 2.0",
                networkInfo = WcNetworkInfoUM(name = "Ethereum", iconRes = R.drawable.img_eth_22),
            ),
            transactionRequestInfo = WcTransactionRequestInfoUM(
                persistentListOf(
                    WcTransactionRequestInfoItemUM(
                        title = resourceReference(R.string.wc_signature_type),
                        description = "personal_sign",
                    ),
                    WcTransactionRequestInfoItemUM(
                        title = resourceReference(R.string.wc_contents),
                        description = "Hello! My name is John Dow. test@tange.com",
                    ),
                ),
            ),
            actions = WcTransactionActionsUM(
                onDismiss = {},
                onBack = {},
                onSign = {},
                onCopy = {},
                transactionRequestOnClick = {},
            ),
        ),
    ),
)