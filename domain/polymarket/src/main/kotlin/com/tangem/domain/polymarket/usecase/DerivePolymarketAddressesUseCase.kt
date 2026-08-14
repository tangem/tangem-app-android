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
            .flatMap { ownerAddress ->
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
}