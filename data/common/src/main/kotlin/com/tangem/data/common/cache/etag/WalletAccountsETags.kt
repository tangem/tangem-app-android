package com.tangem.data.common.cache.etag

import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ETAG_HEADER
import com.tangem.domain.models.wallet.UserWalletId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The wallet-level ETag of the accounts document. The backend versions the accounts and the token lists
 * together, so `GET/PUT /accounts` and `PUT /tokens` all carry — and are conditioned by — this one value:
 * every reader and writer of it goes through here instead of reimplementing the header handling.
 */
@Singleton
class WalletAccountsETags @Inject constructor(
    private val eTagsStore: ETagsStore,
) {

    suspend fun getSyncOrNull(userWalletId: UserWalletId): String? {
        return eTagsStore.getSyncOrNull(userWalletId = userWalletId, key = ETagsStore.Key.WalletAccounts)
    }

    /**
     * Stores the response's ETag when the header is present; a response without it keeps the stored value —
     * a read invalidates nothing.
     */
    suspend fun store(userWalletId: UserWalletId, apiResponse: ApiResponse<*>) {
        val eTag = apiResponse.headers[ETAG_HEADER]?.firstOrNull() ?: return

        eTagsStore.store(userWalletId = userWalletId, key = ETagsStore.Key.WalletAccounts, value = eTag)
    }

    /**
     * Refreshes the stored ETag from a successful write. A write bumps the wallet's version, so a response
     * without the header would leave a knowingly stale value behind — every next conditional write would 412 —
     * hence the stored value is dropped instead: the next `GET /accounts` restores it.
     */
    suspend fun refresh(userWalletId: UserWalletId, apiResponse: ApiResponse<*>) {
        val eTag = apiResponse.headers[ETAG_HEADER]?.firstOrNull()

        if (eTag != null) {
            eTagsStore.store(userWalletId = userWalletId, key = ETagsStore.Key.WalletAccounts, value = eTag)
        } else {
            eTagsStore.clear(userWalletId = userWalletId, key = ETagsStore.Key.WalletAccounts)
        }
    }

    suspend fun clear(userWalletId: UserWalletId) {
        eTagsStore.clear(userWalletId = userWalletId, key = ETagsStore.Key.WalletAccounts)
    }
}