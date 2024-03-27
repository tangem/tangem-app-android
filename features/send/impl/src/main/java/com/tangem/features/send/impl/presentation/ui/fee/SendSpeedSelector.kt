package com.tangem.features.send.impl.presentation.ui.fee

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.BigDecimalFormatter.CAN_BE_LOWER_SIGN
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.fee.FeeType
import com.tangem.features.send.impl.presentation.state.fee.SendFeeNotification
import com.tangem.features.send.impl.presentation.state.previewdata.FeeStatePreviewData
import com.tangem.features.send.impl.presentation.state.previewdata.SendClickIntentsStub
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import java.math.BigDecimal

@Suppress("LongMethod")
@Composable
internal fun SendSpeedSelector(
    state: SendStates.FeeState,
    clickIntents: SendClickIntents,
    modifier: Modifier = Modifier,
) {
    val feeSelectorState = state.feeSelectorState
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(TangemTheme.shapes.roundedCornersXMedium)
                .background(TangemTheme.colors.background.action),
        ) {
            val fees = feeSelectorState.fees
            val choosableFees = fees as? TransactionFee.Choosable
            val isSelected = feeSelectorState.selectedFee
            val minimumAmount = choosableFees?.minimum?.amount
            val normalAmount = fees?.normal?.amount
            val priorityAmount = choosableFees?.priority?.amount
            SendSpeedSelectorItem(
                titleRes = R.string.common_fee_selector_option_slow,
                iconRes = R.drawable.ic_tortoise_24,
                amount = getCryptoReference(minimumAmount, state.isFeeApproximate),
                fiatAmount = getFiatReference(minimumAmount, state.rate, state.appCurrency),
                symbolLength = minimumAmount?.currencySymbol?.length,
                isSelected = isSelected == FeeType.Slow,
                isSingle = fees is TransactionFee.Single,
                onSelect = { clickIntents.onFeeSelectorClick(FeeType.Slow) },
                feeSelectorState = feeSelectorState,
            )
            SendSpeedSelectorItem(
                titleRes = R.string.common_fee_selector_option_market,
                iconRes = R.drawable.ic_bird_24,
                amount = getCryptoReference(normalAmount, state.isFeeApproximate),
                fiatAmount = getFiatReference(normalAmount, state.rate, state.appCurrency),
                symbolLength = normalAmount?.currencySymbol?.length,
                isSelected = isSelected == FeeType.Market,
                showDivider = fees !is TransactionFee.Single,
                onSelect = { clickIntents.onFeeSelectorClick(FeeType.Market) },
                feeSelectorState = feeSelectorState,
            )
            SendSpeedSelectorItem(
                titleRes = R.string.common_fee_selector_option_fast,
                iconRes = R.drawable.ic_hare_24,
                amount = getCryptoReference(priorityAmount, state.isFeeApproximate),
                fiatAmount = getFiatReference(priorityAmount, state.rate, state.appCurrency),
                symbolLength = priorityAmount?.currencySymbol?.length,
                isSelected = isSelected == FeeType.Fast,
                isSingle = fees is TransactionFee.Single,
                onSelect = { clickIntents.onFeeSelectorClick(FeeType.Fast) },
                showDivider = fees?.normal is Fee.Ethereum,
                feeSelectorState = feeSelectorState,
            )
            AnimatedVisibility(
                visible = fees?.normal is Fee.Ethereum,
                label = "Custom fee appearance animation",
            ) {
                val showWarning = state.notifications.any { it is SendFeeNotification.Warning.TooHigh }
                SendSpeedSelectorItem(
                    titleRes = R.string.common_fee_selector_option_custom,
                    iconRes = R.drawable.ic_edit_24,
                    isSelected = isSelected == FeeType.Custom,
                    onSelect = { clickIntents.onFeeSelectorClick(FeeType.Custom) },
                    showDivider = fees?.normal !is Fee.Ethereum,
                    showWarning = showWarning,
                    feeSelectorState = feeSelectorState,
                )
            }
        }
        FooterText(clickIntents::onReadMoreClick)
    }
}

@Composable
private fun FooterText(onReadMoreClick: () -> Unit) {
    val linkText = stringResource(R.string.common_read_more)
    val fullString = stringResource(R.string.common_fee_selector_footer, linkText)
    val linkTextPosition = fullString.length - linkText.length
    val defaultStyle = TangemTheme.colors.text.tertiary
    val linkStyle = TangemTheme.colors.text.accent
    val annotatedString = remember(defaultStyle, linkStyle) {
        buildAnnotatedString {
            withStyle(SpanStyle(defaultStyle)) {
                append(fullString.substring(0, linkTextPosition))
            }
            withStyle(SpanStyle(linkStyle)) {
                append(fullString.substring(linkTextPosition, fullString.length))
            }
        }
    }

    val click = { i: Int ->
        val readMoreStyle = requireNotNull(annotatedString.spanStyles.getOrNull(1))
        if (i in readMoreStyle.start..readMoreStyle.end) {
            onReadMoreClick()
        }
    }

    ClickableText(
        text = annotatedString,
        style = TangemTheme.typography.caption2.copy(textAlign = TextAlign.Start),
        modifier = Modifier.padding(top = TangemTheme.dimens.spacing8),
        onClick = click,
    )
}

// todo remove after refactoring https://tangem.atlassian.net/browse/AND-5940
private fun getCryptoReference(amount: Amount?, isFeeApproximate: Boolean): TextReference {
    if (amount == null) return TextReference.EMPTY
    return combinedReference(
        if (isFeeApproximate) stringReference("$CAN_BE_LOWER_SIGN ") else TextReference.EMPTY,
        stringReference(
            BigDecimalFormatter.formatCryptoAmount(
                cryptoAmount = amount.value,
                cryptoCurrency = amount.currencySymbol,
                decimals = amount.decimals,
            ),
        ),
    )
}

// todo remove after refactoring https://tangem.atlassian.net/browse/AND-5940
private fun getFiatReference(amount: Amount?, rate: BigDecimal?, appCurrency: AppCurrency): TextReference {
    if (amount == null) return TextReference.EMPTY
    return stringReference(
        BigDecimalFormatter.formatFiatAmount(
            fiatAmount = rate?.let { amount.value?.multiply(it) },
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
        ),
    )
}

// region Preview
@Preview
@Composable
private fun SendSpeedSelectorPreview_Light(
    @PreviewParameter(SendSpeedSelectorPreviewProvider::class) feeState: SendStates.FeeState,
) {
    TangemTheme {
        SendSpeedSelector(state = feeState, clickIntents = SendClickIntentsStub)
    }
}

@Preview
@Composable
private fun SendSpeedSelectorPreview_Dark(
    @PreviewParameter(SendSpeedSelectorPreviewProvider::class) feeState: SendStates.FeeState,
) {
    TangemTheme(isDark = true) {
        SendSpeedSelector(state = feeState, clickIntents = SendClickIntentsStub)
    }
}

private class SendSpeedSelectorPreviewProvider : PreviewParameterProvider<SendStates.FeeState> {
    override val values: Sequence<SendStates.FeeState>
        get() = sequenceOf(
            FeeStatePreviewData.feeState,
            FeeStatePreviewData.loadingFeeState,
            FeeStatePreviewData.errorFeeState,
        )
}
// endregion
