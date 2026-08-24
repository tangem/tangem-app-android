package com.tangem.domain.wallets.repository

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.backup.WalletCardBackup
import com.tangem.domain.wallets.models.errors.WalletCardsBackupError

/**
 * Access to the backend's record of which cards belong to a wallet and how far their backup got.
 *
 * The backend is the only place this survives an app reinstall or a login from another device, which is what
 * makes an interrupted backup detectable at all in those cases.
 */
interface WalletCardsBackupRepository {

    /**
     * Reports the cards known to the app for [userWalletId] and the state of their backup.
     *
     * A report that does not reach the backend is kept, and [sendPendingWalletCards] resends it later — the
     * returned [Either.Left] says the report has not landed yet, not that it has been lost.
     *
     * @param usedSeed `true` if a seed phrase was used to create or import the wallet
     */
    suspend fun saveWalletCards(
        userWalletId: UserWalletId,
        cards: List<WalletCardBackup>,
        usedSeed: Boolean,
    ): Either<WalletCardsBackupError, Unit>

    /**
     * Resends the reports that [saveWalletCards] could not deliver, oldest first.
     *
     * Order is part of the contract: the backend keeps a change history of the reports it receives, so
     * replaying them out of order would misrepresent how the backup progressed. A report that fails because
     * the device is offline is left queued together with everything after it, and the whole drain stops. A
     * report the backend rejects is dropped — resending it can only fail again, and keeping it would wedge
     * every later report behind it.
     */
    suspend fun sendPendingWalletCards(): Either<WalletCardsBackupError, Unit>

    /**
     * Returns the cards the backend knows about for [userWalletId].
     *
     * An empty list means the backend has no data for this wallet — a `404` is folded into it, since the wallet
     * being absent from the backend's database is the same situation. A transport failure is a [Either.Left] and
     * never an empty list, so "the backend cannot be reached" is never mistaken for "the backend knows nothing".
     */
    suspend fun getWalletCards(userWalletId: UserWalletId): Either<WalletCardsBackupError, List<WalletCardBackup>>
}