package com.tangem.data.polymarket.store

import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.datastore.core.DataStore
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.common.services.secure.SecureStorage
import com.tangem.data.polymarket.entity.PolymarketPendingAuthorizationDTO
import com.tangem.data.polymarket.entity.toDTO
import com.tangem.data.polymarket.entity.toDomain
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.sessionkey.PolymarketPendingAuthorization
import com.tangem.domain.polymarket.sessionkey.PolymarketPendingAuthorizationStore
import com.tangem.domain.polymarket.signing.PolymarketRelayerDeadline
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.security.UnrecoverableKeyException
import java.time.Clock

internal class DefaultPolymarketPendingAuthorizationStore(
    private val secureStorage: SecureStorage,
    private val inFlightUntil: DataStore<Map<String, String>>,
    private val json: Json,
    private val dispatchers: CoroutineDispatcherProvider,
    private val clock: Clock = Clock.systemUTC(),
) : PolymarketPendingAuthorizationStore {

    override suspend fun get(
        userWalletId: UserWalletId,
    ): Either<PolymarketSessionKeyError, PolymarketPendingAuthorization?> = withContext(dispatchers.io) {
        runSuspendCatching { secureStorage.getAsString(storageKey(userWalletId)) }.fold(
            onSuccess = { payload ->
                if (payload != null) decoded(userWalletId, payload) else missing(userWalletId)
            },
            onFailure = { error -> unreadable(userWalletId, error) },
        )
    }

    override suspend fun store(
        userWalletId: UserWalletId,
        pending: PolymarketPendingAuthorization,
    ): Either<PolymarketSessionKeyError, Unit> = withContext(dispatchers.io) {
        val lastDeadline = pending.lastDeadline() ?: run {
            TangemLogger.e("Pending session key authorization of $userWalletId not kept: deadline is not a number")
            return@withContext PolymarketSessionKeyError.Failure(IllegalArgumentException(UNPARSEABLE_DEADLINE)).left()
        }
        Either.catch {
            inFlightUntil.updateData { marked -> marked + (userWalletId.markerKey() to lastDeadline.toString()) }
            val payload = json.encodeToString(PolymarketPendingAuthorizationDTO.serializer(), pending.toDTO())
            secureStorage.store(storageKey(userWalletId), payload)
        }.onLeft { clearMarker(userWalletId) }.mapLeft(PolymarketSessionKeyError::Failure)
    }

    override suspend fun clear(userWalletId: UserWalletId) {
        withContext(dispatchers.io) {
            runSuspendCatching { secureStorage.delete(storageKey(userWalletId)) }.onFailure { error ->
                TangemLogger.e("Failed to clear the pending session key authorization of $userWalletId", error)
            }
            clearMarker(userWalletId)
        }
    }

    private suspend fun missing(userWalletId: UserWalletId): Either<PolymarketSessionKeyError, Nothing?> {
        val marked = runSuspendCatching { inFlightUntil.data.first() }
            .getOrElse { error ->
                TangemLogger.e("Pending session key authorization marker of $userWalletId unreadable", error)
                return PolymarketSessionKeyError.Failure(error).left()
            }
        val markerKeys = listOf(userWalletId.markerKey(), ANY_WALLET_MARKER_KEY).filter { it in marked }
        val (expired, inFlight) = markerKeys.partition { key ->
            PolymarketRelayerDeadline.isExpired(deadline = marked.getValue(key), nowMillis = clock.millis())
        }
        expired.forEach { key -> clearMarker(key) }
        if (inFlight.isEmpty()) return null.right()
        TangemLogger.e("Pending session key authorization of $userWalletId unreadable while it may still land")
        return PolymarketSessionKeyError.Failure(IllegalStateException(UNREADABLE_IN_FLIGHT)).left()
    }

    private suspend fun clearMarker(userWalletId: UserWalletId) {
        clearMarker(userWalletId.markerKey())
    }

    private suspend fun clearMarker(markerKey: String) {
        runSuspendCatching { inFlightUntil.updateData { marked -> marked - markerKey } }
            .onFailure { error ->
                TangemLogger.e("Failed to clear the pending session key authorization marker $markerKey", error)
            }
    }

    private suspend fun decoded(
        userWalletId: UserWalletId,
        payload: String,
    ): Either<PolymarketSessionKeyError, PolymarketPendingAuthorization?> = runSuspendCatching {
        json.decodeFromString(PolymarketPendingAuthorizationDTO.serializer(), payload).toDomain()
    }.fold(
        onSuccess = { it.right() },
        onFailure = { error -> drop(userWalletId, error) },
    )

    private suspend fun unreadable(
        userWalletId: UserWalletId,
        error: Throwable,
    ): Either<PolymarketSessionKeyError, PolymarketPendingAuthorization?> = if (error.isPermanentKeyLoss()) {
        drop(userWalletId, error)
    } else {
        TangemLogger.e("Pending session key authorization of $userWalletId unreadable: ${error::class.simpleName}")
        PolymarketSessionKeyError.Failure(error).left()
    }

    private suspend fun drop(
        userWalletId: UserWalletId,
        error: Throwable,
    ): Either<PolymarketSessionKeyError, PolymarketPendingAuthorization?> {
        TangemLogger.e("Pending session key authorization of $userWalletId dropped: ${error::class.simpleName}")
        runSuspendCatching { secureStorage.delete(storageKey(userWalletId)) }.onFailure { deleteError ->
            TangemLogger.e("Failed to drop the pending session key authorization of $userWalletId", deleteError)
        }
        return missing(userWalletId)
    }

    private fun storageKey(userWalletId: UserWalletId): String = KEY_PREFIX + userWalletId.stringValue.lowercase()

    private companion object {
        const val KEY_PREFIX = "polymarket_pending_authorization_"
        const val UNREADABLE_IN_FLIGHT = "Pending authorization unreadable while it may still land"
        const val UNPARSEABLE_DEADLINE = "Pending authorization deadline is not a number"
    }
}

internal const val ANY_WALLET_MARKER_KEY = "*"

internal fun inFlightMarkersAfterCorruption(nowMillis: Long): Map<String, String> = mapOf(
    ANY_WALLET_MARKER_KEY to PolymarketRelayerDeadline.fromNow(
        nowMillis = nowMillis,
        windowSeconds = PolymarketRelayerDeadline.FOLLOW_UP_WINDOW_SECONDS,
    ),
)

private fun UserWalletId.markerKey(): String = stringValue.lowercase()

private fun PolymarketPendingAuthorization.lastDeadline(): Long? {
    val revocationDeadline = revocation.deadline.toLongOrNull() ?: return null
    val authorizationDeadline = authorization.deadline.toLongOrNull() ?: return null
    return maxOf(revocationDeadline, authorizationDeadline)
}

private fun Throwable.isPermanentKeyLoss(): Boolean = generateSequence(this) { it.cause }
    .any { it is KeyPermanentlyInvalidatedException || it is UnrecoverableKeyException }