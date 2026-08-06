package com.tangem.data.polymarket.cleaner

import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketCredentialsStore
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

/**
 * Removes the Polymarket CLOB credentials of deleted wallets.
 *
 * [UserWalletId] is derived from the wallet key, so re-adding the same wallet reproduces the storage key.
 * Without this, the credentials of a deleted wallet are handed back to it: the onboarding run returns any
 * stored entry before asking for a signature, so the card tap that proves key custody never happens — and
 * the entry would otherwise survive for the life of the install, unreachable by any later cleanup.
 *
 * @property credentialsStore secure storage of the L2 API credentials
 */
internal class PolymarketUserWalletDataCleaner @Inject constructor(
    private val credentialsStore: PolymarketCredentialsStore,
) : UserWalletDataCleaner {

    override suspend fun clear(userWalletIds: List<UserWalletId>) {
        userWalletIds.forEach { userWalletId ->
            runSuspendCatching { credentialsStore.clear(userWalletId) }
                .onFailure { TangemLogger.e("Failed to clear the Polymarket credentials of $userWalletId", it) }
        }
    }
}