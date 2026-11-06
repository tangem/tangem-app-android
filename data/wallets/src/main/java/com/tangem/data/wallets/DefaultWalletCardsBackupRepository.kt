package com.tangem.data.wallets

import androidx.datastore.preferences.core.stringSetPreferencesKey
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException
import com.tangem.data.wallets.converters.PendingWalletCardsBackupConverter
import com.tangem.data.wallets.converters.WalletCardBackupConverter
import com.tangem.data.wallets.converters.WalletCardDTOConverter
import com.tangem.data.wallets.store.PendingWalletCardsBackup
import com.tangem.data.wallets.store.PendingWalletCardsBackupStore
import com.tangem.data.wallets.store.WalletCardsRegistryStore
import com.tangem.datasource.api.common.response.fold
import com.tangem.datasource.api.common.response.isNetworkError
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.datasource.api.tangemTech.models.WalletCardDTO
import com.tangem.domain.wallets.backup.WalletCardsBackupDeliveryScheduler
import com.tangem.domain.wallets.models.backup.WalletCardBackup
import com.tangem.domain.wallets.models.errors.WalletCardsBackupError
import com.tangem.domain.wallets.repository.WalletCardsBackupRepository
import com.tangem.features.onboarding.v2.OnboardingV2FeatureToggles
import com.tangem.utils.annotations.RemoveWithToggle
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

