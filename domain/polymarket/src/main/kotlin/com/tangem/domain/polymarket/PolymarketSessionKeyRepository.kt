package com.tangem.domain.polymarket

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyAuthorization
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyRevocation
import com.tangem.domain.polymarket.model.PolymarketSessionSigner

interface PolymarketSessionKeyRepository {

    /** @return the key's record — `PENDING`, or its current state for a repeat of the last accepted request */
    suspend fun authorizeSessionKey(
        userWalletId: UserWalletId,
        authorization: PolymarketSessionKeyAuthorization,
    ): Either<PolymarketSessionKeyError, PolymarketSessionKey>

    /** @return the key's record — `REVOKING`, or its current state for a repeat of the last accepted request */
    suspend fun revokeSessionKey(
        userWalletId: UserWalletId,
        revocation: PolymarketSessionKeyRevocation,
    ): Either<PolymarketSessionKeyError, PolymarketSessionKey>

    suspend fun setSessionKeyEnabled(
        userWalletId: UserWalletId,
        sessionKeyAddress: String,
        isEnabled: Boolean,
    ): Either<PolymarketSessionKeyError, PolymarketSessionKey>

    suspend fun getSessionSigners(
        ownerAddress: String,
        credentials: PolymarketApiCredentials,
    ): Either<PolymarketAuthError, List<PolymarketSessionSigner>>
}