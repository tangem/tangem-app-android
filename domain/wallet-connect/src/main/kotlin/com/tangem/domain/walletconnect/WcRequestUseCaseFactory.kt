package com.tangem.domain.walletconnect

import arrow.core.Either
import com.tangem.domain.walletconnect.model.WcMethod
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.usecase.method.WcMethodUseCase

interface WcRequestUseCaseFactory {

    suspend fun <T : WcMethodUseCase> createUseCase(request: WcSdkSessionRequest): Either<WcMethod.Unsupported, T>
}