package com.tangem.domain.walletconnect.usecase.method

import arrow.core.Either
import com.tangem.domain.models.network.Network
import com.tangem.domain.walletconnect.model.HandleMethodError

interface WcSwitchNetworkUseCase :
    WcMethodUseCase,
    WcMethodContext {

    suspend operator fun invoke(): Either<HandleMethodError, SwitchNetwork>
    fun reject()

    data class SwitchNetwork(
        val network: Network,
    )
}