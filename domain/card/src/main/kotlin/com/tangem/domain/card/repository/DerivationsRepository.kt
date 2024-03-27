package com.tangem.domain.card.repository

import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

interface DerivationsRepository {

    @Throws
    suspend fun derivePublicKeys(userWalletId: UserWalletId, currencies: List<CryptoCurrency>)

    @Throws
    suspend fun deriveExtendedPublicKey(userWalletId: UserWalletId, derivation: DerivationPath): ExtendedPublicKey?
}
