package com.tangem.features.send.api.subcomponents.amount

import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.navigationButtons.NavigationModelCallback
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.wallets.models.errors.GetUserWalletError

interface SendAmountComponent : ComposableContentComponent {

    fun updateState(amountUM: AmountState)

    interface ModelCallback : NavigationModelCallback {
        fun onAmountResult(amountUM: AmountState, isResetPredefined: Boolean)
        fun onConvertToAnotherToken(lastAmount: String, isEnterInFiatSelected: Boolean)
        fun resetSendNavigation()
        fun onError(error: GetUserWalletError)
    }

    interface Factory : ComponentFactory<SendAmountComponentParams.AmountParams, SendAmountComponent>
}