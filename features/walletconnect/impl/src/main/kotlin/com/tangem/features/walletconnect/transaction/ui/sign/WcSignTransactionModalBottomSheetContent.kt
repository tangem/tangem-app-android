package com.tangem.features.walletconnect.transaction.ui.sign

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.walletconnect.connections.ui.WcAppInfoItem
import com.tangem.features.walletconnect.transaction.entity.WcSignTransactionUM
import com.tangem.features.walletconnect.transaction.ui.common.*
import com.tangem.features.walletconnect.transaction.ui.common.WcNetworkItem
import com.tangem.features.walletconnect.transaction.ui.common.WcTransactionRequestButtons
import com.tangem.features.walletconnect.transaction.ui.common.WcTransactionRequestItem
import com.tangem.features.walletconnect.transaction.ui.common.WcWalletItem

@Composable
internal fun WcSignTransactionModalBottomSheetContent(state: WcSignTransactionUM) {
    Column(modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16)) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(TangemTheme.dimens.radius14))
                .background(color = TangemTheme.colors.background.action)
                .fillMaxWidth()
                .animateContentSize(),
        ) {
            RequestFromItem()
            WcAppInfoItem(
                iconUrl = state.transaction.appIcon,
                title = state.transaction.appName,
                subtitle = state.transaction.appSubtitle,
                isVerified = state.transaction.isVerified,
            )
            HorizontalDivider(
                thickness = TangemTheme.dimens.size1,
                color = TangemTheme.colors.stroke.primary,
            )
            WcTransactionRequestItem(
                iconRes = state.transactionIconRes,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { state.actions.transactionRequestOnClick() }
                    .padding(TangemTheme.dimens.spacing12),
            )
        }
        Column(modifier = Modifier.padding(top = TangemTheme.dimens.spacing16)) {
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

                HorizontalDivider(
                    thickness = TangemTheme.dimens.size1,
                    color = TangemTheme.colors.stroke.primary,
                )
                WcWalletItem(
                    modifier = itemsModifier,
                    walletName = state.transaction.walletName,
                )
                HorizontalDivider(
                    thickness = TangemTheme.dimens.size1,
                    color = TangemTheme.colors.stroke.primary,
                )
                WcNetworkItem(
                    modifier = itemsModifier,
                    networkInfo = state.transaction.networkInfo,
                )
            }
            WcTransactionRequestButtons(
                modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing16),
                onDismiss = state.actions.onDismiss,
                onSign = state.actions.onSign,
                isLoading = state.transaction.isLoading,
            )
        }
    }
}