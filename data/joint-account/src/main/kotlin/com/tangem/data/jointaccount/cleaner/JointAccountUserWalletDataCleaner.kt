package com.tangem.data.jointaccount.cleaner

import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.domain.jointaccount.store.JointAccountInvitesStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

/**
 * Removes the joint account invites of deleted wallets.
 *
 * [UserWalletId] is derived from the wallet key, so re-adding the same wallet reproduces the storage key.
 * Invite ids are secrets guarding the wallet's pending joint accounts — they must not outlive the wallet, and
 * without this cleaner the entry would survive for the life of the install, unreachable by any later cleanup.
 */
internal class JointAccountUserWalletDataCleaner @Inject constructor(
    private val invitesStore: JointAccountInvitesStore,
) : UserWalletDataCleaner {

    override suspend fun clear(userWalletIds: List<UserWalletId>) {
        userWalletIds.forEach { userWalletId ->
            runSuspendCatching { invitesStore.clearAll(userWalletId) }
                .onFailure { TangemLogger.e("Failed to clear the joint account invites of $userWalletId", it) }
        }
    }
}