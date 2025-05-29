package com.tangem.domain.walletconnect.usecase.method

import arrow.core.Either

interface WcAddNetworkUseCase :
    WcMethodUseCase,
    WcMethodContext {

    suspend fun approve(): Either<Throwable, Unit>
    fun reject()
}