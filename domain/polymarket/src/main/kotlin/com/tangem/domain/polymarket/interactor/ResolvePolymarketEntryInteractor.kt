package com.tangem.domain.polymarket.interactor

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketAccessMode
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketEntry
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.usecase.CheckPolymarketGeoblockUseCase
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketApiCredentialsUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketWalletStatusUseCase
import com.tangem.utils.logging.TangemLogger

/**
 * Decides where a user lands when they open the feature.
 *
 * A blocked region forbids trading, not access: a user who already has a deposit wallet keeps read-only
 * access to it, including withdrawal. Only a blocked user without a wallet is turned away.
 *
 * The region is read first and a failure stops the resolution, so a network error can never be mistaken for
 * an allowed region. Resolving the wallet state requires the owner address, so this may open a card session
 * when the wallet has never derived the Polymarket key.
 *
 * A ready backend status alone is not enough to reach the feed: the L2 credentials that sign CLOB requests
 * are stored locally and can be missing even when the backend considers the wallet ready to trade — a
 * reinstall, cleared storage, a new device, a restored backup, or a wallet onboarded on another platform all
 * leave the status ready with nothing usable to sign with. Such a user still owes onboarding, which derives
 * the credentials before it reports itself finished.
 */
class ResolvePolymarketEntryInteractor(
    private val checkPolymarketGeoblockUseCase: CheckPolymarketGeoblockUseCase,
    private val derivePolymarketAddressesUseCase: DerivePolymarketAddressesUseCase,
    private val getPolymarketWalletStatusUseCase: GetPolymarketWalletStatusUseCase,
    private val getPolymarketApiCredentialsUseCase: GetPolymarketApiCredentialsUseCase,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<PolymarketOnboardingError, PolymarketEntry> =
        either {
            val isBlocked = checkGeoblock().bind()
            val addresses = deriveAddresses(userWalletId).bind()
            val state = readWalletStatus(addresses).bind()

            val hasCredentials = hasCredentials(addresses)
            TangemLogger.i("Resolve: credentials found=$hasCredentials")

            val entry = state.toEntry(isBlocked = isBlocked, hasCredentials = hasCredentials)
            TangemLogger.i("Resolve: entry=$entry")
            entry
        }

    private suspend fun checkGeoblock(): Either<PolymarketOnboardingError, Boolean> =
        checkPolymarketGeoblockUseCase().also { result ->
            result.fold(
                ifLeft = { error -> TangemLogger.e("Resolve: geoblock check failed: $error") },
                ifRight = { isBlocked -> TangemLogger.i("Resolve: geoblock isBlocked=$isBlocked") },
            )
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

    private suspend fun hasCredentials(addresses: PolymarketAddresses): Boolean =
        getPolymarketApiCredentialsUseCase(addresses.userWalletId) != null

    private fun PolymarketWalletState.toEntry(isBlocked: Boolean, hasCredentials: Boolean): PolymarketEntry = when {
        isBlocked && !hasDepositWallet() -> PolymarketEntry.RegionBlocked
        isBlocked -> PolymarketEntry.Onboarded(accessMode = PolymarketAccessMode.READ_ONLY)
        status == PolymarketWalletStatus.READY_TO_TRADE && hasCredentials ->
            PolymarketEntry.Onboarded(accessMode = PolymarketAccessMode.TRADING)
        else -> PolymarketEntry.Onboard(status = status)
    }

    private fun PolymarketWalletState.hasDepositWallet(): Boolean = depositWalletAddress != null
}