package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.jointaccount.repository.JointOwnerKeysRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.operations.derivation.ExtendedPublicKeysMap

/**
 * Derives [currencies]' missing network keys together with the wallet's missing joint-account owner keys
 * in a single [DerivationsRepository.derivePublicKeys] call, so a Cold wallet only needs one NFC tap.
 *
 * Reports what the card actually did as a [DerivationOutcome], so callers can tell a card that derived nothing
 * from one that derived at least a part of what was asked.
 */
class DeriveCurrencyAndOwnerKeysUseCase(
    private val derivationsRepository: DerivationsRepository,
    private val jointOwnerKeysRepository: JointOwnerKeysRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        currencies: List<CryptoCurrency>,
    ): Either<Throwable, DerivationOutcome> {
        return Either.catch {
            val currencyDerivations = derivationsRepository.findMissingDerivations(userWalletId, currencies)
            val jointDerivations = jointOwnerKeysRepository.findMissingOwnerKeyDerivations(userWalletId)
            val derivations = currencyDerivations.merge(jointDerivations)

            if (derivations.values.all(List<DerivationPath>::isEmpty)) return@catch DerivationOutcome.NOTHING_MISSING

            val derivedKeys = derivationsRepository.derivePublicKeys(userWalletId, derivations)

            if (derivedKeys.derivedNothing()) DerivationOutcome.NOTHING_DERIVED else DerivationOutcome.DERIVED
        }
    }

    /**
     * A wallet that could not derive at all — a locked mobile wallet — answers with no entries, which is a
     * different case from one that answered for every requested seed key and derived not a single path.
     */
    private fun Map<ByteArrayKey, ExtendedPublicKeysMap>.derivedNothing(): Boolean {
        return isNotEmpty() && values.all(Map<*, *>::isEmpty)
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