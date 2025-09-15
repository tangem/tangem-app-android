package com.tangem.common.ui.amountScreen.preview

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.tangem.common.ui.amountScreen.models.AmountFieldModel
import com.tangem.common.ui.amountScreen.models.AmountSegmentedButtonsConfig
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.tokens.model.AmountType
import com.tangem.utils.StringsSigns
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

object AmountStatePreviewData {

    val emptyState = AmountState.Empty(isRedesignEnabled = true)

    val amountState = AmountState.Data(
        isPrimaryButtonEnabled = false,
        title = stringReference("Family Wallet"),
        availableBalance = stringReference("2 130,81231238 USDT • 2 129,12 \$)"),
        availableBalanceCrypto = stringReference("2 130,81231238 USDT"),
        availableBalanceFiat = stringReference("1 232 129,12 \$"),
        tokenIconState = CurrencyIconState.Loading,
        segmentedButtonConfig = persistentListOf(
            AmountSegmentedButtonsConfig(
                title = stringReference("USDT"),
                iconState = CurrencyIconState.Locked,
                isFiat = false,
            ),
            AmountSegmentedButtonsConfig(
                title = stringReference("USD"),
                isFiat = true,
            ),
        ),
        appCurrency = AppCurrency.Default,
        tokenName = stringReference("Tether"),
        amountTextField = AmountFieldModel(
            value = "2 130,81231238",
            onValueChange = {},
            keyboardOptions = KeyboardOptions.Default,
            keyboardActions = KeyboardActions.Default,
            cryptoAmount = Amount(
                currencySymbol = "USDT",
                value = "2112312330.81212331238".toBigDecimal(),
                decimals = 18,
                type = AmountType.CoinType,
            ),
            fiatAmount = Amount(
                currencySymbol = "$",
                value = "2111232330.81".toBigDecimal(),
                decimals = 2,
                type = AmountType.CoinType,
            ),
            isFiatValue = false,
            fiatValue = "123.123",
            isFiatUnavailable = false,
            isError = false,
            isWarning = false,
            error = TextReference.EMPTY,
            isValuePasted = false,
            onValuePastedTriggerDismiss = {},
        ),
        isSegmentedButtonsEnabled = true,
        selectedButton = 0,
        isRedesignEnabled = false,
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

    val amountStateV2 = amountState.copy(
        isRedesignEnabled = true,
        availableBalance = stringReference("2 130,81231238 USDT • 2 129,12 \$)"),
        availableBalanceCrypto = stringReference("2 130,81231238 USDT"),
        availableBalanceFiat = stringReference(" ${StringsSigns.DOT} 1 232 129,12 $"),
    )

    val amountWithValueFiatState = amountWithValueState.copy(
        amountTextField = amountWithValueState.amountTextField.copy(isFiatValue = false),
    )

    val amountStateV2WithoutRates = amountState.copy(
        amountTextField = amountState.amountTextField.copy(
            fiatAmount = amountState.amountTextField.fiatAmount.copy(
                value = null,
            ),
        ),
    )
    val amountErrorState = amountWithValueState.copy(
        amountTextField = amountWithValueState.amountTextField.copy(
            isError = true,
            error = stringReference("Insufficient funds for transfer"),
        ),
    )
}