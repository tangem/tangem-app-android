package com.tangem.domain.card.repository

import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.operations.derivation.ExtendedPublicKeysMap

interface DerivationsRepository {

    @Throws
    suspend fun derivePublicKeys(userWalletId: UserWalletId, currencies: List<CryptoCurrency>)

    @Throws
    suspend fun derivePublicKeys(
        userWalletId: UserWalletId,
        derivations: Map<ByteArrayKey, List<DerivationPath>>,
    ): Map<ByteArrayKey, ExtendedPublicKeysMap>
}
