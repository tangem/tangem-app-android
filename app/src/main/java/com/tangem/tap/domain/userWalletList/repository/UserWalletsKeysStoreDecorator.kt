package com.tangem.tap.domain.userWalletList.repository

import com.tangem.common.services.secure.SecureStorage
import com.tangem.utils.Provider

/**
 * A decorator for [SecureStorage] that facilitates data migration between two storages.
 *
 * @property featureStorage The primary storage, which will eventually contain all user data.
 * @property cardSdkStorageProvider The SDK's storage where user data might have been previously stored.
 */
internal class UserWalletsKeysStoreDecorator(
    private val featureStorage: SecureStorage,
    private val cardSdkStorageProvider: Provider<SecureStorage>,
) : SecureStorage by featureStorage {

    override fun delete(account: String) {
        featureStorage.delete(account)
        cardSdkStorageProvider().delete(account)
    }

    override fun get(account: String): ByteArray? {
        var data = featureStorage.get(account)

        if (data == null) {
            data = cardSdkStorageProvider().get(account) ?: return null
            featureStorage.store(data, account)
        }

        return data
    }
}