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
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

internal object AmountStatePreviewData {

    val amountState = SendStates.AmountState(
        type = SendUiStateType.Amount,
        isPrimaryButtonEnabled = false,
        walletName = "Wallet",
        walletBalance = stringReference("123.123"),
        tokenIconState = TokenIconState.Loading,
        segmentedButtonConfig = persistentListOf(),
        notifications = persistentListOf(),
        isFeeLoading = false,
        appCurrencyCode = "usd",
        subtractedFee = null,
        amountTextField = SendTextField.AmountField(
            value = "123.123123123123123123",
            onValueChange = {},
            keyboardOptions = KeyboardOptions.Default,
            keyboardActions = KeyboardActions.Default,
            cryptoAmount = Amount(
                currencySymbol = "ETH",
                value = BigDecimal(123.123),
                decimals = 18,
                type = AmountType.CoinType,
            ),
            fiatAmount = Amount(
                currencySymbol = "$",
                value = BigDecimal(123.123),
                decimals = 2,
                type = AmountType.CoinType,
            ),
            isFiatValue = false,
            fiatValue = "123.123",
            isFiatUnavailable = false,
            isError = false,
            error = TextReference.EMPTY,
        ),
        isSegmentedButtonsEnabled = true,
    )

    val fiatAmountState = amountState.copy(
        amountTextField = amountState.amountTextField.copy(isFiatValue = true),
    )
}