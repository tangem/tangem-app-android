package com.tangem.tap.domain.userWalletList.repository

import com.tangem.common.authentication.keystore.AuthenticationPolicy
import com.tangem.common.authentication.keystore.KeystoreManager
import com.tangem.utils.Provider
import javax.crypto.SecretKey

internal class DelegatedKeystoreManager(
    private val keystoreManagerProvider: Provider<KeystoreManager>,
) : KeystoreManager {

    override suspend fun get(
        masterKeyConfig: KeystoreManager.MasterKeyConfig,
        keyAlias: String,
        authenticationPolicy: AuthenticationPolicy,
    ): SecretKey? {
        return keystoreManagerProvider().get(masterKeyConfig, keyAlias, authenticationPolicy)
    }

    override suspend fun get(
        masterKeyConfig: KeystoreManager.MasterKeyConfig,
        keyAliases: Set<String>,
        authenticationPolicy: AuthenticationPolicy,
    ): Map<String, SecretKey> {
        return keystoreManagerProvider().get(masterKeyConfig, keyAliases, authenticationPolicy)
    }

    override suspend fun store(masterKeyConfig: KeystoreManager.MasterKeyConfig, keyAlias: String, key: SecretKey) {
        keystoreManagerProvider().store(masterKeyConfig, keyAlias, key)
    }
}