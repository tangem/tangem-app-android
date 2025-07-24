package com.tangem.features.swap.v2.impl.choosetoken.fromSupported

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapCurrencies
import com.tangem.features.swap.v2.api.choosetoken.SwapChooseTokenNetworkListener
import com.tangem.features.swap.v2.api.choosetoken.SwapChooseTokenNetworkTrigger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultSwapChooseTokenNetworkTrigger @Inject constructor() :
    SwapChooseTokenNetworkTrigger,
    SwapChooseTokenNetworkListener {

    override val swapChooseTokenNetworkResultFlow: SharedFlow<Pair<SwapCurrencies, CryptoCurrency>>
    field = MutableSharedFlow<Pair<SwapCurrencies, CryptoCurrency>>()

    override suspend fun trigger(swapCurrencies: SwapCurrencies, cryptoCurrency: CryptoCurrency) {
        swapChooseTokenNetworkResultFlow.emit(swapCurrencies to cryptoCurrency)
    }
}