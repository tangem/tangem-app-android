package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.express.onramp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.tangem.common.ui.expressStatus.ExpressStatusBlock
import com.tangem.common.ui.expressStatus.ExpressStatusNotificationBlock
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH10
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tokendetails.presentation.tokendetails.state.express.ExpressTransactionStateUM
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.express.ExpressEstimate
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.express.ExpressProvider

@Composable
internal fun OnrampStatusBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet(
        config = config,
        containerColor = TangemTheme.colors.background.tertiary,
    ) { content: OnrampStatusBottomSheetConfig ->
        OnrampStatusBottomSheetContent(config = content.value)
    }
}

@Composable
private fun OnrampStatusBottomSheetContent(config: ExpressTransactionStateUM.OnrampUM) {
    Column(modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16)) {
        SpacerH10()
        Text(
            text = stringResource(id = R.string.common_transaction_status),
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
        ExpressEstimate(
            timestamp = config.info.timestamp,
            fromTokenIconState = config.info.fromCurrencyIcon,
            toTokenIconState = config.info.toCurrencyIcon,
            fromCryptoAmount = config.info.fromAmount,
            fromCryptoSymbol = config.info.fromAmountSymbol,
            toCryptoAmount = config.info.toAmount,
            toCryptoSymbol = config.info.toAmountSymbol,
            fromFiatAmount = config.info.fromFiatAmount,
            toFiatAmount = config.info.toFiatAmount,
        )
        SpacerH12()

        ExpressProvider(
            providerName = stringReference(config.providerName),
            providerType = stringReference(config.providerType),
            providerTxId = config.info.txExternalId,
            imageUrl = config.providerImageUrl,
        )
        SpacerH12()
        ExpressStatusBlock(state = config.info.status)
        if (config.info.notification != null) {
            ExpressStatusNotificationBlock(state = config.info.notification)
        }
        SpacerH24()
    }
}

internal data class OnrampStatusBottomSheetConfig(
    val value: ExpressTransactionStateUM.OnrampUM,
) : TangemBottomSheetConfigContent