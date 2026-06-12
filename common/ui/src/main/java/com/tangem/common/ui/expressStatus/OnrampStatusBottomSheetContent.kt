package com.tangem.common.ui.expressStatus

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.core.ui.R
import com.tangem.core.ui.components.*
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme

@Composable
fun OnrampStatusBottomSheetContent(state: ExpressTransactionStateUM.OnrampUM, isExpressShareButtonEnabled: Boolean) {
    Column(
        modifier = Modifier
            .padding(horizontal = TangemTheme.dimens.spacing16)
            .verticalScroll(rememberScrollState()),
    ) {
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
        ExpressHideButton(
            isTerminal = state.activeStatus.isTerminal,
            isAutoDisposable = state.activeStatus.isAutoDisposable,
            onClick = state.info.onDisposeExpressStatus,
        )
        if (isExpressShareButtonEnabled) {
            SpacerH(80.dp)
        } else {
            SpacerH24()
        }
    }
}