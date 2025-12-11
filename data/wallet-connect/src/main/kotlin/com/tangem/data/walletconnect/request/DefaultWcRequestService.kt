package com.tangem.data.walletconnect.request

import com.reown.walletkit.client.Wallet
import com.tangem.data.walletconnect.BuildConfig
import com.tangem.data.walletconnect.respond.DefaultWcRespondService
import com.tangem.data.walletconnect.utils.WC_TAG
import com.tangem.data.walletconnect.utils.WcSdkObserver
import com.tangem.data.walletconnect.utils.WcSdkSessionRequestConverter
import com.tangem.data.walletconnect.utils.getDappOriginUrl
import com.tangem.domain.walletconnect.WcRequestService
import com.tangem.domain.walletconnect.model.WcMethodName
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.joda.time.DateTime
import timber.log.Timber

internal class DefaultWcRequestService(
    private val requestConverters: Set<WcRequestToUseCaseConverter>,
    private val respondService: DefaultWcRespondService,
) : WcSdkObserver, WcRequestService {

    private val _wcRequest: Channel<Pair<WcMethodName, WcSdkSessionRequest>> = Channel(Channel.BUFFERED)
    override val wcRequest: Flow<Pair<WcMethodName, WcSdkSessionRequest>> = _wcRequest.receiveAsFlow()
        .filter { filterDuplicateRequest(request = it.second) }
        .onEach { saveRequest(request = it.second) }

    override fun onSessionRequest(
        sessionRequest: Wallet.Model.SessionRequest,
        verifyContext: Wallet.Model.VerifyContext,
    ) {
        // Triggered when a Dapp sends SessionRequest to sign a transaction or a message
        val sr = WcSdkSessionRequestConverter.convert(
            WcSdkSessionRequestConverter.Input(
                originUrl = verifyContext.getDappOriginUrl(),
                sessionRequest = sessionRequest,
            ),
        )
        val name = requestConverters.firstNotNullOfOrNull { it.toWcMethodName(sr) }
            ?: WcMethodName.Unsupported(sr.request.method)

        if (name is WcMethodName.Unsupported) {
            respondService.rejectRequestNonBlock(sr)
            if (name.raw.startsWith("wallet_")) return
        }

        val sendResult = _wcRequest.trySend(name to sr)
        if (sendResult.isFailure) {
            Timber.tag(WC_TAG).e(
                "Failed to send request to channel: ${sr.request.id} (${sr.request.method})",
            )
        }
    }

    private fun filterDuplicateRequest(request: WcSdkSessionRequest): Boolean {
        val hash = respondService.sessionRequestHash(request)
        val now = DateTime.now().millis
        val expiredMillis = now - respondService.expireDuration.millis
        val cachedRequest = respondService.cachedRequest.updateAndGet {
            it.filterTo(mutableSetOf()) { (millis, _) -> millis > expiredMillis }
        }

        return cachedRequest.none { (_, hashParams) -> hash == hashParams }
    }

    private fun saveRequest(request: WcSdkSessionRequest) {
        // Skip caching in debug builds since filtering is disabled
        if (BuildConfig.DEBUG) {
            return
        }

        val hash = respondService.sessionRequestHash(request)
        val now = DateTime.now().millis
        respondService.cachedRequest.update { it + (now to hash) }
    }
}