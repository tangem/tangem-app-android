package com.tangem.domain.polymarket.sessionkey

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketSessionKeyAuthorization
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyRevocation
import com.tangem.domain.polymarket.signing.PolymarketRelayerDeadline

data class PolymarketPendingAuthorization(
    val revocation: PolymarketSessionKeyRevocation,
    val authorization: PolymarketSessionKeyAuthorization,
    val purpose: Purpose,
) {

    fun isExecutable(nowMillis: Long): Boolean = !PolymarketRelayerDeadline.isExpired(authorization.deadline, nowMillis)

    fun isResumable(nowMillis: Long): Boolean = isExecutable(nowMillis + PolymarketRelayerDeadline.SEND_MARGIN_MILLIS)

    fun isInFlight(nowMillis: Long): Boolean =
        isExecutable(nowMillis) || !PolymarketRelayerDeadline.isExpired(revocation.deadline, nowMillis)

    fun mayStillLand(nowMillis: Long): Boolean =
        revocation.deadline.isWithinSendMargin(nowMillis) || authorization.deadline.isWithinSendMargin(nowMillis)

    enum class Purpose { EXTEND, REPLACE }
}

private fun String.isWithinSendMargin(nowMillis: Long): Boolean =
    !PolymarketRelayerDeadline.isExpired(this, nowMillis) &&
        PolymarketRelayerDeadline.isExpired(this, nowMillis + PolymarketRelayerDeadline.SEND_MARGIN_MILLIS)

interface PolymarketPendingAuthorizationStore {

    suspend fun get(userWalletId: UserWalletId): Either<PolymarketSessionKeyError, PolymarketPendingAuthorization?>

    suspend fun store(
        userWalletId: UserWalletId,
        pending: PolymarketPendingAuthorization,
    ): Either<PolymarketSessionKeyError, Unit>

    suspend fun clear(userWalletId: UserWalletId)
}