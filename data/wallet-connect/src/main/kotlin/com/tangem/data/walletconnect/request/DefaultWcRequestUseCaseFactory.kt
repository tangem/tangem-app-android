package com.tangem.data.walletconnect.request

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.walletconnect.utils.WC_TAG
import com.tangem.data.walletconnect.utils.WcNamespaceConverter
import com.tangem.domain.walletconnect.WcAnalyticEvents
import com.tangem.domain.walletconnect.WcRequestUseCaseFactory
import com.tangem.domain.walletconnect.model.HandleMethodError
import com.tangem.domain.walletconnect.model.WcMethod
import com.tangem.domain.walletconnect.model.WcRequestError.Companion.code
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.usecase.method.WcMethodUseCase
import timber.log.Timber
import javax.inject.Inject

internal class DefaultWcRequestUseCaseFactory @Inject constructor(
    private val requestConverters: Set<WcRequestToUseCaseConverter>,
    private val namespaceConverters: Set<WcNamespaceConverter>,
    private val analytics: AnalyticsEventHandler,
) : WcRequestUseCaseFactory {

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : WcMethodUseCase> createUseCase(
        request: WcSdkSessionRequest,
    ): Either<HandleMethodError, T> {
        val useCase = requestConverters
            .find { it.toWcMethodName(request) != null }
            ?.toUseCase(request)
            ?: HandleMethodError.UnknownError("Failed to create WcUseCase").left()

        val result = useCase.fold(
            ifLeft = {
                Timber.tag(WC_TAG).e("$it")
                it.left()
            },
            ifRight = { (it as? T)?.right() ?: HandleMethodError.Unsupported(WcMethod.Unsupported(request)).left() },
        )
        result.onLeft { logError(it, request) }
        return result
    }

    private fun logError(error: HandleMethodError, request: WcSdkSessionRequest) {
        val blockchainName = namespaceConverters
            .firstNotNullOfOrNull { it.toBlockchain(request.chainId.orEmpty()) }
            ?.getCoinName().orEmpty()
        val event = WcAnalyticEvents.SignatureRequestReceivedFailed(
            rawRequest = request,
            errorCode = error.code().orEmpty(),
            blockchain = blockchainName,
        )
        analytics.send(event)
    }
}