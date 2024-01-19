package com.tangem.tap.domain.card

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.card.DerivePublicKeysUseCase
import com.tangem.domain.card.repository.DerivationsRepository
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.operations.derivation.DerivationTaskResponse
import com.tangem.tap.domain.TangemSdkManager

internal class DefaultDerivePublicKeysUseCase(
    private val tangemSdkManager: TangemSdkManager,
    private val derivationsRepository: DerivationsRepository,
) : DerivePublicKeysUseCase {

    override suspend fun invoke(
        cardId: String?,
        derivations: Map<ByteArrayKey, List<DerivationPath>>,
    ): Either<Unit, DerivationTaskResponse> {
        tangemSdkManager.derivePublicKeys(cardId = cardId, derivations = derivations)
            .doOnSuccess { return it.right() }
            .doOnFailure { return Unit.left() }

        return Unit.left()
    }

    override suspend fun invoke(
        userWalletId: UserWalletId,
        currencies: List<CryptoCurrency>,
    ): Either<Throwable, Unit> {
        return Either.catch {
            derivationsRepository.derivePublicKeys(userWalletId, currencies)
        }
    }
}