package com.tangem.features.walletconnect.transaction.ui.common

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.divider.DividerWithPadding
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.v2.api.FeeSelectorBlockComponent
import com.tangem.features.walletconnect.transaction.entity.common.WcNetworkInfoUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionFeeState

@Composable
internal fun WcSendTransactionItems(
    walletName: String,
    networkInfo: WcNetworkInfoUM,
    feeState: WcTransactionFeeState,
    feeSelectorBlockComponent: FeeSelectorBlockComponent,
    modifier: Modifier = Modifier,
    address: String? = null,
) {
    Column(
        modifier = modifier
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
            walletName = walletName,
        )
        DividerWithPadding(start = TangemTheme.dimens.spacing40, end = TangemTheme.dimens.spacing12)
        WcNetworkItem(
            modifier = itemsModifier,
            networkInfo = networkInfo,
        )
        if (!address.isNullOrEmpty()) {
            DividerWithPadding(start = 40.dp, end = 12.dp)
            WcAddressItem(
                modifier = itemsModifier,
                addressText = address,
            )
        }
        DividerWithPadding(start = 40.dp, end = 12.dp)
        if (feeState != WcTransactionFeeState.None) {
            feeSelectorBlockComponent.Content(Modifier)
        }
    }
}