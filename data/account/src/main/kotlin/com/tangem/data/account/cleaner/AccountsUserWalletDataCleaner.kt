package com.tangem.data.account.cleaner

import com.tangem.data.account.store.AccountsResponseStoreFactory
import com.tangem.data.account.store.LegacyUserTokensResponseStore
import com.tangem.data.common.cache.etag.ETagsStore
import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Clears per-wallet accounts and user tokens caches when wallets are deleted.
 *
 * Since [UserWalletId] is derived from the wallet key, re-adding the same wallet reuses the same id. Without this
 * cleanup the stale accounts response and its ETag survive the deletion, the server answers `304 Not Modified` on
 * re-add and the cached (non-empty) accounts short-circuit the fetch flow, so public key derivation never runs.
 *
 * @property accountsResponseStoreFactory factory owning the per-wallet accounts response stores
 * @property legacyUserTokensResponseStore legacy per-wallet user tokens store
 * @property eTagsStore                    store of caching ETags
 */
internal class AccountsUserWalletDataCleaner @Inject constructor(
    private val accountsResponseStoreFactory: AccountsResponseStoreFactory,
    private val legacyUserTokensResponseStore: LegacyUserTokensResponseStore,
    private val eTagsStore: ETagsStore,
) : UserWalletDataCleaner {

    override suspend fun clear(userWalletIds: List<UserWalletId>) {
        // Best-effort: isolate failures per store so one failing store does not cancel the others.
        coroutineScope {
            launch { clearSafely(store = "accounts response") { accountsResponseStoreFactory.clear(userWalletIds) } }
            launch { clearSafely(store = "legacy user tokens") { legacyUserTokensResponseStore.clear(userWalletIds) } }
            launch { clearSafely(store = "ETags") { eTagsStore.clear(userWalletIds) } }
        }
    }

    private suspend fun clearSafely(store: String, clear: suspend () -> Unit) {
        runSuspendCatching { clear() }
            .onFailure { TangemLogger.e("Failed to clear $store store", it) }
    }
}