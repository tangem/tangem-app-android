package com.tangem.tap.network.auth

import com.tangem.common.extensions.toHexString
import com.tangem.lib.auth.LazyAuthProvider
import com.tangem.tap.proxy.AppStateHolder

class LazyAuthProviderImpl(private val appStateHolder: AppStateHolder) : LazyAuthProvider {

    override fun getCardPublicKeyProvider(): () -> String {
        return { appStateHolder.getActualCard()?.cardPublicKey?.toHexString() ?: "" }
    }

    override fun getCardIdProvider(): () -> String {
        return { appStateHolder.getActualCard()?.cardId ?: "" }
    }
}