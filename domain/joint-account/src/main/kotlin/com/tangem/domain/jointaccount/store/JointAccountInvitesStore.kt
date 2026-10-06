package com.tangem.domain.jointaccount.store

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

/**

 *
 * The backend returns invites exactly once — in the `POST /joint-accounts` create response — and has no read
 * endpoint for them, so a lost response makes inviting members impossible. The creation flow must [store] them
 * immediately after a successful `POST`.
 *
 * An invite id is a secret: whoever holds it can take a slot in the pending account. Implementations must keep
 * the ids encrypted and must never let them reach logs.
 */
interface JointAccountInvitesStore {

    /** Replaces the stored invites of the account [cryptoAccountId] in the wallet [userWalletId]. */
    suspend fun store(userWalletId: UserWalletId, cryptoAccountId: String, invites: List<String>)

    /** Returns the stored invites of the account [cryptoAccountId], or `null` when nothing is stored for it. */
    suspend fun get(userWalletId: UserWalletId, cryptoAccountId: String): List<String>?

    /** Records that the invite [inviteId] of the account [cryptoAccountId] has been handed to the share sheet. */
    suspend fun markShared(userWalletId: UserWalletId, cryptoAccountId: String, inviteId: String)

    /** The invites of the account [cryptoAccountId] already handed to the share sheet; emits again on every write. */
    fun observeShared(userWalletId: UserWalletId, cryptoAccountId: String): Flow<Set<String>>

    /** Removes the invites of the account [cryptoAccountId] — they are useless once the account is active. */
    suspend fun clear(userWalletId: UserWalletId, cryptoAccountId: String)

    /** Removes the invites of every account of the wallet [userWalletId] — for wallet deletion. */
    suspend fun clearAll(userWalletId: UserWalletId)
}