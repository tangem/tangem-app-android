package com.tangem.data.wallets

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException
import com.tangem.data.wallets.converters.WalletCardBackupConverter
import com.tangem.data.wallets.converters.WalletCardDTOConverter
import com.tangem.datasource.api.common.response.fold
import com.tangem.datasource.api.common.response.isNetworkError
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.WalletCardsBody
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.backup.WalletCardBackup
import com.tangem.domain.wallets.models.errors.WalletCardsBackupError
import com.tangem.domain.wallets.repository.WalletCardsBackupRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.withContext

internal class DefaultWalletCardsBackupRepository(
    private val tangemTechApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : WalletCardsBackupRepository {

    override suspend fun saveWalletCards(
        userWalletId: UserWalletId,
        cards: List<WalletCardBackup>,
        usedSeed: Boolean,
    ): Either<WalletCardsBackupError, Unit> = withContext(dispatchers.io) {
        tangemTechApi.saveWalletCards(
            walletId = userWalletId.stringValue,
            body = WalletCardsBody(
                cards = WalletCardDTOConverter.convertList(cards),
                usedSeed = usedSeed,
            ),
        ).fold(
            onSuccess = { Unit.right() },
            onError = { error ->
                TangemLogger.e("saveWalletCards wallet=$userWalletId failed", error)
                error.toDomainError().left()
            },
        )
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

    private fun ApiResponseError.toDomainError(): WalletCardsBackupError = when (this) {
        is ApiResponseError.NetworkException,
        is ApiResponseError.TimeoutException,
        -> WalletCardsBackupError.NoInternetConnection
        else -> WalletCardsBackupError.Unexpected(cause = this)
    }
}