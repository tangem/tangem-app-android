package com.tangem.domain.polymarket.sessionkey

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PolymarketSessionKeyAuthorization
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyRevocation
import java.util.concurrent.ConcurrentHashMap

internal class FakePolymarketPendingAuthorizationStore : PolymarketPendingAuthorizationStore {

    private val entries = ConcurrentHashMap<UserWalletId, PolymarketPendingAuthorization>()

    var storeResult: Either<PolymarketSessionKeyError, Unit> = Unit.right()

    var readFailure: PolymarketSessionKeyError? = null

    fun stored(userWalletId: UserWalletId): PolymarketPendingAuthorization? = entries[userWalletId]

    override suspend fun get(
        userWalletId: UserWalletId,
    ): Either<PolymarketSessionKeyError, PolymarketPendingAuthorization?> =
        readFailure?.left() ?: entries[userWalletId].right()

    override suspend fun store(
        userWalletId: UserWalletId,
        pending: PolymarketPendingAuthorization,
    ): Either<PolymarketSessionKeyError, Unit> {
        if (storeResult.isRight()) entries[userWalletId] = pending
        return storeResult
    }

    override suspend fun clear(userWalletId: UserWalletId) {
        entries.remove(userWalletId)
    }
}

internal const val FUTURE_DEADLINE = "9999999999"
internal const val EXPIRED_DEADLINE = "1"

internal fun createPendingAuthorization(
    deadline: String = FUTURE_DEADLINE,
    purpose: PolymarketPendingAuthorization.Purpose = PolymarketPendingAuthorization.Purpose.EXTEND,
): PolymarketPendingAuthorization {
    val owner = "0x1111111111111111111111111111111111111111"
    val depositWallet = "0x2222222222222222222222222222222222222222"
    val sessionKey = "0x5e55000000000000000000000000000000000001"
    val call = PolymarketApprovalCall(target = depositWallet, value = "0", data = "0x")
    return PolymarketPendingAuthorization(
        revocation = PolymarketSessionKeyRevocation(
            ownerAddress = owner,
            depositWalletAddress = depositWallet,
            sessionKeyAddress = sessionKey,
            nonce = "7",
            deadline = deadline,
            call = call,
            signature = "0xrevoke",
        ),
        authorization = PolymarketSessionKeyAuthorization(
            ownerAddress = owner,
            depositWalletAddress = depositWallet,
            sessionKeyAddress = sessionKey,
            deviceId = "device",
            deviceName = "Pixel",
            validUntil = "1789430000",
            nonce = "8",
            deadline = deadline,
            call = call,
            signature = "0xsig",
            replacesSessionKeyAddress = null,
        ),
        purpose = purpose,
    )
}