package com.tangem.feature.swap.ui.preview

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.common.ui.account.AccountNameUM
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.amountScreen.models.AmountFieldModel
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.tokens.model.AmountType
import com.tangem.feature.swap.models.SwapCardState
import com.tangem.feature.swap.models.TransactionCardType
import com.tangem.feature.swap.presentation.R
import java.math.BigDecimal

internal object SwapTransactionCardPreview {

    val sendCard = SwapCardState.SwapCardData(
        type = TransactionCardType.Inputtable(
            onFocusChanged = {},
            inputError = TransactionCardType.InputError.Empty,
            accountTitleUM = AccountTitleUM.Account(
                prefixText = stringReference("From"),
                name = AccountNameUM.DefaultMain.value,
                icon = CryptoPortfolioIconConverter.convert(CryptoPortfolioIcon.ofDefaultCustomAccount()),
            ),
            isEnabled = true,
        ),
        amountEquivalent = stringReference("1 000 000"),
        currencyIconState = CurrencyIconState.Loading,
        tokenSymbol = stringReference("DAI"),
        balance = stringReference("Balance: 123123123.123123 DAI"),
        isBalanceHidden = false,
        appCurrency = AppCurrency.Default,
        amountField = AmountFieldModel(
            value = "100",
            onValueChange = {},
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Number),
            keyboardActions = KeyboardActions(),
            cryptoAmount = Amount(currencySymbol = "DAI", value = BigDecimal("100"), decimals = 18),
            fiatAmount = Amount(
                currencySymbol = "$",
                value = BigDecimal("100"),
                decimals = 2,
                type = AmountType.FiatType("USD"),
            ),
            isFiatValue = false,
            fiatValue = "$100.00",
            isFiatUnavailable = false,
            isValuePasted = false,
            onValuePastedTriggerDismiss = {},
            isError = false,
            isWarning = false,
            error = TextReference.EMPTY,
        ),
    )

    val receiveCard = SwapCardState.SwapCardData(
        type = TransactionCardType.ReadOnly(
            accountTitleUM = AccountTitleUM.Account(
                prefixText = stringReference("To"),
                name = AccountNameUM.DefaultMain.value,
                icon = CryptoPortfolioIconConverter.convert(CryptoPortfolioIcon.ofDefaultCustomAccount()),
            ),
        ),
        amountEquivalent = stringReference("1 000 000"),
        currencyIconState = CurrencyIconState.Loading,
        tokenSymbol = stringReference("DAI"),
        balance = stringReference("Balance: 33333 DAI"),
        isBalanceHidden = false,
        appCurrency = AppCurrency.Default,
        amountField = AmountFieldModel(
            value = "100",
            onValueChange = {},
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Number),
            keyboardActions = KeyboardActions(),
            cryptoAmount = Amount(currencySymbol = "DAI", value = BigDecimal("100"), decimals = 18),
            fiatAmount = Amount(
                currencySymbol = "$",
                value = BigDecimal("100"),
                decimals = 2,
                type = AmountType.FiatType("USD"),
            ),
            isFiatValue = false,
            fiatValue = "$100.00",
            isFiatUnavailable = false,
            isValuePasted = false,
            onValuePastedTriggerDismiss = {},
            isError = false,
            isWarning = false,
            error = TextReference.EMPTY,
        ),
    )

    val emptyReadOnlyCard = SwapCardState.Empty(
        type = TransactionCardType.ReadOnly(
            accountTitleUM = AccountTitleUM.Text(title = resourceReference(R.string.swapping_to_title)),
        ),
        amountEquivalent = stringReference("$0.00"),
        amountField = null,
    )

    val emptyInputtableCard = SwapCardState.Empty(
        type = TransactionCardType.Inputtable(
            onFocusChanged = {},
            inputError = TransactionCardType.InputError.Empty,
            accountTitleUM = AccountTitleUM.Text(title = resourceReference(R.string.swapping_from_title)),
            isEnabled = false,
        ),
        amountEquivalent = stringReference("$0.00"),
        amountField = null,
    )

    val loadingCard = SwapCardState.Loading(
        type = TransactionCardType.Inputtable(
            onFocusChanged = {},
            inputError = TransactionCardType.InputError.Empty,
            accountTitleUM = AccountTitleUM.Text(title = resourceReference(R.string.swapping_to_title)),
            isEnabled = false,
        ),
    )
}