package com.tangem.domain.polymarket.interactor

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketOnboardedStore
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketEntry
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketApiCredentialsUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketWalletStatusUseCase
import com.tangem.domain.polymarket.usecase.RecordPolymarketConfirmationUseCase
import com.tangem.utils.logging.TangemLogger

/**
 * Decides where a user lands when they open the feature: the feed, or the screen that owes onboarding.
 *
 * The region is deliberately not part of this decision — it forbids opening a *new* account, which is a
 * question for [RunPolymarketOnboardingInteractor], not for opening a screen.
 *
 * A ready backend status is not enough to reach the feed: the L2 credentials that sign CLOB requests are
 * stored locally, so a reinstall, a new device or a wallet onboarded elsewhere leaves the status ready with
 * nothing to sign with. Such a user still owes onboarding, which restores the credentials without deploying.
 *
 * A wallet confirmed earlier is still checked against the backend whenever its addresses are stored: the backend
 * can demote a ready wallet (a new approvals set), and only its status sends that user back to onboarding. The
 * local confirmation answers alone only when the backend cannot be read or the addresses are not stored.
 * [invoke] may open a card session to derive the owner address; [withoutPrompting] stops short of that instead of
 * paying for a session the user never asked for.
 */
class ResolvePolymarketEntryInteractor(
    private val derivePolymarketAddressesUseCase: DerivePolymarketAddressesUseCase,
    private val getPolymarketWalletStatusUseCase: GetPolymarketWalletStatusUseCase,
    private val getPolymarketApiCredentialsUseCase: GetPolymarketApiCredentialsUseCase,
    private val polymarketOnboardedStore: PolymarketOnboardedStore,
    private val recordPolymarketConfirmation: RecordPolymarketConfirmationUseCase,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<PolymarketOnboardingError, PolymarketEntry> =
        either {
            confirmedEntry(userWalletId)
                ?: entryFor(addresses = deriveAddresses(userWalletId).bind()).bind()
        }

    /**
     * The decision as far as it can be taken without prompting the user. A wallet whose addresses this device
     * cannot already produce yields [PolymarketEntry.Undetermined] rather than opening a card session.
     */
    suspend fun withoutPrompting(userWalletId: UserWalletId): Either<PolymarketOnboardingError, PolymarketEntry> =
        either {
            confirmedEntry(userWalletId)?.let { return@either it }

            val addresses = derivePolymarketAddressesUseCase.stored(userWalletId)
            if (addresses == null) {
                return@either unpromptedEntry(userWalletId)
            }

            entryFor(addresses = addresses).bind()
        }

    private suspend fun unpromptedEntry(userWalletId: UserWalletId): PolymarketEntry {
        val entry = if (polymarketOnboardedStore.isOnboardedSync(userWalletId)) {
            PolymarketEntry.Reconnect
        } else {
            PolymarketEntry.Undetermined
        }
        TangemLogger.i("Resolve: no addresses available without prompting, entry=$entry")

        return entry
    }

    /**
     * The entry of a wallet already confirmed ready, or `null` when it is not confirmed or has nothing to sign
     * with — either way the backend has to be asked.
     */
    private suspend fun confirmedEntry(userWalletId: UserWalletId): PolymarketEntry? {
        if (!polymarketOnboardedStore.isOnboardedSync(userWalletId)) return null
        if (getPolymarketApiCredentialsUseCase(userWalletId) == null) return null

        val addresses = derivePolymarketAddressesUseCase.stored(userWalletId)
        if (addresses == null) {
            TangemLogger.i("Resolve: $userWalletId is confirmed onboarded, no stored addresses to recheck")
            return PolymarketEntry.Onboarded
        }

        return entryFor(addresses).getOrElse {
            TangemLogger.i("Resolve: $userWalletId is confirmed onboarded, recheck unavailable")
            PolymarketEntry.Onboarded
        }
    }

    private suspend fun entryFor(addresses: PolymarketAddresses): Either<PolymarketOnboardingError, PolymarketEntry> =
        either {
            val state = readWalletStatus(addresses).bind()

            recordPolymarketConfirmation(userWalletId = addresses.userWalletId, status = state.status)

            val hasCredentials = getPolymarketApiCredentialsUseCase(addresses.userWalletId) != null
            TangemLogger.i("Resolve: credentials found=$hasCredentials")

            val entry = state.toEntry(hasCredentials = hasCredentials)
            TangemLogger.i("Resolve: entry=$entry")
            entry
        }

    private suspend fun deriveAddresses(
        userWalletId: UserWalletId,
    ): Either<PolymarketOnboardingError, PolymarketAddresses> =
        derivePolymarketAddressesUseCase(userWalletId).also { result ->
            result.fold(
                ifLeft = { error -> TangemLogger.e("Resolve: address derivation failed: $error") },
                ifRight = { addresses -> TangemLogger.i("Resolve: owner=${addresses.ownerAddress}") },
            )
        }

    private suspend fun readWalletStatus(
        addresses: PolymarketAddresses,
    ): Either<PolymarketOnboardingError, PolymarketWalletState> =
        getPolymarketWalletStatusUseCase(addresses).also { result ->
            result.fold(
                ifLeft = { error -> TangemLogger.e("Resolve: wallet status failed: $error") },
                ifRight = { state -> TangemLogger.i("Resolve: wallet status=${state.status}") },
            )
        }

    private fun PolymarketWalletState.toEntry(hasCredentials: Boolean): PolymarketEntry = when {
        status != PolymarketWalletStatus.READY_TO_TRADE -> PolymarketEntry.Onboard(status = status)
        hasCredentials -> PolymarketEntry.Onboarded
        else -> PolymarketEntry.Reconnect
    }
}