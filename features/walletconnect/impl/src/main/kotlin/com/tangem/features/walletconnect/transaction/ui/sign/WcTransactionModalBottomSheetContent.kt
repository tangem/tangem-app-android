package com.tangem.features.walletconnect.transaction.ui.sign

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.walletconnect.connections.ui.WcAppInfoItem
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionActionsUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionUM
import com.tangem.features.walletconnect.transaction.ui.approve.SpendAllowanceItem
import com.tangem.features.walletconnect.transaction.ui.common.*
import com.tangem.features.walletconnect.transaction.ui.common.WcNetworkItem
import com.tangem.features.walletconnect.transaction.ui.common.WcTransactionRequestButtons
import com.tangem.features.walletconnect.transaction.ui.common.WcTransactionRequestItem
import com.tangem.features.walletconnect.transaction.ui.common.WcWalletItem

@Composable
internal fun WcTransactionModalBottomSheetContent(transaction: WcTransactionUM, actions: WcTransactionActionsUM) {
    Column(modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16)) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(TangemTheme.dimens.radius14))
                .background(color = TangemTheme.colors.background.action)
                .fillMaxWidth()
                .animateContentSize(),
        ) {
            WcSmallTitleItem(R.string.wc_request_from)
            WcAppInfoItem(
                iconUrl = transaction.appIcon,
                title = transaction.appName,
                subtitle = transaction.appSubtitle,
                isVerified = transaction.isVerified,
            )
            DividerWithPadding(start = 0.dp, end = 0.dp)
            WcTransactionRequestItem(
                iconRes = R.drawable.ic_doc_new_24,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { actions.transactionRequestOnClick() }
                    .padding(TangemTheme.dimens.spacing12),
            )
        }
        Column(modifier = Modifier.padding(top = TangemTheme.dimens.spacing16)) {
            if (transaction.spendAllowance != null) {
                SpendAllowanceItem(transaction.spendAllowance)
                Spacer(Modifier.height(TangemTheme.dimens.spacing16))
            }
            WcSignTransactionItems(transaction)
            WcTransactionRequestButtons(
                modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing16),
                onDismiss = actions.onDismiss,
                onSign = actions.activeButtonOnClick,
                activeButtonText = transaction.activeButtonText,
                isLoading = transaction.isLoading,
            )
        }
    }
}

@Composable
private fun WcSignTransactionItems(transaction: WcTransactionUM) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(TangemTheme.dimens.radius14))
            .background(color = TangemTheme.colors.background.action)
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        val itemsModifier = Modifier
            .fillMaxWidth()
            .padding(TangemTheme.dimens.spacing12)

        DividerWithPadding(start = 0.dp, end = 0.dp)
        WcWalletItem(
            modifier = itemsModifier,
            walletName = transaction.walletName,
        )
        DividerWithPadding(start = TangemTheme.dimens.spacing40, end = TangemTheme.dimens.spacing12)
        WcNetworkItem(
            modifier = itemsModifier,
            networkInfo = transaction.networkInfo,
        )
        if (!transaction.addressText.isNullOrEmpty()) {
            DividerWithPadding(start = TangemTheme.dimens.spacing40, end = TangemTheme.dimens.spacing12)
            WcAddressItem(
                modifier = itemsModifier,
                addressText = transaction.addressText,
            )
        }
        if (!transaction.networkFee.isNullOrEmpty()) {
            DividerWithPadding(start = TangemTheme.dimens.spacing40, end = TangemTheme.dimens.spacing12)
            WcNetworkFeeItem(
                modifier = itemsModifier,
                networkFeeText = transaction.networkFee,
            )
        }
    }
}

@Composable
private fun DividerWithPadding(start: Dp, end: Dp) {
    HorizontalDivider(
        modifier = Modifier.padding(
            start = start,
            end = end,
        ),
        thickness = TangemTheme.dimens.size1,
        color = TangemTheme.colors.stroke.primary,
    )
}