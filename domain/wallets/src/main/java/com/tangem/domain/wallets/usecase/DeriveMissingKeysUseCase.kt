package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.jointaccount.repository.JointOwnerKeysRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.derivations.DerivationsRepository

/**
 * Derives [currencies]' missing network keys together with the wallet's missing joint-account owner keys
 * in a single [DerivationsRepository.derivePublicKeys] call, so a Cold wallet only needs one NFC tap.
 */
class DeriveMissingKeysUseCase(
    private val derivationsRepository: DerivationsRepository,
    private val jointOwnerKeysRepository: JointOwnerKeysRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, currencies: List<CryptoCurrency>): Either<Throwable, Unit> {
        return Either.catch {
            val currencyDerivations = derivationsRepository.findMissingDerivations(userWalletId, currencies)
            val jointDerivations = jointOwnerKeysRepository.findMissingOwnerKeyDerivations(userWalletId)
            val derivations = currencyDerivations.merge(jointDerivations)

            if (derivations.isNotEmpty()) {
                derivationsRepository.derivePublicKeys(userWalletId, derivations)
            }
        }
    }

    private fun Map<ByteArrayKey, List<DerivationPath>>.merge(
        other: Map<ByteArrayKey, List<DerivationPath>>,
    ): Map<ByteArrayKey, List<DerivationPath>> {
        if (isEmpty()) return other
        if (other.isEmpty()) return this

        return (keys + other.keys).associateWith { key ->
            (this[key].orEmpty() + other[key].orEmpty()).distinct()
        }
    }
}