package com.tangem.domain.polymarket.sessionkey

import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.model.newestFor
import com.tangem.domain.polymarket.signing.PolymarketRelayerDeadline
import java.math.BigInteger

internal sealed interface PolymarketPendingAuthorizationStage {

    data class RevocationSent(val revoking: PolymarketSessionKey) : PolymarketPendingAuthorizationStage

    data object RevocationUnsent : PolymarketPendingAuthorizationStage

    data object RevokedRecordDropped : PolymarketPendingAuthorizationStage

    data class RevocationUnconfirmed(val revoking: PolymarketSessionKey) : PolymarketPendingAuthorizationStage

    data object Expired : PolymarketPendingAuthorizationStage

    data object NonceMoved : PolymarketPendingAuthorizationStage

    data object NonceBehind : PolymarketPendingAuthorizationStage
}

internal fun PolymarketPendingAuthorization.stageAt(
    sessionKeys: List<PolymarketSessionKey>,
    walletNonce: BigInteger,
    nowMillis: Long,
): PolymarketPendingAuthorizationStage {
    if (!isExecutable(nowMillis)) return PolymarketPendingAuthorizationStage.Expired
    val authorizationNonce = authorization.nonce.toBigIntegerOrNull()
        ?: return PolymarketPendingAuthorizationStage.NonceMoved
    if (revocation.nonce.toBigIntegerOrNull() != authorizationNonce - BigInteger.ONE) {
        return PolymarketPendingAuthorizationStage.NonceMoved
    }
    val revocationRecord = sessionKeys.newestFor(revocation.sessionKeyAddress)
    return when {
        walletNonce == authorizationNonce -> when {
            revocationRecord == null -> PolymarketPendingAuthorizationStage.RevokedRecordDropped
            revocationRecord.showsRevocation() -> PolymarketPendingAuthorizationStage.RevocationSent(revocationRecord)
            else -> PolymarketPendingAuthorizationStage.RevocationUnconfirmed(revocationRecord)
        }
        walletNonce < authorizationNonce - BigInteger.ONE -> PolymarketPendingAuthorizationStage.NonceBehind
        walletNonce != authorizationNonce - BigInteger.ONE -> PolymarketPendingAuthorizationStage.NonceMoved
        revocationRecord?.showsRevocation() == true -> {
            PolymarketPendingAuthorizationStage.RevocationSent(revocationRecord)
        }
        PolymarketRelayerDeadline.isExpired(
            deadline = revocation.deadline,
            nowMillis = nowMillis + PolymarketRelayerDeadline.SEND_MARGIN_MILLIS,
        ) -> {
            PolymarketPendingAuthorizationStage.Expired
        }
        else -> PolymarketPendingAuthorizationStage.RevocationUnsent
    }
}

internal fun PolymarketPendingAuthorization.acceptedIn(sessionKeys: List<PolymarketSessionKey>): PolymarketSessionKey? =
    sessionKeys.newestFor(authorization.sessionKeyAddress)?.takeIf(::isAcceptedAuthorization)

private fun PolymarketPendingAuthorization.isAcceptedAuthorization(record: PolymarketSessionKey): Boolean =
    (record.status == PolymarketSessionKeyStatus.PENDING || record.status == PolymarketSessionKeyStatus.ACTIVE) &&
        record.validUntil?.epochSecond?.toString() == authorization.validUntil

private fun PolymarketSessionKey.showsRevocation(): Boolean =
    status == PolymarketSessionKeyStatus.REVOKING || status == PolymarketSessionKeyStatus.REVOKED