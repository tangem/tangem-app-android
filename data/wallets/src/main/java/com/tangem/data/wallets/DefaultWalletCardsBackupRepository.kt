package com.tangem.data.wallets

import arrow.core.Either
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
import com.tangem.datasource.api.common.response.fold
import com.tangem.datasource.api.common.response.isNetworkError
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.backup.WalletCardBackup
import com.tangem.domain.wallets.models.errors.WalletCardsBackupError
import com.tangem.domain.wallets.repository.WalletCardsBackupRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.withContext
import java.util.UUID

internal class DefaultWalletCardsBackupRepository(
    private val tangemTechApi: TangemTechApi,
    private val pendingStore: PendingWalletCardsBackupStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : WalletCardsBackupRepository {

    override suspend fun saveWalletCards(
        userWalletId: UserWalletId,
        cards: List<WalletCardBackup>,
        usedSeed: Boolean,
    ): Either<WalletCardsBackupError, Unit> = withContext(dispatchers.io) {
        val pending = PendingWalletCardsBackup(
            id = UUID.randomUUID().toString(),
            walletId = userWalletId.stringValue,
            cards = PendingWalletCardsBackupConverter.convert(WalletCardDTOConverter.convertList(cards)),
            usedSeed = usedSeed,
        )

        // enqueued before the request leaves, so a report is never lost to a dead network or a killed process
        pendingStore.enqueue(pending)

        send(pending)
    }

    override suspend fun sendPendingWalletCards(): Either<WalletCardsBackupError, Unit> = withContext(dispatchers.io) {
        pendingStore.getAll().forEach { pending ->
            send(pending).onLeft { error ->
                when (error) {
                    // offline: this entry and every later one stay queued, so the order the backend
                    // receives them in still reflects the order the backup actually progressed in
                    WalletCardsBackupError.NoInternetConnection -> return@withContext error.left()
                    // the backend rejected the report itself — resending it can only fail again, and
                    // keeping it would wedge every later report behind it
                    is WalletCardsBackupError.Unexpected -> {
                        TangemLogger.e("Dropping rejected cards backup report wallet=${pending.walletId}: $error")
                        pendingStore.remove(pending.id)
                    }
                }
            }
        }

        Unit.right()
    }

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

    private suspend fun send(pending: PendingWalletCardsBackup): Either<WalletCardsBackupError, Unit> {
        // `fold` is not inline, and dequeuing on success suspends
        val response = tangemTechApi.saveWalletCards(
            walletId = pending.walletId,
            body = PendingWalletCardsBackupConverter.convertBack(pending),
        )

        return when (response) {
            is ApiResponse.Success -> {
                pendingStore.remove(pending.id)
                Unit.right()
            }
            is ApiResponse.Error -> {
                TangemLogger.e("saveWalletCards wallet=${pending.walletId} failed", response.cause)
                response.cause.toDomainError().left()
            }
        }
    }

    private fun ApiResponseError.toDomainError(): WalletCardsBackupError = when (this) {
        is ApiResponseError.NetworkException,
        is ApiResponseError.TimeoutException,
        -> WalletCardsBackupError.NoInternetConnection
        else -> WalletCardsBackupError.Unexpected(cause = this)
    }
}