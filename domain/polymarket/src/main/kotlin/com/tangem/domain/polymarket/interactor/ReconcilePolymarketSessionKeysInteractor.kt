package com.tangem.domain.polymarket.interactor

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.PolymarketSessionKeyRepository
import com.tangem.domain.polymarket.model.PolymarketClobIdentity
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.model.PolymarketSessionSigner
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.sessionkey.PolymarketDeviceIdProvider
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyAssessment
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyAssessor
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeySnapshot
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyStore
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyTradingStore
import com.tangem.domain.polymarket.sessionkey.lists
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketApiCredentialsUseCase
import com.tangem.utils.logging.TangemLogger
import java.time.Clock

@Suppress("LongParameterList")
class ReconcilePolymarketSessionKeysInteractor(
    private val polymarketRepository: PolymarketRepository,
    private val sessionKeyRepository: PolymarketSessionKeyRepository,
    private val sessionKeyStore: PolymarketSessionKeyStore,
    private val deviceIdProvider: PolymarketDeviceIdProvider,
    private val tradingStore: PolymarketSessionKeyTradingStore,
    private val deriveAddresses: DerivePolymarketAddressesUseCase,
    private val getApiCredentials: GetPolymarketApiCredentialsUseCase,
    private val clock: Clock = Clock.systemUTC(),
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
    ): Either<PolymarketWalletError, PolymarketSessionKeyOverview> = either {
        val wallet = polymarketRepository.getWalletStatusByWalletId(walletId = userWalletId.stringValue).bind()
        val localKeyAddress = sessionKeyStore.heldAddress(userWalletId)
        val signers = readSigners(userWalletId)
        val assessment = PolymarketSessionKeyAssessor.assess(
            PolymarketSessionKeySnapshot(
                sessionKeys = wallet.sessionKeys,
                localKeyAddress = localKeyAddress,
                deviceId = deviceIdProvider.deviceId(),
                signers = signers,
                now = clock.instant(),
            ),
        )
        tradingStore.setReconciliation(userWalletId, assessment.reconciliation)
        markListedKeyReady(
            userWalletId = userWalletId,
            wallet = wallet,
            localKeyAddress = localKeyAddress,
            signers = signers,
        )

        PolymarketSessionKeyOverview(wallet = wallet, localKeyAddress = localKeyAddress, assessment = assessment)
    }

    private suspend fun markListedKeyReady(
        userWalletId: UserWalletId,
        wallet: PolymarketWalletState,
        localKeyAddress: String?,
        signers: List<PolymarketSessionSigner>?,
    ) {
        val own = wallet.sessionKeys.firstOrNull { it.address.equals(localKeyAddress, ignoreCase = true) } ?: return
        val validUntil = own.validUntil ?: return
        val isListed = signers.orEmpty().any { it.lists(own, now = clock.instant()) }
        if (own.status != PolymarketSessionKeyStatus.ACTIVE || !isListed) return
        if (tradingStore.isRefused(userWalletId, own.address)) return
        if (!tradingStore.isReadyOnExchange(userWalletId, own.address, validUntil)) {
            TangemLogger.i("Polymarket lists the session key ${own.address}, marking it ready")
            tradingStore.markReadyOnExchange(userWalletId, own.address, validUntil)
        }
    }

    private suspend fun readSigners(userWalletId: UserWalletId): List<PolymarketSessionSigner>? {
        val addresses = deriveAddresses.stored(userWalletId) ?: return null
        val credentials = getApiCredentials(userWalletId) ?: return null
        return sessionKeyRepository
            .getSessionSigners(
                PolymarketClobIdentity.Owner(address = addresses.ownerAddress, credentials = credentials),
            )
            .onLeft { error -> TangemLogger.e("Polymarket session signers could not be read: $error") }
            .getOrNull()
    }
}

/**
 * @property localKeyAddress the key this device holds for the wallet, `null` if there is none
 */
data class PolymarketSessionKeyOverview(
    val wallet: PolymarketWalletState,
    val localKeyAddress: String?,
    val assessment: PolymarketSessionKeyAssessment,
)