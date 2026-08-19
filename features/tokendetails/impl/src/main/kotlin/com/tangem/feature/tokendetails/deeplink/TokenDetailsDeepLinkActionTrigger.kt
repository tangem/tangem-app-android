package com.tangem.feature.tokendetails.deeplink

import com.tangem.features.tokendetails.deeplink.ExpressDeepLinkListener
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

interface TokenDetailsDeepLinkActionTrigger {
    suspend fun trigger(txId: String)
}

@Singleton
internal class DefaultExpressDeepLinkTrigger @Inject constructor() :
    TokenDetailsDeepLinkActionTrigger,
    ExpressDeepLinkListener {

    override val actionFlow: SharedFlow<String>
        field = MutableSharedFlow<String>()

    override suspend fun trigger(txId: String) {
        actionFlow.emit(txId)
    }
}