package com.tangem.tap.domain.userWalletList.repository

import com.tangem.common.authentication.KeystoreManager
import com.tangem.utils.Provider
import javax.crypto.SecretKey

internal class DelegatedKeystoreManager(
    private val keystoreManagerProvider: Provider<KeystoreManager>,
) : KeystoreManager {

    override suspend fun get(keyAlias: String): SecretKey? {
        return keystoreManagerProvider().get(keyAlias)
    }

    override suspend fun get(keyAliases: Collection<String>): Map<String, SecretKey> {
        return keystoreManagerProvider().get(keyAliases)
    }

    override suspend fun store(keyAlias: String, key: SecretKey) {
        return keystoreManagerProvider().store(keyAlias, key)
    }
}