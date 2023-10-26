package com.tangem.tap.domain.card

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.card.DerivePublicKeysUseCase
import com.tangem.operations.derivation.DerivationTaskResponse
import com.tangem.tap.domain.TangemSdkManager

// TODO: [REDACTED_JIRA]
internal class DefaultDerivePublicKeysUseCase(
    private val tangemSdkManager: TangemSdkManager,
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
}