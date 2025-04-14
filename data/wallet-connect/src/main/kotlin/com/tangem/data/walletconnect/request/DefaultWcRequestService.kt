package com.tangem.data.walletconnect.request

import com.reown.walletkit.client.Wallet
import com.tangem.data.walletconnect.utils.WcSdkObserver
import com.tangem.data.walletconnect.utils.WcSdkSessionRequestConverter
import com.tangem.domain.walletconnect.usecase.WcMethodUseCase
import com.tangem.domain.walletconnect.usecase.WcUseCasesFlowProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal class DefaultWcRequestService(
    private val requestConverters: Set<WcRequestToUseCaseConverter>,
    private val scope: CoroutineScope,
) : WcSdkObserver, WcUseCasesFlowProvider {

    private val _useCases: Channel<WcMethodUseCase> = Channel(Channel.BUFFERED)
    override val useCases = _useCases.receiveAsFlow()

    override fun onSessionRequest(
        sessionRequest: Wallet.Model.SessionRequest,
        verifyContext: Wallet.Model.VerifyContext,
    ) {
        // Triggered when a Dapp sends SessionRequest to sign a transaction or a message
        val sr = WcSdkSessionRequestConverter.convert(sessionRequest)
        scope.launch {
            val useCase = requestConverters.firstNotNullOfOrNull { it.toUseCase(sr) }
                ?: return@launch // todo(wc) handle unsupported
            _useCases.trySend(useCase)
        }
    }
}