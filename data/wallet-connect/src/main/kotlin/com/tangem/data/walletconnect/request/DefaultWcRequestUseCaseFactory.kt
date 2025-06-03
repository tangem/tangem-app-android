package com.tangem.data.walletconnect.request

import com.tangem.domain.walletconnect.WcRequestUseCaseFactory
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.usecase.method.WcMethodUseCase
import javax.inject.Inject

internal class DefaultWcRequestUseCaseFactory @Inject constructor(
    private val requestConverters: Set<WcRequestToUseCaseConverter>,
) : WcRequestUseCaseFactory {

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : WcMethodUseCase> createUseCase(request: WcSdkSessionRequest): T {
        return requestConverters.firstNotNullOf { it.toUseCase(request) } as? T
            ?: error("Should check request before in WcRouting")
    }
}