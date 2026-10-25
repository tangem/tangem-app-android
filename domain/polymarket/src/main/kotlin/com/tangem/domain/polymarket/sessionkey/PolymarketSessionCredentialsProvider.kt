package com.tangem.domain.polymarket.sessionkey

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketClobIdentity
import com.tangem.domain.polymarket.model.PolymarketSessionCredentialsError

interface PolymarketSessionCredentialsProvider {

    suspend fun get(
        userWalletId: UserWalletId,
    ): Either<PolymarketSessionCredentialsError, PolymarketClobIdentity.Session>

    fun invalidate(userWalletId: UserWalletId, identity: PolymarketClobIdentity.Session)
}