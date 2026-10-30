package com.tangem.domain.collectibles.account.repository

import arrow.core.Either
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * The wallet's collectibles account as this device knows it.
 *
 * The account exists because its vault does, and the vault lives on chain — so what is kept here is a record of
 * that, not the account itself. `null` is the absence of any record, which is not an account: a wallet that has
 * never onboarded and one whose vault was closed elsewhere look the same, and both correctly have no account.
 */
interface CollectiblesAccountRepository {

    /** The recorded status of [userWalletId], re-emitting as the record changes. */
    fun getStatus(userWalletId: UserWalletId): Flow<CollectiblesAccountStatusValue?>

    /** The recorded status of [userWalletId], answered once. */
    suspend fun getStatusSync(userWalletId: UserWalletId): CollectiblesAccountStatusValue?

    /** Adds the account to the wallet's backend accounts document, unless it is already there. */
    suspend fun registerAccount(userWalletId: UserWalletId): Either<CollectiblesAccountError, Unit>

    /**
     * Records that [userWalletId] has a collectibles account, in the state [status] describes.
     *

     * accounts, they differ in whether spins can be signed yet.
     */
    suspend fun saveAccount(userWalletId: UserWalletId, status: CollectiblesAccountStatusValue)

    /**
     * Records that `update_vault` was just broadcast for [userWalletId]. Until it lands the vault still answers to
     * the old delegate, and that must not read as a reason to relink again.
     */
    fun markRelinkSent(userWalletId: UserWalletId)

    /** Whether a relink of [userWalletId] was broadcast recently enough that it may still land. */
    fun isRelinkPending(userWalletId: UserWalletId): Boolean

    /** Drops the relink mark of [userWalletId] once the vault answers to this device. */
    fun clearRelinkPending(userWalletId: UserWalletId)

    /** Removes whatever is recorded for [userWalletId]. */
    suspend fun clearAccount(userWalletId: UserWalletId)

    /** Drops the account from the wallet's backend accounts document; the backend archives the row. */
    suspend fun unregisterAccount(userWalletId: UserWalletId)
}