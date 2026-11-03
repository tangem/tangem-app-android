package com.tangem.domain.polymarket.sessionkey

import arrow.core.getOrElse
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketClobIdentity
import com.tangem.domain.polymarket.model.PredictionOrderFailure
import com.tangem.domain.polymarket.model.PredictionOrderSigningRoute
import com.tangem.domain.polymarket.model.PredictionPlaceOrderError
import com.tangem.utils.logging.TangemLogger
import java.time.Clock

class PolymarketOrderSigningRouter(
    private val polymarketRepository: PolymarketRepository,
    private val sessionKeyStore: PolymarketSessionKeyStore,
    private val tradingStore: PolymarketSessionKeyTradingStore,
    private val sessionCredentialsProvider: PolymarketSessionCredentialsProvider,
    private val clock: Clock = Clock.systemUTC(),
) {

    suspend fun route(addresses: PolymarketAddresses): PredictionOrderSigningRoute {
        val userWalletId = addresses.userWalletId
        val sessionKeyAddress = sessionKeyStore.heldAddress(userWalletId) ?: return PredictionOrderSigningRoute.Owner

        val tradability = tradingStore.tradability(
            userWalletId = userWalletId,
            sessionKeyAddress = sessionKeyAddress,
            clock = clock,
            readWallet = { polymarketRepository.getWalletStatusByWalletId(walletId = userWalletId.stringValue) },
        )
        if (tradability is PolymarketSessionKeyTradability.Untradable) return owner(reason = tradability.reason)

        val identity = sessionCredentialsProvider.get(userWalletId)
            .getOrElse { error -> return owner(reason = "the session credentials could not be derived: $error") }
        if (!identity.address.equals(sessionKeyAddress, ignoreCase = true)) {
            return owner(reason = "the session credentials belong to another key")
        }

        return PredictionOrderSigningRoute.Session(identity = identity)
    }

    suspend fun reportRefused(
        addresses: PolymarketAddresses,
        identity: PolymarketClobIdentity.Session,
        error: PredictionPlaceOrderError,
    ) {
        TangemLogger.e(
            "The exchange refused the order signed with session key ${identity.address}, " +
                "signing it with the owner key: $error",
        )
        tradingStore.markRefused(addresses.userWalletId, identity.address)
    }

    suspend fun reportSigningFailed(
        addresses: PolymarketAddresses,
        identity: PolymarketClobIdentity.Session,
        failure: PredictionOrderFailure,
    ) {
        TangemLogger.e(
            "Session key ${identity.address} could not sign the order, signing it with the owner key: $failure",
        )
        tradingStore.markRefused(addresses.userWalletId, identity.address)
    }

    private fun owner(reason: String): PredictionOrderSigningRoute {
        TangemLogger.i("Polymarket order signed with the owner key: $reason")
        return PredictionOrderSigningRoute.Owner
    }
}