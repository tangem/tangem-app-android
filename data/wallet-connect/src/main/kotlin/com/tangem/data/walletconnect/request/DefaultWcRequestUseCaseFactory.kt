package com.tangem.data.walletconnect.request

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.walletconnect.WcRequestUseCaseFactory
import com.tangem.domain.walletconnect.model.WcMethod
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.usecase.method.WcMethodUseCase
import javax.inject.Inject

internal class DefaultWcRequestUseCaseFactory @Inject constructor(
    private val requestConverters: Set<WcRequestToUseCaseConverter>,
) : WcRequestUseCaseFactory {

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : WcMethodUseCase> createUseCase(
        request: WcSdkSessionRequest,
    ): Either<WcMethod.Unsupported, T> {
        val useCase = requestConverters.firstNotNullOfOrNull { converter -> converter.toUseCase(request) }
        return (useCase as? T)?.let { useCase.right() } ?: WcMethod.Unsupported(request).left()
    }
}