package com.tangem.feature.tokendetails.deeplink

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

interface TokenDetailsDeepLinkActionTrigger {
    suspend fun trigger(txId: String)
}

interface TokenDetailsDeepLinkActionListener {
    val tokenDetailsActionFlow: SharedFlow<String>
}

@Singleton
internal class DefaultTokenDetailsDeepLinkActionTrigger @Inject constructor() :
    TokenDetailsDeepLinkActionTrigger,
    TokenDetailsDeepLinkActionListener {

    override val tokenDetailsActionFlow: SharedFlow<String>
        field = MutableSharedFlow<String>()

    override suspend fun trigger(txId: String) {
        tokenDetailsActionFlow.emit(txId)
    }
}