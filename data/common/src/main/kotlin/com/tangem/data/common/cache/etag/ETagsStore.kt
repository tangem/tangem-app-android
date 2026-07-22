package com.tangem.data.common.cache.etag

import com.tangem.domain.models.wallet.UserWalletId

/**
 * Interface for working with ETag (Entity Tag), which is used for data caching and validation.
 *
[REDACTED_AUTHOR]
 */
interface ETagsStore {

    /**
     * Retrieves the stored ETag value for the specified wallet and key
     *
     * @param userWalletId identifier of the user wallet
     * @param key          the key for which to get the ETag value
     */
    suspend fun getSyncOrNull(userWalletId: UserWalletId, key: Key): String?

    /**
     * Stores the ETag value for the specified wallet and key
     *
     * @param userWalletId identifier of the user wallet
     * @param key          the key for which to get the ETag value
     */
    suspend fun store(userWalletId: UserWalletId, key: Key, value: String)

    /**
     * Clears the stored ETag value for the specified wallet and key
     *
     * @param userWalletId identifier of the user wallet
     * @param key          the key for which to get the ETag value
     */
    suspend fun clear(userWalletId: UserWalletId, key: Key)

    /**
     * Clears all stored ETag values (every [Key]) for the specified wallets in a single operation
     *
     * @param userWalletIds identifiers of the user wallets
     */
    suspend fun clear(userWalletIds: List<UserWalletId>)

    /** Enumeration of possible keys for storing ETag values */
    enum class Key {
        WalletAccounts,
        UserTokens,
        AddressBook,
    }
}