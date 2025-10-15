package com.tangem.features.walletconnect.transaction.ui.blockaid

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.approve.WcSpendAllowanceUM
import com.tangem.features.walletconnect.transaction.entity.blockaid.BlockAidNotificationUM
import com.tangem.features.walletconnect.transaction.entity.blockaid.WcEstimatedWalletChangeUM
import com.tangem.features.walletconnect.transaction.entity.blockaid.WcEstimatedWalletChangesUM
import com.tangem.features.walletconnect.transaction.entity.blockaid.WcSendReceiveTransactionCheckResultsUM
import com.tangem.features.walletconnect.transaction.ui.approve.WcSpendAllowanceItem
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

@Composable
internal fun TransactionCheckResultsItem(
    item: WcSendReceiveTransactionCheckResultsUM,
    onClickAllowToSpend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                ApproveDescription(
                    modifier = Modifier.padding(bottom = 6.dp, start = 12.dp, end = 12.dp),
                    onLearnMoreClick = item.spendAllowance.onLearnMoreClicked,
                )
            } else if (!item.additionalNotification.isNullOrEmpty()) {
                WcEstimatedWalletChangesNotificationItem(description = item.additionalNotification)
            } else {
                WcEstimatedWalletChangesNotificationItem()
            }
        }
    }
}

@Composable
private fun ApproveDescription(modifier: Modifier = Modifier, onLearnMoreClick: () -> Unit) {
    val linkText = stringResourceSafe(R.string.common_learn_more)
    val fullString = stringResourceSafe(R.string.wc_approve_description)
    val defaultColor = TangemTheme.colors.text.tertiary
    val linkColor = TangemTheme.colors.text.accent
    Text(
        modifier = modifier,
        style = TangemTheme.typography.caption2,
        text = buildAnnotatedString {
            appendColored(fullString, defaultColor)
            appendSpace()
            withLink(
                link = LinkAnnotation.Clickable(
                    tag = "WC_APPROVE_LEARN_MORE_TAG",
                    linkInteractionListener = { onLearnMoreClick() },
                ),
                block = { appendColored(text = linkText, color = linkColor) },
            )
        },
    )
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
        WcSendReceiveTransactionCheckResultsUM(
            isLoading = false,
            notification = BlockAidNotificationUM(
                type = BlockAidNotificationUM.Type.ERROR,
                title = TextReference.Res(R.string.wc_malicious_transaction),
                text = TextReference.Str("The transaction approves erc20 tokens to a known malicious address"),
            ),
            spendAllowance = WcSpendAllowanceUM(
                amountValue = BigDecimal.ZERO,
                isUnlimited = false,
                amountText = stringReference("0.00 WPOL"),
                tokenSymbol = "",
                tokenImageUrl = "",
                networkIconRes = 0,
                onLearnMoreClicked = {},
            ),
        ),
    )
}