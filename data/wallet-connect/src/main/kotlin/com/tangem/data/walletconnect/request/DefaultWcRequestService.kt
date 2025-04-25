package com.tangem.data.walletconnect.request

import com.reown.walletkit.client.Wallet
import com.tangem.data.walletconnect.utils.WcSdkObserver
import com.tangem.data.walletconnect.utils.WcSdkSessionRequestConverter
import com.tangem.domain.walletconnect.WcRequestService
import com.tangem.domain.walletconnect.model.WcMethodName
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

internal class DefaultWcRequestService(
    private val requestConverters: Set<WcRequestToUseCaseConverter>,
) : WcSdkObserver, WcRequestService {

    private val _wcRequest: Channel<Pair<WcMethodName, WcSdkSessionRequest>> = Channel(Channel.BUFFERED)
    override val wcRequest: Flow<Pair<WcMethodName, WcSdkSessionRequest>> = _wcRequest.receiveAsFlow()

    override fun onSessionRequest(
        sessionRequest: Wallet.Model.SessionRequest,
        verifyContext: Wallet.Model.VerifyContext,
    ) {
        // Triggered when a Dapp sends SessionRequest to sign a transaction or a message
        val sr = WcSdkSessionRequestConverter.convert(sessionRequest)
        val name = requestConverters.firstNotNullOfOrNull { it.toWcMethodName(sr) }
            ?: WcMethodName.Unsupported(sr.request.method)
        _wcRequest.trySend(name to sr)
    }
}