package com.tangem.domain.polymarket.interactor

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketEntry
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketApiCredentialsUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketWalletStatusUseCase
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
 * [invoke] may open a card session to derive the owner address; [withoutPrompting] stops short of that instead
 * of paying for a session the user never asked for.
 */
class ResolvePolymarketEntryInteractor(
    private val derivePolymarketAddressesUseCase: DerivePolymarketAddressesUseCase,
    private val getPolymarketWalletStatusUseCase: GetPolymarketWalletStatusUseCase,
    private val getPolymarketApiCredentialsUseCase: GetPolymarketApiCredentialsUseCase,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<PolymarketOnboardingError, PolymarketEntry> =
        either {
            entryFor(addresses = deriveAddresses(userWalletId).bind()).bind()
        }

    /**
     * The decision as far as it can be taken without prompting the user. A wallet whose addresses this device
     * cannot already produce yields [PolymarketEntry.Undetermined] rather than opening a card session.
     */
    suspend fun withoutPrompting(userWalletId: UserWalletId): Either<PolymarketOnboardingError, PolymarketEntry> =
        either {
            val addresses = derivePolymarketAddressesUseCase.stored(userWalletId)
            if (addresses == null) {
                TangemLogger.i("Resolve: no addresses available without prompting, entry=Undetermined")
                return@either PolymarketEntry.Undetermined
            }

            entryFor(addresses = addresses).bind()
        }

    private suspend fun entryFor(addresses: PolymarketAddresses): Either<PolymarketOnboardingError, PolymarketEntry> =
        either {
            val state = readWalletStatus(addresses).bind()

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

    private fun PolymarketWalletState.toEntry(hasCredentials: Boolean): PolymarketEntry =
        if (status == PolymarketWalletStatus.READY_TO_TRADE && hasCredentials) {
            PolymarketEntry.Onboarded
        } else {
            PolymarketEntry.Onboard(status = status)
        }
}