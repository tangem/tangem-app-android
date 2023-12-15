package com.tangem.domain.card.repository

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

interface DerivationsRepository {

    suspend fun derivePublicKeys(userWalletId: UserWalletId, currencies: List<CryptoCurrency>)
}