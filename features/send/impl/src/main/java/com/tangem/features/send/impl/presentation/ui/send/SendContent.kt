package com.tangem.features.send.impl.presentation.ui.send

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.tangem.core.ui.components.inputrow.InputRowDefault
import com.tangem.core.ui.components.inputrow.InputRowImage
import com.tangem.core.ui.components.inputrow.InputRowRecipientDefault
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.transactions.TransactionDoneTitle
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.BigDecimalFormatter.formatCryptoAmount
import com.tangem.core.ui.utils.BigDecimalFormatter.formatFiatAmount
import com.tangem.domain.tokens.model.AmountType
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.SendNotification
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.SendUiState
import kotlinx.collections.immutable.ImmutableList

@Suppress("LongMethod")
@Composable
internal fun SendContent(uiState: SendUiState) {
    val amountState = uiState.amountState ?: return
    val recipientState = uiState.recipientState ?: return
    val feeState = uiState.feeState ?: return
    val sendState = uiState.sendState

    val isSuccess = sendState.isSuccess
    val timestamp = sendState.transactionDate

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = TangemTheme.dimens.spacing16),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12)) {
                AnimatedVisibility(visible = isSuccess) {
                    TransactionDoneTitle(
                        titleRes = R.string.sent_transaction_sent_title,
                        date = timestamp,
                    )
                }
                AnimatedVisibility(visible = !isSuccess) {
                    FromWallet(
                        walletName = amountState.walletName,
                        walletBalance = amountState.walletBalance.resolveReference(),
                    )
                }
                RecipientBlock(
                    recipientState = recipientState,
                    isSuccess = isSuccess,
                    isEditingDisabled = uiState.isEditingDisabled,
                    onClick = uiState.clickIntents::showRecipient,
                )
                AmountBlock(
                    amountState = amountState,
                    isSuccess = isSuccess,
                    isEditingDisabled = uiState.isEditingDisabled,
                    onClick = uiState.clickIntents::showAmount,
                )
                FeeBlock(
                    feeState = feeState,
                    isSuccess = isSuccess,
                    onClick = uiState.clickIntents::showFee,
                )
            }
        }
        notifications(sendState.notifications)
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
private fun AmountBlock(
    amountState: SendStates.AmountState,
    isSuccess: Boolean,
    isEditingDisabled: Boolean,
    onClick: () -> Unit,
) {
    val amount = amountState.amountTextField

    val cryptoAmount = formatCryptoAmount(
        cryptoAmount = amount.cryptoAmount.value,
        cryptoCurrency = amount.cryptoAmount.currencySymbol,
        decimals = amount.cryptoAmount.decimals,
    )
    val fiatAmount = formatFiatAmount(
        fiatAmount = amount.fiatAmount.value,
        fiatCurrencyCode = (amount.fiatAmount.type as AmountType.FiatType).code,
        fiatCurrencySymbol = amount.fiatAmount.currencySymbol,
    )
    val backgroundColor = if (isEditingDisabled) {
        TangemTheme.colors.button.disabled
    } else {
        TangemTheme.colors.background.action
    }
    InputRowImage(
        title = TextReference.Res(R.string.send_amount_label),
        subtitle = TextReference.Str(cryptoAmount),
        caption = TextReference.Str(fiatAmount),
        tokenIconState = amountState.tokenIconState,
        showNetworkIcon = true,
        modifier = Modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(backgroundColor)
            .clickable(enabled = !isSuccess && !isEditingDisabled) { onClick() },
    )
}

@Composable
private fun RecipientBlock(
    recipientState: SendStates.RecipientState,
    isSuccess: Boolean,
    isEditingDisabled: Boolean,
    onClick: () -> Unit,
) {
    val address = recipientState.addressTextField
    val memo = recipientState.memoTextField
    val backgroundColor = if (isEditingDisabled) {
        TangemTheme.colors.button.disabled
    } else {
        TangemTheme.colors.background.action
    }

    Column(
        modifier = Modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(backgroundColor)
            .clickable(enabled = !isSuccess && !isEditingDisabled) { onClick() },
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
private fun FeeBlock(feeState: SendStates.FeeState, isSuccess: Boolean, onClick: () -> Unit) {
    val fee = feeState.fee ?: return
    val feeCryptoValue = formatCryptoAmount(
        cryptoAmount = fee.amount.value,
        cryptoCurrency = fee.amount.currencySymbol,
        decimals = fee.amount.decimals,
    )
    val feeFiatValue = formatFiatAmount(
        fiatAmount = feeState.rate?.let { fee.amount.value?.multiply(it) },
        fiatCurrencyCode = feeState.appCurrency.code,
        fiatCurrencySymbol = feeState.appCurrency.symbol,
    )
    InputRowDefault(
        title = resourceReference(R.string.common_network_fee_title),
        text = resourceReference(
            id = R.string.send_wallet_balance_format,
            wrappedList(feeCryptoValue, feeFiatValue),
        ),
        modifier = Modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .clickable(enabled = !isSuccess) { onClick() },
    )
}

@OptIn(ExperimentalFoundationApi::class)
internal fun LazyListScope.notifications(configs: ImmutableList<SendNotification>, modifier: Modifier = Modifier) {
    items(
        items = configs,
        key = { it::class.java },
        contentType = { it::class.java },
        itemContent = {
            Notification(
                config = it.config,
                modifier = modifier.animateItemPlacement(),
                containerColor = when (it) {
                    is SendNotification.Warning.HighFeeError -> TangemTheme.colors.background.action
                    else -> TangemTheme.colors.button.disabled
                },
                iconTint = when (it) {
                    is SendNotification.Error -> TangemTheme.colors.icon.warning
                    is SendNotification.Warning -> null
                },
            )
        },
    )
}