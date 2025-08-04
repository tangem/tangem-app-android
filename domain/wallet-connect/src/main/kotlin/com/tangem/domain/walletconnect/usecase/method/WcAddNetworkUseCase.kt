package com.tangem.domain.walletconnect.usecase.method

import arrow.core.Either
import com.tangem.domain.walletconnect.model.WcRequestError

interface WcAddNetworkUseCase :
    WcMethodUseCase,
    WcMethodContext {

    suspend fun approve(): Either<WcRequestError, String>
    fun reject()
}