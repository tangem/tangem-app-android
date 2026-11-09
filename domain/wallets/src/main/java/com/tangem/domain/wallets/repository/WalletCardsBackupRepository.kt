package com.tangem.domain.wallets.repository

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.backup.WalletCardBackup
import com.tangem.domain.wallets.models.errors.WalletCardsBackupError
import com.tangem.utils.annotations.RemoveWithToggle

/**
 * Access to the backend's record of which cards belong to a wallet and how far their backup got.
 *
 * The backend is the only place this survives an app reinstall or a login from another device, which is what
 * makes an interrupted backup detectable at all in those cases.
 */
interface WalletCardsBackupRepository {

    /**
     * Reports [cards] of [userWalletId] together with every other card of the wallet this device has seen.
     *
     * The cards are remembered first, so a later report about any card of the wallet carries them too. A card
     * reported without a role or an error keeps the ones remembered for it. The report is then queued and sent
     * behind everything already waiting, so a report held up earlier is never overtaken by a newer one. A report
     * that does not reach the backend stays queued and [sendPendingWalletCards] resends it later — the returned
     * [Either.Left] says the report has not landed yet, not that it has been lost.
     *
     * With `TWI_1586_CARD_LINKED_STATUS_UPDATE_V2` off only [cards] are sent and nothing is remembered.
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
     * replaying them out of order would misrepresent how the backup progressed.
     *
     * A report that fails for a reason that may pass — the device is offline, the backend is down, rate
     * limiting, a response that could not be read — stays queued together with everything after it, and the
     * drain stops rather than retrying the rest against the same failure. Only a report the backend refuses
     * outright is dropped: resending identical bytes can only be refused again, and keeping it would wedge
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

    /**
     * Same as [saveWalletCards] for a single [card] seen outside onboarding, but sends nothing when this device
     * already knows the card in exactly this state — scanning the same card again must not repeat the report.
     */
    suspend fun saveSeenWalletCard(
        userWalletId: UserWalletId,
        card: WalletCardBackup,
        usedSeed: Boolean,
    ): Either<WalletCardsBackupError, Unit>

    /** Whether this device has seen a card of [userWalletId] whose backup is not complete */
    suspend fun hasCardWithIncompleteBackup(userWalletId: UserWalletId): Boolean

    /** Whether this device has already delivered the stored `CardLinked` state of [userWalletId] to the backend */
    @RemoveWithToggle("TWI_1586_CARD_LINKED_STATUS_UPDATE_V2")
    suspend fun isCardLinkedReported(userWalletId: UserWalletId): Boolean

    @RemoveWithToggle("TWI_1586_CARD_LINKED_STATUS_UPDATE_V2")
    suspend fun setCardLinkedReported(userWalletId: UserWalletId)
}