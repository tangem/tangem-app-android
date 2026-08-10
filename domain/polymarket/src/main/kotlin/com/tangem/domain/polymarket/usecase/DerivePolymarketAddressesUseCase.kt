package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import arrow.core.flatMap
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.derivation.PolymarketDepositWalletDeriver
import com.tangem.domain.polymarket.derivation.PolymarketEoaDeriver
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketOnboardingError

/**
 * Produces the owner EOA and its deposit wallet. Deriving the owner key may require a card session;
 * the deposit wallet is then computed locally from that owner address.
 */
class DerivePolymarketAddressesUseCase(
    private val eoaDeriver: PolymarketEoaDeriver,
    private val depositWalletDeriver: PolymarketDepositWalletDeriver,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<PolymarketOnboardingError, PolymarketAddresses> =
        eoaDeriver.deriveOwnerEoa(userWalletId = userWalletId)
            .mapLeft { it.toOnboardingError() }
            .flatMap { ownerAddress -> addresses(userWalletId = userWalletId, ownerAddress = ownerAddress) }

    /**
     * The addresses if this device can produce them without asking the user for anything — no card session, no
     * wallet unlock — so a screen may call it before the user has acted on it.
     *
     * `null` means "not available here", which covers two cases the caller cannot tell apart: the owner key has
     * never been derived on this device, or it has but the deposit wallet could not be derived from it. Neither
     * says anything about whether the wallet has a Polymarket account — only [invoke] can answer that.
     */
    suspend fun stored(userWalletId: UserWalletId): PolymarketAddresses? {
        val ownerAddress = eoaDeriver.storedOwnerEoa(userWalletId = userWalletId) ?: return null

        return addresses(userWalletId = userWalletId, ownerAddress = ownerAddress).getOrNull()
    }

    private fun addresses(
        userWalletId: UserWalletId,
        ownerAddress: String,
    ): Either<PolymarketOnboardingError, PolymarketAddresses> =
        Either.catch { depositWalletDeriver.deriveDepositWallet(ownerAddress = ownerAddress) }
            .mapLeft { PolymarketOnboardingError.Unknown }
            .map { depositWalletAddress ->
                PolymarketAddresses(
                    ownerAddress = ownerAddress,
                    depositWalletAddress = depositWalletAddress,
                    userWalletId = userWalletId,
                )
            }
}