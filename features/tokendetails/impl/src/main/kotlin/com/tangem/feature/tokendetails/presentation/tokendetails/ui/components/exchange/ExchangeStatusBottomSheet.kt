package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.exchange

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
import com.tangem.feature.swap.domain.models.domain.ExchangeStatus
import com.tangem.feature.tokendetails.presentation.tokendetails.state.SwapTransactionsState

@Composable
internal fun ExchangeStatusBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet(
        config = config,
        containerColor = TangemTheme.colors.background.tertiary,
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
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(CenterHorizontally),
        )
        SpacerH16()
        ExchangeEstimate(
            timestamp = config.timestamp,
            fromTokenIconState = config.fromCurrencyIcon,
            toTokenIconState = config.toCurrencyIcon,
            fromCryptoAmount = TextReference.Str(config.fromCryptoAmount),
            fromCryptoSymbol = config.fromCryptoSymbol,
            toCryptoAmount = TextReference.Str(config.toCryptoAmount),
            toCryptoSymbol = config.toCryptoSymbol,
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
            showLink = config.showProviderLink,
            onClick = { config.onGoToProviderClick(config.txUrl.orEmpty()) },
        )
        AnimatedContent(
            targetState = config.notification,
            label = "Exchange Status Notification Change",
        ) {
            it?.let {
                val tint = when (config.activeStatus) {
                    ExchangeStatus.Verifying -> TangemTheme.colors.icon.attention
                    ExchangeStatus.Failed -> TangemTheme.colors.icon.warning
                    else -> null
                }
                Notification(
                    config = it.config,
                    iconTint = tint,
                    containerColor = TangemTheme.colors.background.action,
                    modifier = Modifier.padding(top = TangemTheme.dimens.spacing12),
                )
            }
        }
        SpacerH24()
    }
}

internal data class ExchangeStatusBottomSheetConfig(
    val value: SwapTransactionsState,
) : TangemBottomSheetConfigContent