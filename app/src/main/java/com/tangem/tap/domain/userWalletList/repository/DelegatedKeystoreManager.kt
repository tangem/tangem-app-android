package com.tangem.tap.domain.userWalletList.repository

import com.tangem.common.authentication.keystore.KeystoreManager
import com.tangem.utils.Provider
import javax.crypto.SecretKey

internal class DelegatedKeystoreManager(
    private val keystoreManagerProvider: Provider<KeystoreManager>,
) : KeystoreManager {

    override suspend fun get(
        masterKeyConfig: KeystoreManager.MasterKeyConfig,
        keyAlias: String,
        forceAuthentication: Boolean,
    ): SecretKey? {
        return keystoreManagerProvider().get(masterKeyConfig, keyAlias, forceAuthentication)
    }

    override suspend fun get(
        masterKeyConfig: KeystoreManager.MasterKeyConfig,
        keyAliases: Set<String>,
        forceAuthentication: Boolean,
    ): Map<String, SecretKey> {
        return keystoreManagerProvider().get(masterKeyConfig, keyAliases, forceAuthentication)
    }

    override suspend fun store(masterKeyConfig: KeystoreManager.MasterKeyConfig, keyAlias: String, key: SecretKey) {
        keystoreManagerProvider().store(masterKeyConfig, keyAlias, key)
    }
}