package com.tangem.domain.walletconnect

import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.usecase.WcMethodUseCase

interface WcRequestUseCaseFactory {

    suspend fun <T : WcMethodUseCase> createUseCase(request: WcSdkSessionRequest): T
}