@Suppress("LongParameterList")
internal class DefaultWalletCardsBackupRepository(
    private val tangemTechApi: TangemTechApi,
    private val pendingStore: PendingWalletCardsBackupStore,
    private val registryStore: WalletCardsRegistryStore,
    @RemoveWithToggle("TWI_1586_CARD_LINKED_STATUS_UPDATE_V2") private val appPreferencesStore: AppPreferencesStore,
    @RemoveWithToggle(
        "TWI_1586_CARD_LINKED_STATUS_UPDATE_V2",
        description = "inline the true branch in saveWalletCards",
    )
    private val onboardingV2FeatureToggles: OnboardingV2FeatureToggles,
    private val deliveryScheduler: WalletCardsBackupDeliveryScheduler,
    private val dispatchers: CoroutineDispatcherProvider,
) : WalletCardsBackupRepository {

    /**
     * Onboarding fires each report from its own coroutine, so without this two reports would race and could
     * reach the backend in either order — or, with a drain running, be sent twice. Every send goes through
     * [drain], and only one drain runs at a time.
     */
    private val sendMutex = Mutex()

    override suspend fun saveWalletCards(
        userWalletId: UserWalletId,
        cards: List<WalletCardBackup>,
        usedSeed: Boolean,
    ): Either<WalletCardsBackupError, Unit> = withContext(dispatchers.io) {
        if (!onboardingV2FeatureToggles.isCardLinkedStatusUpdateV2Enabled) {
            val reportedCards = PendingWalletCardsBackupConverter.convert(WalletCardDTOConverter.convertList(cards))
            return@withContext queueAndSend(userWalletId, reportedCards, usedSeed)
        }

        val registration = register(userWalletId, cards).getOrElse { return@withContext it.left() }
        queueAndSend(userWalletId, registration.walletCards, usedSeed)
    }

    override suspend fun saveSeenWalletCard(
        userWalletId: UserWalletId,
        card: WalletCardBackup,
        usedSeed: Boolean,
    ): Either<WalletCardsBackupError, Unit> = withContext(dispatchers.io) {
        val registration = register(userWalletId, listOf(card)).getOrElse { return@withContext it.left() }
        if (!registration.isChanged) return@withContext Unit.right()

        queueAndSend(userWalletId, registration.walletCards, usedSeed)
    }

    override suspend fun sendPendingWalletCards(): Either<WalletCardsBackupError, Unit> =
        withContext(dispatchers.io) { drain() }

    override suspend fun getWalletCards(
        userWalletId: UserWalletId,
    ): Either<WalletCardsBackupError, List<WalletCardBackup>> = withContext(dispatchers.io) {
        tangemTechApi.getWalletCards(walletId = userWalletId.stringValue).fold(
            onSuccess = { response -> WalletCardBackupConverter.convertList(response.cards).right() },
            onError = { error ->
                // an unknown wallet is not a failure: the backend simply has no data about it, same as an
                // empty cards array
                if (error.isNetworkError(HttpException.Code.NOT_FOUND)) {
                    emptyList<WalletCardBackup>().right()
                } else {
                    TangemLogger.e("getWalletCards wallet=$userWalletId failed", error)
                    error.toDomainError().left()
                }
            },
        )
    }

    override suspend fun hasCardWithIncompleteBackup(userWalletId: UserWalletId): Boolean {
        return withContext(dispatchers.io) { getRegisteredCards(userWalletId.stringValue) }
            .any { it.backupStatus != WalletCardDTO.BackupStatus.ACTIVE }
    }

    @RemoveWithToggle("TWI_1586_CARD_LINKED_STATUS_UPDATE_V2")
    override suspend fun isCardLinkedReported(userWalletId: UserWalletId): Boolean {
        val reportedWalletIds = appPreferencesStore.getSyncOrDefault(CARD_LINKED_REPORTED_WALLET_IDS_KEY, emptySet())
        return userWalletId.stringValue in reportedWalletIds
    }

    @RemoveWithToggle("TWI_1586_CARD_LINKED_STATUS_UPDATE_V2")
    override suspend fun setCardLinkedReported(userWalletId: UserWalletId) {
        appPreferencesStore.editData { preferences ->
            preferences[CARD_LINKED_REPORTED_WALLET_IDS_KEY] =
                preferences.getOrDefault(CARD_LINKED_REPORTED_WALLET_IDS_KEY, emptySet()) + userWalletId.stringValue
        }
    }

    private suspend fun getRegisteredCards(walletId: String): List<PendingWalletCardsBackup.Card> {
        return runSuspendCatching { registryStore.getWalletCards(walletId) }
            .getOrElse { error ->
                TangemLogger.e("Wallet cards registry is unavailable", error)
                emptyList()
            }
    }

    private suspend fun register(
        userWalletId: UserWalletId,
        cards: List<WalletCardBackup>,
    ): Either<WalletCardsBackupError, WalletCardsRegistryStore.Registration> {
        val registeredCards = PendingWalletCardsBackupConverter.convert(WalletCardDTOConverter.convertList(cards))

        return runSuspendCatching { registryStore.register(userWalletId.stringValue, registeredCards) }
            .map { it.right() }
            .getOrElse { error ->
                TangemLogger.e("Unable to remember cards of wallet=$userWalletId", error)
                WalletCardsBackupError.Unexpected(cause = error).left()
            }
    }

    private suspend fun queueAndSend(
        userWalletId: UserWalletId,
        walletCards: List<PendingWalletCardsBackup.Card>,
        usedSeed: Boolean,
    ): Either<WalletCardsBackupError, Unit> {
        val pending = PendingWalletCardsBackup(
            id = UUID.randomUUID().toString(),
            walletId = userWalletId.stringValue,
            cards = walletCards,
            usedSeed = usedSeed,
        )

        // queued before the request leaves, so a report is never lost to a dead network or a killed process,
        // and sent by the same drain as everything else so an earlier report still waiting goes out first
        runSuspendCatching { pendingStore.enqueue(pending) }.getOrElse { error ->
            TangemLogger.e("Unable to queue cards backup report wallet=$userWalletId", error)
            return WalletCardsBackupError.Unexpected(cause = error).left()
        }

        return drain().onLeft { deliveryScheduler.schedule() }
    }

    /**
     * Sends the queued reports oldest first — the order the backend records them in is the order the backup
     * progressed in, so a report still waiting is never overtaken by a newer one.
     */
    private suspend fun drain(): Either<WalletCardsBackupError, Unit> = sendMutex.withLock {
        runSuspendCatching { drainQueue() }.getOrElse { error ->
            TangemLogger.e("Pending cards backup queue is unavailable", error)
            WalletCardsBackupError.Unexpected(cause = error).left()
        }
    }

    private suspend fun drainQueue(): Either<WalletCardsBackupError, Unit> {
        for (pending in pendingStore.getAll()) {
            when (val result = send(pending)) {
                SendResult.Delivered -> Unit
                // whatever stopped this one will stop the rest: leave them queued, in order, for the next
                // attempt rather than hammering a backend that is down
                is SendResult.Deferred -> return result.error.left()
                is SendResult.Rejected -> {
                    TangemLogger.e("Dropping rejected cards backup report wallet=${pending.walletId}: $result")
                    pendingStore.remove(pending.id)
                }
            }
        }

        return Unit.right()
    }

    private suspend fun send(pending: PendingWalletCardsBackup): SendResult {
        // `fold` is not inline, and dequeuing on success suspends
        val response = tangemTechApi.saveWalletCards(
            walletId = pending.walletId,
            body = PendingWalletCardsBackupConverter.convertBack(pending),
        )

        return when (response) {
            is ApiResponse.Success -> {
                pendingStore.remove(pending.id)
                TangemLogger.i("Cards backup report delivered wallet=${pending.walletId}")
                SendResult.Delivered
            }
            is ApiResponse.Error -> {
                TangemLogger.e("saveWalletCards wallet=${pending.walletId} failed", response.cause)
                response.cause.toSendResult()
            }
        }
    }

    private fun ApiResponseError.toSendResult(): SendResult = when (this) {
        is ApiResponseError.NetworkException,
        is ApiResponseError.TimeoutException,
        -> SendResult.Deferred(WalletCardsBackupError.NoInternetConnection)
        is HttpException -> if (isRetryable()) {
            SendResult.Deferred(WalletCardsBackupError.Unexpected(cause = this))
        } else {
            SendResult.Rejected(WalletCardsBackupError.Unexpected(cause = this))
        }
        // the response could not be read at all, which says nothing about whether the report was accepted —
        // dropping it here would lose a report the backend may never have seen
        is ApiResponseError.UnknownException -> SendResult.Deferred(WalletCardsBackupError.Unexpected(cause = this))
    }

    /**
     * Whether sending the same report again could succeed. A backend that is overloaded, restarting or behind
     * a failing gateway will accept the very same bytes once it recovers; one that refuses them outright
     * (malformed body, unknown wallet, a card missing from the attestation database) never will.
     */
    private fun HttpException.isRetryable(): Boolean = isServerError() ||
        code == HttpException.Code.TOO_MANY_REQUESTS ||
        code == HttpException.Code.REQUEST_TIMEOUT

    private fun ApiResponseError.toDomainError(): WalletCardsBackupError = when (this) {
        is ApiResponseError.NetworkException,
        is ApiResponseError.TimeoutException,
        -> WalletCardsBackupError.NoInternetConnection
        else -> WalletCardsBackupError.Unexpected(cause = this)
    }

    /** Outcome of sending one queued report, and what the queue should do with it */
    private sealed interface SendResult {

        /** The backend accepted the report; the entry is gone from the queue */
        data object Delivered : SendResult

        /** Sending failed for a reason that may pass: the entry stays queued and the drain stops */
        data class Deferred(val error: WalletCardsBackupError) : SendResult

        /** The backend refused the report itself: the entry is dropped so it cannot wedge the queue */
        data class Rejected(val error: WalletCardsBackupError) : SendResult
    }

    private companion object {

        @RemoveWithToggle("TWI_1586_CARD_LINKED_STATUS_UPDATE_V2")
        val CARD_LINKED_REPORTED_WALLET_IDS_KEY by lazy {
            stringSetPreferencesKey(name = "cardLinkedReportedWalletIds")
        }
    }
}