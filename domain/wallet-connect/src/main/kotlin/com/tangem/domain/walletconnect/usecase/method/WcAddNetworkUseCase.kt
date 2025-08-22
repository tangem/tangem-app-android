package com.tangem.domain.walletconnect.usecase.method

import arrow.core.Either
import com.tangem.domain.models.network.Network
import com.tangem.domain.walletconnect.model.HandleMethodError
import com.tangem.domain.walletconnect.model.WcRequestError

interface WcAddNetworkUseCase :
    WcMethodUseCase,
    WcMethodContext {

    suspend operator fun invoke(): Either<HandleMethodError, AddNetwork>
    suspend fun approve(): Either<WcRequestError, String>
    fun reject()

    data class AddNetwork(
        val network: Network,
        val isExistInWcSession: Boolean,
    )
}