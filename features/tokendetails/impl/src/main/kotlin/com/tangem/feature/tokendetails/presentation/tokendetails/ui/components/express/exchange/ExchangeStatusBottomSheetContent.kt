package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.express.exchange

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.tangem.common.ui.expressStatus.ExpressEstimate
import com.tangem.common.ui.expressStatus.ExpressProvider
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH10
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.notifications.CurrencyNotification
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.domain.models.domain.ExchangeStatus
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.ExchangeStatusNotifications
import com.tangem.feature.tokendetails.presentation.tokendetails.state.express.ExchangeUM

@Composable
internal fun ExchangeStatusBottomSheetContent(state: ExchangeUM) {
    Column(modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16)) {
        SpacerH10()
        Text(
            text = stringResourceSafe(id = R.string.express_exchange_status_title),
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
            providerName = TextReference.Str(state.provider.name),
            providerType = TextReference.Str(state.provider.type.providerName),
            providerTxId = state.info.txExternalId,
            imageUrl = state.provider.imageLarge,
        )
        SpacerH12()
        ExchangeStatusBlock(
            statuses = state.statuses,
            showLink = state.showProviderLink,
            onClick = { state.info.onGoToProviderClick(state.info.txExternalUrl.orEmpty()) },
        )
        if (state.notification != null) {
            Notification(state = state.notification, activeStatus = state.activeStatus)
        }
        SpacerH24()
    }
}

@Composable
private fun Notification(state: ExchangeStatusNotifications, activeStatus: ExchangeStatus?) {
    AnimatedContent(
        targetState = state,
        modifier = Modifier.padding(top = TangemTheme.dimens.spacing12),
        label = "Exchange Status Notification Change",
    ) { notification ->
        when (notification) {
            is ExchangeStatusNotifications.CommonNotification -> {
                com.tangem.core.ui.components.notifications.Notification(
                    config = notification.config,
                    iconTint = when (activeStatus) {
                        ExchangeStatus.Verifying -> TangemTheme.colors.icon.attention
                        ExchangeStatus.Failed -> TangemTheme.colors.icon.warning
                        else -> null
                    },
                    containerColor = TangemTheme.colors.background.action,
                )
            }
            is ExchangeStatusNotifications.TokenRefunded -> {
                CurrencyNotification(
                    config = notification.config,
                    containerColor = TangemTheme.colors.background.action,
                )
            }
        }
    }
}