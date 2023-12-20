package com.tangem.features.send.impl.presentation.ui.send

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.toBigDecimalOrDefault
import com.tangem.core.ui.components.inputrow.InputRowDefault
import com.tangem.core.ui.components.inputrow.InputRowImage
import com.tangem.core.ui.components.inputrow.InputRowRecipientDefault
import com.tangem.core.ui.components.transactions.TransactionDoneTitle
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.BigDecimalFormatter.formatCryptoAmount
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState
import com.tangem.features.send.impl.presentation.state.fee.FeeType

@Suppress("LongMethod")
@Composable
internal fun SendContent(uiState: SendUiState) {
    val amountState = uiState.amountState ?: return
    val recipientState = uiState.recipientState ?: return
    val feeState = uiState.feeState ?: return
    val sendState = uiState.sendState ?: return

    val isSuccess = sendState.isSuccess.collectAsStateWithLifecycle()
    val timestamp = sendState.transactionDate.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .padding(horizontal = TangemTheme.dimens.spacing16),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        AnimatedVisibility(visible = isSuccess.value) {
            TransactionDoneTitle(
                titleRes = R.string.sent_transaction_sent_title,
                date = timestamp.value,
            )
        }
        AnimatedVisibility(visible = !isSuccess.value) {
            FromWallet(
                walletName = amountState.walletName,
                walletBalance = amountState.walletBalance,
            )
        }
        AmountBlock(
            amountState = amountState,
            isSuccess = isSuccess,
            onClick = uiState.clickIntents::showAmount,
        )
        RecipientBlock(
            recipientState = recipientState,
            isSuccess = isSuccess,
            onClick = uiState.clickIntents::showRecipient,
        )
        FeeBlock(
            feeState = feeState,
            isSuccess = isSuccess,
            onClick = uiState.clickIntents::showFee,
        )
    }
}

@Composable
private fun FromWallet(walletName: String, walletBalance: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.button.disabled)
            .padding(TangemTheme.dimens.spacing12),
    ) {
        Text(
            text = buildAnnotatedString {
                append(stringResource(R.string.send_from_wallet_android))
                append(" ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(walletName)
                }
            },
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.secondary,
        )
        Text(
            text = walletBalance,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
            modifier = Modifier
                .padding(
                    top = TangemTheme.dimens.spacing8,
                ),
        )
    }
}

@Composable
private fun AmountBlock(amountState: SendStates.AmountState, isSuccess: State<Boolean>, onClick: () -> Unit) {
    val amount = amountState.amountTextField

    val cryptoAmount = formatCryptoAmount(
        cryptoCurrency = amountState.cryptoCurrencyStatus.currency,
        cryptoAmount = amount.value.toBigDecimalOrDefault(),
    )
    val fiatAmount = BigDecimalFormatter.formatFiatAmount(
        fiatAmount = amount.fiatValue.toBigDecimalOrDefault(),
        fiatCurrencyCode = amountState.appCurrency.code,
        fiatCurrencySymbol = amountState.appCurrency.symbol,
    )
    InputRowImage(
        title = TextReference.Res(R.string.send_amount_label),
        subtitle = TextReference.Str(cryptoAmount),
        caption = TextReference.Str(fiatAmount),
        tokenIconState = amountState.tokenIconState,
        showNetworkIcon = true,
        modifier = Modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .clickable(enabled = !isSuccess.value) { onClick() },
    )
}

@Composable
private fun RecipientBlock(recipientState: SendStates.RecipientState, isSuccess: State<Boolean>, onClick: () -> Unit) {
    val address = recipientState.addressTextField
    val memo = recipientState.memoTextField

    Column(
        modifier = Modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .clickable(enabled = !isSuccess.value) { onClick() },
    ) {
        val showMemo = memo != null && memo.value.isNotBlank()
        InputRowRecipientDefault(
            title = TextReference.Res(R.string.send_recipient),
            value = address.value,
            showDivider = showMemo,
        )
        if (showMemo) {
            InputRowDefault(
                title = TextReference.Res(R.string.send_extras_hint_memo),
                text = TextReference.Str(memo?.value.orEmpty()),
            )
        }
    }
}

@Composable
private fun FeeBlock(feeState: SendStates.FeeState, isSuccess: State<Boolean>, onClick: () -> Unit) {
    val feeSelector =
        feeState.feeSelectorState.collectAsStateWithLifecycle().value as? FeeSelectorState.Content ?: return
    val customValue = feeSelector.customValues.collectAsStateWithLifecycle().value.getOrNull(0)

    val feeValue = formatCryptoAmount(
        cryptoCurrency = feeState.cryptoCurrencyStatus.currency,
        cryptoAmount = when (val selectedFee = feeSelector.fees) {
            is TransactionFee.Single -> selectedFee.normal.amount.value
            is TransactionFee.Choosable -> when (feeSelector.selectedFee) {
                FeeType.SLOW -> selectedFee.minimum.amount.value
                FeeType.MARKET -> selectedFee.normal.amount.value
                FeeType.FAST -> selectedFee.priority.amount.value
                FeeType.CUSTOM -> customValue?.value.toBigDecimalOrDefault()
            }
        },
    )
    InputRowDefault(
        title = TextReference.Res(R.string.send_network_fee_title),
        text = TextReference.Str(feeValue),
        modifier = Modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .clickable(enabled = !isSuccess.value) { onClick() },
    )
}