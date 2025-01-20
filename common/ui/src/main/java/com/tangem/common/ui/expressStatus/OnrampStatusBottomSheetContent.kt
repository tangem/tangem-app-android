package com.tangem.common.ui.expressStatus

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH10
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme

@Composable
fun OnrampStatusBottomSheetContent(state: ExpressTransactionStateUM.OnrampUM) {
    Column(modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16)) {
        SpacerH10()
        Text(
            text = stringResourceSafe(id = R.string.common_transaction_status),
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
            modifier = Modifier.align(CenterHorizontally),
        )
        SpacerH10()
        Text(
            text = stringResourceSafe(id = R.string.express_exchange_status_subtitle),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(CenterHorizontally),
        )
        SpacerH16()
        ExpressEstimate(
            timestamp = state.info.timestampFormatted,
            fromTokenIconState = state.info.fromCurrencyIcon,
            toTokenIconState = state.info.toCurrencyIcon,
            fromCryptoAmount = state.info.fromAmount,
            fromCryptoSymbol = state.info.fromAmountSymbol,
            toCryptoAmount = state.info.toAmount,
            toCryptoSymbol = state.info.toAmountSymbol,
            fromFiatAmount = state.info.fromFiatAmount,
            toFiatAmount = state.info.toFiatAmount,
        )
        SpacerH12()

        ExpressProvider(
            providerName = stringReference(state.providerName),
            providerType = stringReference(state.providerType),
            providerTxId = state.info.txExternalId,
            imageUrl = state.providerImageUrl,
        )
        SpacerH12()
        ExpressStatusBlock(state = state.info.status)
        ExpressStatusNotificationBlock(state = state.info.notification)
        SpacerH24()
    }
}