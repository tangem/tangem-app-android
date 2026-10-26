package com.tangem.domain.polymarket.signing

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketSigningError

interface PolymarketSigningSession {

    suspend fun <T> run(
        userWalletId: UserWalletId,
        prepare: suspend (ownerAddress: String) -> PolymarketSignRequest<T>,
    ): Either<PolymarketSigningError, PolymarketSigned<T>>
}

class PolymarketSignRequest<T>(val value: T, val payloads: List<PolymarketTypedPayload>)

class PolymarketSigned<T>(val value: T, val signatures: List<String>)

sealed interface PolymarketTypedPayload {

    data class ClobAuth(val data: PolymarketClobAuthData) : PolymarketTypedPayload

    data class RelayedBatch(val batch: PolymarketApprovalsPayload) : PolymarketTypedPayload
}