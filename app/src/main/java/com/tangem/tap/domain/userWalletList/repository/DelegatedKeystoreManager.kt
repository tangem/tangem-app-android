package com.tangem.tap.domain.userWalletList.repository

import com.tangem.common.Provider
import com.tangem.common.authentication.KeystoreManager
import javax.crypto.SecretKey

internal class DelegatedKeystoreManager(
    private val keystoreManagerProvider: Provider<KeystoreManager>,
) : KeystoreManager {

    override suspend fun authenticateAndGetKey(keyAlias: String): SecretKey? {
        return keystoreManagerProvider().authenticateAndGetKey(keyAlias)
    }

    override suspend fun storeKey(keyAlias: String, key: SecretKey) {
        keystoreManagerProvider().storeKey(keyAlias, key)
    }
}