package com.tangem.features.send.impl.presentation.state.previewdata

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.tangem.core.ui.components.currency.tokenicon.TokenIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.tokens.model.AmountType
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.SendUiStateType
import com.tangem.features.send.impl.presentation.state.amount.SendAmountSegmentedButtonsConfig
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

internal object AmountStatePreviewData {

    val amountState = SendStates.AmountState(
        type = SendUiStateType.Amount,
        isPrimaryButtonEnabled = false,
        walletName = "Family Wallet",
        walletBalance = stringReference("2 130,88 USDT (2 129,92 \$)"),
        tokenIconState = TokenIconState.Loading,
        segmentedButtonConfig = persistentListOf(
            SendAmountSegmentedButtonsConfig(
                title = stringReference("USDT"),
                iconState = TokenIconState.Locked,
                isFiat = false,
            ),
            SendAmountSegmentedButtonsConfig(
                title = stringReference("USD"),
                isFiat = true,
            ),
        ),
        appCurrencyCode = "usd",
        amountTextField = SendTextField.AmountField(
            value = "",
            onValueChange = {},
            keyboardOptions = KeyboardOptions.Default,
            keyboardActions = KeyboardActions.Default,
            cryptoAmount = Amount(
                currencySymbol = "USDT",
                value = BigDecimal.ZERO,
                decimals = 18,
                type = AmountType.CoinType,
            ),
            fiatAmount = Amount(
                currencySymbol = "$",
                value = BigDecimal.ZERO,
                decimals = 2,
                type = AmountType.CoinType,
            ),
            isFiatValue = false,
            fiatValue = "123.123",
            isFiatUnavailable = false,
            isError = false,
            error = TextReference.EMPTY,
            isValuePasted = false,
            onValuePastedTriggerDismiss = {},
        ),
        isSegmentedButtonsEnabled = true,
        selectedButton = 0,
    )

    val amountWithValueState = amountState.copy(
        amountTextField = amountState.amountTextField.copy(
            value = "100.00",
            cryptoAmount = amountState.amountTextField.cryptoAmount.copy(
                value = BigDecimal("100.00"),
            ),
            fiatValue = "99.98",
            fiatAmount = amountState.amountTextField.fiatAmount.copy(
                value = BigDecimal("99.98"),
            ),
        ),
    )

    val amountWithValueFiatState = amountWithValueState.copy(
        amountTextField = amountWithValueState.amountTextField.copy(isFiatValue = false),
    )

    val amountErrorState = amountWithValueState.copy(
        amountTextField = amountWithValueState.amountTextField.copy(
            isError = true,
            error = stringReference("Insufficient funds for transfer"),
        ),
    )
}