package com.tangem.features.swap.v2.impl.choosetoken.fromSupported

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.swap.v2.api.choosetoken.SwapChooseTokenNetworkTrigger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Listens to swap token selection
 */
interface SwapChooseTokenNetworkListener {
    val swapChooseTokenNetworkResultFlow: SharedFlow<CryptoCurrency>
}

@Singleton
internal class DefaultSwapChooseTokenNetworkTrigger @Inject constructor() :
    SwapChooseTokenNetworkTrigger,
    SwapChooseTokenNetworkListener {

    override val swapChooseTokenNetworkResultFlow: SharedFlow<CryptoCurrency>
    field = MutableSharedFlow<CryptoCurrency>()

    override suspend fun trigger(result: CryptoCurrency) {
        swapChooseTokenNetworkResultFlow.emit(result)
    }
}