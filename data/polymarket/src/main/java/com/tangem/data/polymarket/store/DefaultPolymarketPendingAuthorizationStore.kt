package com.tangem.data.polymarket.store

import android.security.keystore.KeyPermanentlyInvalidatedException
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
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.security.UnrecoverableKeyException

internal class DefaultPolymarketPendingAuthorizationStore(
    private val secureStorage: SecureStorage,
    private val json: Json,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketPendingAuthorizationStore {

    override suspend fun get(
        userWalletId: UserWalletId,
    ): Either<PolymarketSessionKeyError, PolymarketPendingAuthorization?> = withContext(dispatchers.io) {
        runSuspendCatching { secureStorage.getAsString(storageKey(userWalletId)) }.fold(
            onSuccess = { payload -> payload?.let { decoded(userWalletId, it) }.right() },
            onFailure = { error -> unreadable(userWalletId, error) },
        )
    }

    override suspend fun store(
        userWalletId: UserWalletId,
        pending: PolymarketPendingAuthorization,
    ): Either<PolymarketSessionKeyError, Unit> = withContext(dispatchers.io) {
        Either.catch {
            val payload = json.encodeToString(PolymarketPendingAuthorizationDTO.serializer(), pending.toDTO())
            secureStorage.store(storageKey(userWalletId), payload)
        }.mapLeft(PolymarketSessionKeyError::Failure)
    }

    override suspend fun clear(userWalletId: UserWalletId) {
        withContext(dispatchers.io) {
            runSuspendCatching { secureStorage.delete(storageKey(userWalletId)) }.onFailure { error ->
                TangemLogger.e("Failed to clear the pending session key authorization of $userWalletId", error)
            }
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
    }
}

private fun Throwable.isPermanentKeyLoss(): Boolean = generateSequence(this) { it.cause }
    .any { it is KeyPermanentlyInvalidatedException || it is UnrecoverableKeyException }