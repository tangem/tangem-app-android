package com.tangem.tap.domain.userWalletList.repository

import com.tangem.common.services.secure.SecureStorage

/**
 * A decorator for [SecureStorage] that facilitates data migration between two storages.
 *
 * @property featureStorage The primary storage, which will eventually contain all user data.
 * @property cardSdkStorage The SDK's storage where user data might have been previously stored.
 */
internal class UserWalletsKeysStoreDecorator(
    private val featureStorage: SecureStorage,
    private val cardSdkStorage: SecureStorage,
) : SecureStorage by featureStorage {

    override fun delete(account: String) {
        featureStorage.delete(account)
        cardSdkStorage.delete(account)
    }

    override fun get(account: String): ByteArray? {
        var data = featureStorage.get(account)

        if (data == null) {
            data = cardSdkStorage.get(account) ?: return null
            featureStorage.store(data, account)
        }

        return data
    }
}