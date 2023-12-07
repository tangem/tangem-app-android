package com.tangem.feature.wallet.presentation.wallet.viewmodels.intents

import com.tangem.core.ui.extensions.stringReference
import com.tangem.feature.wallet.presentation.wallet.state.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state2.utils.WalletEventSender
import javax.inject.Inject

internal interface VisaWalletIntents {

    fun onDepositClick()

    fun onBalancesAndLimitsClick()
}

internal class VisaWalletIntentsImplementor @Inject constructor(
    private val eventSender: WalletEventSender,
) : VisaWalletIntents {

    override fun onDepositClick() {
        eventSender.send(WalletEvent.ShowToast(stringReference(value = "Not implemented yet")))
    }

    override fun onBalancesAndLimitsClick() {
        eventSender.send(WalletEvent.ShowToast(stringReference(value = "Not implemented yet")))
    }
}