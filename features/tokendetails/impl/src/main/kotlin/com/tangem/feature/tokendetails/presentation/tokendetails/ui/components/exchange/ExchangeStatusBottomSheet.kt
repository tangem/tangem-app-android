package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.exchange

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH10
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.toDateFormat
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.feature.tokendetails.presentation.tokendetails.state.SwapTransactionsState

@Composable
internal fun ExchangeStatusBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet(
        config = config,
        color = TangemTheme.colors.background.tertiary,
    ) { content: ExchangeStatusBottomSheetConfig ->
        ExchangeStatusBottomSheetContent(content = content)
    }
}

@Composable
private fun ExchangeStatusBottomSheetContent(content: ExchangeStatusBottomSheetConfig) {
    val config = content.value
    Column(
        modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16),
    ) {
        SpacerH10()
        Text(
            text = stringResource(id = R.string.express_exchange_status_title),
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
            modifier = Modifier.align(CenterHorizontally),
        )
        SpacerH10()
        Text(
            text = stringResource(id = R.string.express_exchange_status_subtitle),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.secondary,
            modifier = Modifier
                .align(CenterHorizontally),
        )
        SpacerH16()
        val timestamp = config.timestamp
        ExchangeEstimate(
            timestamp = TextReference.Str("${timestamp.toDateFormat()}, ${timestamp.toTimeFormat()}"),
            fromTokenIconState = config.fromCurrencyIcon,
            toTokenIconState = config.toCurrencyIcon,
            fromCryptoAmount = TextReference.Str(config.fromCryptoAmount),
            toCryptoAmount = TextReference.Str(config.toCryptoAmount),
            fromFiatAmount = TextReference.Str(config.fromFiatAmount),
            toFiatAmount = TextReference.Str(config.toFiatAmount),
        )
        SpacerH12()
        ExchangeProvider(
            providerName = TextReference.Str(config.provider.name),
            providerType = TextReference.Str(config.provider.type.name),
            imageUrl = config.provider.imageLarge,
        )
        SpacerH12()
        ExchangeStatusBlock(
            statuses = config.statuses,
            onClick = config.onGoToProviderClick,
        )
        AnimatedContent(
            targetState = config.notification,
            label = "Exchange Status Notification Change",
        ) {
            it?.let {
                SpacerH12()
                Notification(config = it.config)
            }
        }
        SpacerH24()
    }
}

internal data class ExchangeStatusBottomSheetConfig(
    val value: SwapTransactionsState,
) : TangemBottomSheetConfigContent
