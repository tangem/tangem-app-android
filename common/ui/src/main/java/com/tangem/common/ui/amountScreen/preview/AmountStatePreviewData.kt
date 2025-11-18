package com.tangem.common.ui.amountScreen.preview

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.tangem.common.ui.R
import com.tangem.common.ui.account.AccountNameUM
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.amountScreen.models.AmountFieldModel
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.tokens.model.AmountType
import com.tangem.utils.StringsSigns
import java.math.BigDecimal

object AmountStatePreviewData {

    val emptyState = AmountState.Empty

    val amountState = AmountState.Data(
        isPrimaryButtonEnabled = false,
        accountTitleUM = AccountTitleUM.Text(stringReference("Family Wallet")),
        availableBalanceCrypto = stringReference("2 130,81231238 USDT"),
        availableBalanceFiat = stringReference("1 232 129,12 \$"),
        tokenIconState = CurrencyIconState.Loading,
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
    )

    private val amountWithValueState = amountState.copy(
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
        availableBalanceCrypto = stringReference("2 130,81231238 USDT"),
        availableBalanceFiat = stringReference(" ${StringsSigns.DOT} 1 232 129,12 $"),
    )

    val amountStateV2WithoutRates = amountState.copy(
        amountTextField = amountState.amountTextField.copy(
            fiatAmount = amountState.amountTextField.fiatAmount.copy(
                value = null,
            ),
        ),
    )

    val amountStateV2Accounts = amountState.copy(
        accountTitleUM = AccountTitleUM.Account(
            name = AccountNameUM.DefaultMain.value,
            icon = CryptoPortfolioIcon.ofDefaultCustomAccount().toUM(),
            prefixText = resourceReference(R.string.common_from),
        ),
        availableBalanceCrypto = stringReference("2 130,81231238 USDT"),
        availableBalanceFiat = stringReference(" ${StringsSigns.DOT} 1 232 129,12 $"),
    )

    val amountErrorState = amountWithValueState.copy(
        amountTextField = amountWithValueState.amountTextField.copy(
            isError = true,
            error = stringReference("Insufficient funds for transfer"),
        ),
    )
}