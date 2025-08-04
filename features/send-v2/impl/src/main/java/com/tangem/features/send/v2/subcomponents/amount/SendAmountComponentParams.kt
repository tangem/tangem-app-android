package com.tangem.features.send.v2.subcomponents.amount

import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.send.v2.api.entity.PredefinedValues
import com.tangem.features.send.v2.common.CommonSendRoute
import com.tangem.features.send.v2.subcomponents.amount.SendAmountComponent.ModelCallback
import kotlinx.coroutines.flow.StateFlow

internal sealed class SendAmountComponentParams {

    abstract val state: AmountState
    abstract val analyticsCategoryName: String
    abstract val userWalletId: UserWalletId
    abstract val appCurrency: AppCurrency
    abstract val predefinedValues: PredefinedValues
    abstract val isRedesignEnabled: Boolean
    abstract val cryptoCurrency: CryptoCurrency
    abstract val cryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>

    data class AmountParams(
        override val state: AmountState,
        override val analyticsCategoryName: String,
        override val userWalletId: UserWalletId,
        override val appCurrency: AppCurrency,
        override val predefinedValues: PredefinedValues,
        override val isRedesignEnabled: Boolean,
        override val cryptoCurrency: CryptoCurrency,
        override val cryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>,
        val callback: ModelCallback,
        val currentRoute: StateFlow<CommonSendRoute>,
        val isBalanceHidingFlow: StateFlow<Boolean>,
    ) : SendAmountComponentParams()

    data class AmountBlockParams(
        override val state: AmountState,
        override val analyticsCategoryName: String,
        override val appCurrency: AppCurrency,
        override val userWalletId: UserWalletId,
        override val predefinedValues: PredefinedValues,
        override val isRedesignEnabled: Boolean,
        override val cryptoCurrency: CryptoCurrency,
        override val cryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>,
        val userWallet: UserWallet,
        val blockClickEnableFlow: StateFlow<Boolean>,
    ) : SendAmountComponentParams()
}