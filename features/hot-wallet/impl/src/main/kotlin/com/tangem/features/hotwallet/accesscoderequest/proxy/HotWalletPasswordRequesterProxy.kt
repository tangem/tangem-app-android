package com.tangem.features.hotwallet.accesscoderequest.proxy

import com.tangem.features.hotwallet.HotWalletPasswordRequester
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HotWalletPasswordRequesterProxy @Inject constructor() : HotWalletPasswordRequester {

    val componentRequester = MutableStateFlow<HotWalletPasswordRequester?>(null)

    override suspend fun wrongPassword() {
        call { wrongPassword() }
    }

    override suspend fun requestPassword(hasBiometry: Boolean): HotWalletPasswordRequester.Result =
        call { requestPassword(hasBiometry) }

    override suspend fun dismiss() {
        call { dismiss() }
    }

    private suspend fun <T> call(block: suspend HotWalletPasswordRequester.() -> T): T {
        return withTimeout(timeMillis = 1000) {
            componentRequester.filterNotNull().first()
        }.block()
    }
}