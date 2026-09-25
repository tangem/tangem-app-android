package com.tangem.domain.walletconnect.usecase.method

import arrow.core.Either
import com.tangem.domain.models.network.Network
import com.tangem.domain.walletconnect.model.HandleMethodError
import com.tangem.domain.walletconnect.model.WcRequestError

interface WcSwitchNetworkUseCase :
    WcMethodUseCase,
    WcMethodContext {

    suspend operator fun invoke(): Either<HandleMethodError, SwitchNetwork>

    /** EIP-3326: a switch to a chain the session already has is answered with a `null` (empty) success. */
    suspend fun approve(): Either<WcRequestError, String>
    fun reject()

    data class SwitchNetwork(
        val network: Network,
        val isExistInWcSession: Boolean,
    )
}