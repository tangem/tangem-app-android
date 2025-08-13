package com.tangem.features.walletconnect.transaction.ui.blockaid

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.isNullOrEmpty
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.blockaid.BlockAidNotificationUM
import com.tangem.features.walletconnect.transaction.entity.blockaid.WcEstimatedWalletChangeUM
import com.tangem.features.walletconnect.transaction.entity.blockaid.WcEstimatedWalletChangesUM
import com.tangem.features.walletconnect.transaction.entity.blockaid.WcSendReceiveTransactionCheckResultsUM
import com.tangem.features.walletconnect.transaction.ui.approve.WcSpendAllowanceItem
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun TransactionCheckResultsItem(
    item: WcSendReceiveTransactionCheckResultsUM,
    onClickAllowToSpend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (item.isLoading) {
            WcEstimatedWalletChangesLoadingItem()
        } else {
            if (item.notification != null) {
                WcTransactionCheckErrorItem(item.notification)
            }
            if (item.estimatedWalletChanges != null) {
                WcEstimatedWalletChangesItem(item.estimatedWalletChanges)
            } else if (item.spendAllowance != null) {
                WcSpendAllowanceItem(item.spendAllowance, onClickAllowToSpend)
            } else if (!item.additionalNotification.isNullOrEmpty()) {
                WcEstimatedWalletChangesNotificationItem(description = item.additionalNotification)
            } else {
                WcEstimatedWalletChangesNotificationItem()
            }
        }
    }
}

@Composable
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TransactionCheckResultsItemPreview(
    @PreviewParameter(TransactionCheckResultsItemProvider::class) item: WcSendReceiveTransactionCheckResultsUM,
) {
    TangemThemePreview {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors.background.tertiary),
        ) {
            TransactionCheckResultsItem(item = item, {})
        }
    }
}

private class TransactionCheckResultsItemProvider : PreviewParameterProvider<WcSendReceiveTransactionCheckResultsUM> {
    override val values = sequenceOf(
        WcSendReceiveTransactionCheckResultsUM(
            isLoading = false,
            notification = BlockAidNotificationUM(
                type = BlockAidNotificationUM.Type.ERROR,
                title = TextReference.Res(R.string.wc_malicious_transaction),
                text = TextReference.Str("The transaction approves erc20 tokens to a known malicious address"),
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
    )
}