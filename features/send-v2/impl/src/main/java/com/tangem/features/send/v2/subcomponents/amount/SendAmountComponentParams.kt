package com.tangem.features.send.v2.subcomponents.amount

import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.send.v2.common.PredefinedValues
import com.tangem.features.send.v2.send.SendRoute
import com.tangem.features.send.v2.subcomponents.amount.SendAmountComponent.ModelCallback
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal sealed class SendAmountComponentParams {

    abstract val state: AmountState
    abstract val analyticsCategoryName: String
    abstract val userWallet: UserWallet
    abstract val appCurrency: AppCurrency
    abstract val cryptoCurrencyStatus: CryptoCurrencyStatus
    abstract val predefinedValues: PredefinedValues

    data class AmountParams(
        override val state: AmountState,
        override val analyticsCategoryName: String,
        override val userWallet: UserWallet,
        override val appCurrency: AppCurrency,
        override val cryptoCurrencyStatus: CryptoCurrencyStatus,
        override val predefinedValues: PredefinedValues,
        val isEditMode: Boolean,
        val callback: ModelCallback,
        val currentRoute: Flow<SendRoute.Amount>,
        val isBalanceHidingFlow: StateFlow<Boolean>,
    ) : SendAmountComponentParams()

    data class AmountBlockParams(
        override val state: AmountState,
        override val analyticsCategoryName: String,
        override val userWallet: UserWallet,
        override val appCurrency: AppCurrency,
        override val cryptoCurrencyStatus: CryptoCurrencyStatus,
        override val predefinedValues: PredefinedValues,
        val blockClickEnableFlow: StateFlow<Boolean>,
    ) : SendAmountComponentParams()
}