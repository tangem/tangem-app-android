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
                if (payload != null) decoded(userWalletId, payload).right() else missing(userWalletId)
            },
            onFailure = { error -> unreadable(userWalletId, error) },
        )
    }

    override suspend fun store(
        userWalletId: UserWalletId,
        pending: PolymarketPendingAuthorization,
    ): Either<PolymarketSessionKeyError, Unit> = withContext(dispatchers.io) {
        Either.catch {
            inFlightUntil.updateData { marked -> marked + (userWalletId.markerKey() to pending.lastDeadline()) }
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
        val inFlightUntil = runSuspendCatching { inFlightUntil.data.first()[userWalletId.markerKey()] }
            .getOrElse { error ->
                TangemLogger.e("Pending session key authorization marker of $userWalletId unreadable", error)
                return PolymarketSessionKeyError.Failure(error).left()
            }
            ?: return null.right()
        if (PolymarketRelayerDeadline.isExpired(deadline = inFlightUntil, nowMillis = clock.millis())) {
            clearMarker(userWalletId)
            return null.right()
        }
        TangemLogger.e("Pending session key authorization of $userWalletId unreadable while it may still land")
        return PolymarketSessionKeyError.Failure(IllegalStateException(UNREADABLE_IN_FLIGHT)).left()
    }

    private suspend fun clearMarker(userWalletId: UserWalletId) {
        runSuspendCatching { inFlightUntil.updateData { marked -> marked - userWalletId.markerKey() } }
            .onFailure { error ->
                TangemLogger.e("Failed to clear the pending session key authorization marker of $userWalletId", error)
            }
    }

    private suspend fun decoded(userWalletId: UserWalletId, payload: String): PolymarketPendingAuthorization? =
        runSuspendCatching {
            json.decodeFromString(PolymarketPendingAuthorizationDTO.serializer(), payload).toDomain()
        }.getOrElse { error ->
            drop(userWalletId, error)
            null
        }

    private suspend fun unreadable(
        userWalletId: UserWalletId,
        error: Throwable,
    ): Either<PolymarketSessionKeyError, PolymarketPendingAuthorization?> = if (error.isPermanentKeyLoss()) {
        drop(userWalletId, error)
        null.right()
    } else {
        TangemLogger.e("Pending session key authorization of $userWalletId unreadable: ${error::class.simpleName}")
        PolymarketSessionKeyError.Failure(error).left()
    }

    private suspend fun drop(userWalletId: UserWalletId, error: Throwable) {
        TangemLogger.e("Pending session key authorization of $userWalletId dropped: ${error::class.simpleName}")
        clear(userWalletId)
    }

    private fun storageKey(userWalletId: UserWalletId): String = KEY_PREFIX + userWalletId.stringValue.lowercase()

    private companion object {
        const val KEY_PREFIX = "polymarket_pending_authorization_"
        const val UNREADABLE_IN_FLIGHT = "Pending authorization unreadable while it may still land"
    }
}

private fun UserWalletId.markerKey(): String = stringValue.lowercase()

private fun PolymarketPendingAuthorization.lastDeadline(): String =
    maxOf(revocation.deadline.toLongOrNull() ?: 0L, authorization.deadline.toLongOrNull() ?: 0L).toString()

private fun Throwable.isPermanentKeyLoss(): Boolean = generateSequence(this) { it.cause }
    .any { it is KeyPermanentlyInvalidatedException || it is UnrecoverableKeyException }