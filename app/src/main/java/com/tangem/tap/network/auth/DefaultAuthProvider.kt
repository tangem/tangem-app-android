package com.tangem.tap.network.auth

import com.tangem.common.extensions.toHexString
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.tap.proxy.AppStateHolder

internal class DefaultAuthProvider(private val appStateHolder: AppStateHolder) : AuthProvider {

    override fun getCardPublicKey(): String {
        return appStateHolder.scanResponse?.card?.cardPublicKey?.toHexString() ?: ""
    }

    override fun getCardId(): String {
        return appStateHolder.scanResponse?.card?.cardId ?: ""
    }
}
