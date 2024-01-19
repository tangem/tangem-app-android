package com.tangem.domain.card

import arrow.core.Either
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.operations.derivation.DerivationTaskResponse

// TODO: Convert to class [REDACTED_JIRA]
interface DerivePublicKeysUseCase {

    // TODO: delete [REDACTED_JIRA]
    @Deprecated(message = "Use invoke(cardId: String?, derivations: Map<ByteArrayKey, List<DerivationPath>>) instead")
    suspend operator fun invoke(
        cardId: String? = null,
        derivations: Map<ByteArrayKey, List<DerivationPath>>,
    ): Either<Unit, DerivationTaskResponse>

    suspend operator fun invoke(userWalletId: UserWalletId, currencies: List<CryptoCurrency>): Either<Throwable, Unit>
}