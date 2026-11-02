package com.tangem.domain.polymarket.sessionkey

import arrow.core.getOrElse
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.PolymarketSessionKeyRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketClobIdentity
import com.tangem.domain.polymarket.model.PolymarketSessionSigner
import com.tangem.domain.polymarket.model.PredictionOrderFailure
import com.tangem.domain.polymarket.model.PredictionOrderSigningRoute
import com.tangem.domain.polymarket.model.PredictionPlaceOrderError
import com.tangem.domain.polymarket.usecase.GetPolymarketApiCredentialsUseCase
import com.tangem.utils.logging.TangemLogger
import java.time.Clock

@Suppress("LongParameterList")
class PolymarketOrderSigningRouter(
    private val polymarketRepository: PolymarketRepository,
    private val sessionKeyRepository: PolymarketSessionKeyRepository,
    private val sessionKeyStore: PolymarketSessionKeyStore,
    private val tradingStore: PolymarketSessionKeyTradingStore,
    private val sessionCredentialsProvider: PolymarketSessionCredentialsProvider,
    private val deviceIdProvider: PolymarketDeviceIdProvider,
    private val getApiCredentials: GetPolymarketApiCredentialsUseCase,
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

    suspend fun reconcile(addresses: PolymarketAddresses) {
        val userWalletId = addresses.userWalletId
        val state = polymarketRepository.getWalletStatusByWalletId(walletId = userWalletId.stringValue)
            .getOrElse { error ->
                TangemLogger.e("Polymarket session keys not reconciled, the wallet could not be read: $error")
                return
            }

        val assessment = PolymarketSessionKeyAssessor.assess(
            PolymarketSessionKeySnapshot(
                sessionKeys = state.sessionKeys,
                localKeyAddress = sessionKeyStore.heldAddress(userWalletId),
                deviceId = deviceIdProvider.deviceId(),
                signers = readSigners(addresses = addresses),
                now = clock.instant(),
            ),
        )
        tradingStore.setReconciliation(userWalletId, assessment.reconciliation)
    }

    private suspend fun readSigners(addresses: PolymarketAddresses): List<PolymarketSessionSigner>? =
        getApiCredentials(userWalletId = addresses.userWalletId)
            ?.let { credentials ->
                sessionKeyRepository
                    .getSessionSigners(
                        owner = PolymarketClobIdentity.Owner(
                            address = addresses.ownerAddress,
                            credentials = credentials,
                        ),
                    )
                    .onLeft { error -> TangemLogger.e("Polymarket session signers could not be read: $error") }
                    .getOrNull()
            }

    private fun owner(reason: String): PredictionOrderSigningRoute {
        TangemLogger.i("Polymarket order signed with the owner key: $reason")
        return PredictionOrderSigningRoute.Owner
    }
}