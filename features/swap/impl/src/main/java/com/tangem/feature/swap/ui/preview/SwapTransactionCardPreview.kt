package com.tangem.feature.swap.ui.preview

import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.common.ui.account.AccountNameUM
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.feature.swap.models.SwapCardState
import com.tangem.feature.swap.models.TransactionCardType
import com.tangem.feature.swap.presentation.R

internal object SwapTransactionCardPreview {

    val sendCard = SwapCardState.SwapCardData(
        type = TransactionCardType.Inputtable(
            onAmountChanged = {},
            onFocusChanged = {},
            inputError = TransactionCardType.InputError.Empty,
            accountTitleUM = AccountTitleUM.Account(
                prefixText = stringReference("From"),
                name = AccountNameUM.DefaultMain.value,
                icon = CryptoPortfolioIconConverter.convert(CryptoPortfolioIcon.ofDefaultCustomAccount()),
            ),
            isEnabled = true,
        ),
        amountTextFieldValue = TextFieldValue(),
        amountEquivalent = stringReference("1 000 000"),
        currencyIconState = CurrencyIconState.Loading,
        tokenSymbol = stringReference("DAI"),
        balance = "123123123.123123",
        isBalanceHidden = false,
    )

    val receiveCard = SwapCardState.SwapCardData(
        type = TransactionCardType.ReadOnly(
            accountTitleUM = AccountTitleUM.Account(
                prefixText = stringReference("To"),
                name = AccountNameUM.DefaultMain.value,
                icon = CryptoPortfolioIconConverter.convert(CryptoPortfolioIcon.ofDefaultCustomAccount()),
            ),
        ),
        amountTextFieldValue = TextFieldValue(),
        amountEquivalent = stringReference("1 000 000"),
        currencyIconState = CurrencyIconState.Loading,
        tokenSymbol = stringReference("DAI"),
        balance = "33333",
        isBalanceHidden = false,
    )

    val emptyReadOnlyCard = SwapCardState.Empty(
        type = TransactionCardType.ReadOnly(
            accountTitleUM = AccountTitleUM.Text(title = resourceReference(R.string.swapping_to_title)),
        ),
        amountEquivalent = stringReference("$0.00"),
        amountTextFieldValue = null,
    )

    val emptyInputtableCard = SwapCardState.Empty(
        type = TransactionCardType.Inputtable(
            onAmountChanged = {},
            onFocusChanged = {},
            inputError = TransactionCardType.InputError.Empty,
            accountTitleUM = AccountTitleUM.Text(title = resourceReference(R.string.swapping_from_title)),
            isEnabled = false,
        ),
        amountEquivalent = stringReference("$0.00"),
        amountTextFieldValue = null,
    )

    val loadingCard = SwapCardState.Loading(
        type = TransactionCardType.Inputtable(
            onAmountChanged = {},
            onFocusChanged = {},
            inputError = TransactionCardType.InputError.Empty,
            accountTitleUM = AccountTitleUM.Text(title = resourceReference(R.string.swapping_to_title)),
            isEnabled = false,
        ),
    )
}