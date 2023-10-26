package com.tangem.domain.card

import arrow.core.Either
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.operations.derivation.DerivationTaskResponse

interface DerivePublicKeysUseCase {

    suspend operator fun invoke(
        cardId: String? = null,
        derivations: Map<ByteArrayKey, List<DerivationPath>>,
    ): Either<Unit, DerivationTaskResponse>
}