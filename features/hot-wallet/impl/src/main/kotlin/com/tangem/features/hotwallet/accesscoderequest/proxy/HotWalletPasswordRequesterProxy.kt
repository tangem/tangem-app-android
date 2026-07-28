package com.tangem.features.hotwallet.accesscoderequest.proxy

import com.tangem.domain.wallets.hot.HotWalletPasswordRequester
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HotWalletPasswordRequesterProxy @Inject constructor() : HotWalletPasswordRequester {

    val componentRequester = MutableStateFlow<HotWalletPasswordRequester?>(null)

    override suspend fun wrongPassword(attemptRequest: HotWalletPasswordRequester.AttemptRequest) =
        call { wrongPassword(attemptRequest) }

    override suspend fun successfulAuthentication(attemptRequest: HotWalletPasswordRequester.AttemptRequest) =
        call { successfulAuthentication(attemptRequest) }

    override suspend fun requestPassword(
        attemptRequest: HotWalletPasswordRequester.AttemptRequest,
    ): HotWalletPasswordRequester.Result = call { requestPassword(attemptRequest) }

    override suspend fun dismiss() = call { dismiss() }

    private suspend fun <T> call(block: suspend HotWalletPasswordRequester.() -> T): T {
        return withTimeout(timeMillis = 1000) {
            componentRequester.filterNotNull().first()
        }.block()
    }
}