package com.tangem.features.send.impl.presentation.state.previewdata

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType.Coin
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.SendUiStateType
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState
import com.tangem.features.send.impl.presentation.state.fee.FeeType
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

internal object FeeStatePreviewData {
    val feeState = SendStates.FeeState(
        type = SendUiStateType.Amount,
        isPrimaryButtonEnabled = false,
        feeSelectorState = FeeSelectorState.Content(
            fees = TransactionFee.Single(
                normal = Fee.Common(
                    amount = Amount(
                        currencySymbol = "ETH",
                        value = BigDecimal(123.123),
                        decimals = 18,
                        type = Coin,
                    ),
                ),
            ),
            selectedFee = FeeType.Market,
            customValues = persistentListOf(),
        ),
        fee = Fee.Common(
            amount = Amount(
                currencySymbol = "ETH",
                value = BigDecimal(123.123),
                decimals = 18,
                type = Coin,
            ),
        ),
        rate = null,
        appCurrency = AppCurrency(
            code = "USD",
            name = "USD",
            symbol = "$",
            iconSmallUrl = null,
            iconMediumUrl = null,
        ),
        isFeeApproximate = false,
        notifications = persistentListOf(),
    )

    val errorFeeState = feeState.copy(
        feeSelectorState = FeeSelectorState.Error,
    )
}