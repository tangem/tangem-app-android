package com.tangem.domain.card

import arrow.core.Either
import com.tangem.crypto.NetworkType
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.card.repository.DerivationsRepository
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Derivates an exteneded public key (xpub) based on blockchain hardened derivation
 */
class GetExtendedPublicKeyForCurrencyUseCase(
    private val derivationsRepository: DerivationsRepository,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        derivation: Network.DerivationPath,
    ): Either<Throwable, String> {
        return Either.catch {
            val derivationPath = requireNotNull(derivation.value?.let { DerivationPath(it) }) {
                error("Derivation is null")
            }
            val hardenedNodes = derivationPath.nodes.filter { it.isHardened }
            val hardenedDerivation = DerivationPath(hardenedNodes)
            derivationsRepository.deriveExtendedPublicKey(userWalletId, hardenedDerivation)
                ?.serialize(NetworkType.Mainnet).orEmpty()
        }
    }
}