package com.tangem.domain.polymarket.sessionkey

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import java.time.Clock
import java.time.Instant

internal sealed interface PolymarketSessionKeyTradability {

    data class Tradable(val validUntil: Instant) : PolymarketSessionKeyTradability

    data class Untradable(val reason: String) : PolymarketSessionKeyTradability
}

internal suspend fun PolymarketSessionKeyTradingStore.tradability(
    userWalletId: UserWalletId,
    sessionKeyAddress: String,
    clock: Clock,
    readWallet: suspend () -> Either<PolymarketWalletError, PolymarketWalletState>,
): PolymarketSessionKeyTradability {
    if (isRefused(userWalletId, sessionKeyAddress)) {
        return PolymarketSessionKeyTradability.Untradable(reason = "the exchange refused the session key")
    }
    if (isMissingOnExchange(userWalletId, sessionKeyAddress)) {
        return PolymarketSessionKeyTradability.Untradable(reason = "the session key is gone from Polymarket")
    }

    val wallet = readWallet().getOrElse { error ->
        return PolymarketSessionKeyTradability.Untradable(reason = "the wallet could not be read: $error")
    }
    val validUntil = wallet.tradableValidUntil(sessionKeyAddress = sessionKeyAddress, now = clock.instant())
        ?: return PolymarketSessionKeyTradability.Untradable(reason = "the BFF does not let the session key trade")

    if (!isReadyOnExchange(userWalletId, sessionKeyAddress, validUntil)) {
        return PolymarketSessionKeyTradability.Untradable(
            reason = "Polymarket has not listed the session key since its authorization",
        )
    }
    return PolymarketSessionKeyTradability.Tradable(validUntil = validUntil)
}

private fun PolymarketWalletState.tradableValidUntil(sessionKeyAddress: String, now: Instant): Instant? {
    if (status != PolymarketWalletStatus.READY_TO_TRADE) return null

    val record = sessionKeys.firstOrNull { it.address.equals(sessionKeyAddress, ignoreCase = true) }
        ?: return null
    val validUntil = record.validUntil ?: return null
    val isTradable = record.status == PolymarketSessionKeyStatus.ACTIVE &&
        record.isEnabled &&
        now < validUntil - PolymarketSessionKeyAssessor.SIGNING_EXPIRY_MARGIN

    return validUntil.takeIf { isTradable }
}