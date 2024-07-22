package com.tangem.features.send.impl.presentation.state.previewdata

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType.Coin
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.SendUiStateType
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState
import com.tangem.features.send.impl.presentation.state.fee.FeeType
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

internal object FeeStatePreviewData {

    private val fee = Fee.Common(
        amount = Amount(
            currencySymbol = "MATIC",
            value = BigDecimal(0.159806),
            decimals = 18,
            type = Coin,
        ),
    )

    private val singleFee = TransactionFee.Single(normal = fee)

    private val multipleFees = TransactionFee.Choosable(
        normal = fee,
        minimum = fee,
        priority = fee.copy(fee.amount.copy(value = BigDecimal(0.159824))),
    )

    private val customValue = SendTextField.CustomFee(
        value = "0.159834",
        onValueChange = {},
        keyboardOptions = KeyboardOptions.Default,
        keyboardActions = KeyboardActions.Default,
        symbol = "MATIC",
        decimals = 18,
        title = stringReference("Fee up to"),
        footer = stringReference("Maximum commission amount"),
        label = stringReference("0.41 \$"),
        isReadonly = false,
    )

    private val customValues = persistentListOf(
        customValue,
        customValue.copy(
            value = "400",
            symbol = "GWEI",
            title = stringReference("Gas price"),
            footer = stringReference("Gas Price impacts transaction speed; too low, it may not process"),
            label = null,
        ),
        customValue.copy(
            value = "31400",
            symbol = "",
            title = stringReference("Gas limit"),
            footer = stringReference("Gas Limit is auto-calculated; raise it during network congestion"),
            label = null,
        ),
    )

    private val feeSelector = FeeSelectorState.Content(
        fees = singleFee,
        selectedFee = FeeType.Market,
        customValues = persistentListOf(),
    )

    val feeState = SendStates.FeeState(
        type = SendUiStateType.Fee,
        isPrimaryButtonEnabled = false,
        feeSelectorState = feeSelector,
        fee = fee,
        rate = BigDecimal.ONE,
        appCurrency = AppCurrency.Default,
        isFeeApproximate = false,
        notifications = persistentListOf(),
        isCustomSelected = false,
        isFeeConvertibleToFiat = true,
    )

    val feeChoosableState = feeState.copy(
        feeSelectorState = feeSelector.copy(
            fees = multipleFees,
            customValues = customValues,
        ),
    )

    val feeCustomState = feeState.copy(
        feeSelectorState = feeSelector.copy(
            fees = multipleFees,
            customValues = customValues,
            selectedFee = FeeType.Custom,
        ),
        isCustomSelected = true,
    )

    val errorFeeState = feeState.copy(
        feeSelectorState = FeeSelectorState.Error.NetworkError,
    )
}
