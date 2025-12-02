package com.tangem.data.walletconnect.request

import com.reown.walletkit.client.Wallet
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toHexString
import com.tangem.data.walletconnect.BuildConfig
import com.tangem.data.walletconnect.respond.WcRespondService
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
import org.joda.time.Duration
import timber.log.Timber

internal class DefaultWcRequestService(
    private val requestConverters: Set<WcRequestToUseCaseConverter>,
    private val respondService: WcRespondService,
) : WcSdkObserver, WcRequestService {

    private val expireDuration = Duration.standardSeconds(120)
    private val cachedRequest = MutableStateFlow<Set<Pair<Long, String>>>(setOf())
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
        val hash = request.request.params.calculateSha256().toHexString()
        val now = DateTime.now().millis
        val expiredMillis = now - expireDuration.millis
        val cachedRequest = cachedRequest.updateAndGet {
            it.filterTo(mutableSetOf()) { (millis, _) -> millis > expiredMillis }
        }

        return cachedRequest.none { (_, hashParams) -> hash == hashParams }
    }

    private fun saveRequest(request: WcSdkSessionRequest) {
        // Skip caching in debug builds since filtering is disabled
        if (BuildConfig.DEBUG) {
            return
        }

        val hash = request.request.params.calculateSha256().toHexString()
        val now = DateTime.now().millis
        cachedRequest.update { it + (now to hash) }
    }
}