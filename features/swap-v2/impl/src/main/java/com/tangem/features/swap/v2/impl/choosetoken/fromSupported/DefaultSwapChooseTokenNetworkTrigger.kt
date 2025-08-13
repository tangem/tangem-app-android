package com.tangem.features.swap.v2.impl.choosetoken.fromSupported

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapCurrencies
import com.tangem.features.swap.v2.api.choosetoken.SwapChooseTokenNetworkListener
import com.tangem.features.swap.v2.api.choosetoken.SwapChooseTokenNetworkTrigger
import com.tangem.features.swap.v2.api.choosetoken.SwapChooseTokenTriggerData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultSwapChooseTokenNetworkTrigger @Inject constructor() :
    SwapChooseTokenNetworkTrigger,
    SwapChooseTokenNetworkListener {

    override val swapChooseTokenNetworkResultFlow: SharedFlow<SwapChooseTokenTriggerData>
    field = MutableSharedFlow<SwapChooseTokenTriggerData>()

    override suspend fun trigger(
        swapCurrencies: SwapCurrencies,
        cryptoCurrency: CryptoCurrency,
        shouldResetNavigation: Boolean,
    ) {
        swapChooseTokenNetworkResultFlow.emit(
            SwapChooseTokenTriggerData(
                swapCurrencies = swapCurrencies,
                cryptoCurrency = cryptoCurrency,
                shouldResetNavigation = shouldResetNavigation,
            ),
        )
    }
}