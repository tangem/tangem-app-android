package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import arrow.core.flatMap
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketAccessMode
import com.tangem.domain.polymarket.model.PolymarketEntry
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus

/**
 * Decides where a user lands when they open the feature.
 *
 * A blocked region forbids trading, not access: a user who already has a deposit wallet keeps read-only
 * access to it, including withdrawal. Only a blocked user without a wallet is turned away.
 *
 * The region is read first and a failure stops the resolution, so a network error can never be mistaken for
 * an allowed region. Resolving the wallet state requires the owner address, so this may open a card session
 * when the wallet has never derived the Polymarket key.
 */
class ResolvePolymarketEntryUseCase(
    private val checkPolymarketGeoblockUseCase: CheckPolymarketGeoblockUseCase,
    private val derivePolymarketAddressesUseCase: DerivePolymarketAddressesUseCase,
    private val getPolymarketWalletStatusUseCase: GetPolymarketWalletStatusUseCase,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<PolymarketOnboardingError, PolymarketEntry> =
        checkPolymarketGeoblockUseCase()
            .flatMap { isBlocked ->
                derivePolymarketAddressesUseCase(userWalletId)
                    .flatMap { addresses -> getPolymarketWalletStatusUseCase(addresses) }
                    .map { state -> state.toEntry(isBlocked = isBlocked) }
            }

    private fun PolymarketWalletState.toEntry(isBlocked: Boolean): PolymarketEntry = when {
        isBlocked && !hasDepositWallet() -> PolymarketEntry.RegionBlocked
        isBlocked -> PolymarketEntry.Onboarded(accessMode = PolymarketAccessMode.READ_ONLY)
        status == PolymarketWalletStatus.READY_TO_TRADE ->
            PolymarketEntry.Onboarded(accessMode = PolymarketAccessMode.TRADING)
        else -> PolymarketEntry.Onboard(status = status)
    }

    private fun PolymarketWalletState.hasDepositWallet(): Boolean = depositWalletAddress != null
}