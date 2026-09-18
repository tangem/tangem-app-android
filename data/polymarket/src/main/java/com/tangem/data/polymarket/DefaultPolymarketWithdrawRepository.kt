package com.tangem.data.polymarket

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.polymarket.error.PredictionWithdrawErrorResolver
import com.tangem.domain.polymarket.PolymarketWithdrawRepository
import com.tangem.domain.polymarket.model.PredictionWithdrawError
import com.tangem.domain.polymarket.model.PredictionWithdrawalBatch
import com.tangem.domain.polymarket.model.PredictionWithdrawalReceipt
import com.tangem.domain.polymarket.model.PredictionWithdrawalState
import com.tangem.grow.datasource.polymarket.PolymarketApi
import com.tangem.grow.datasource.polymarket.models.PolymarketApprovalCallDto
import com.tangem.grow.datasource.polymarket.models.PolymarketRelayedBatchRequest
import com.tangem.grow.datasource.polymarket.models.PolymarketRelayedBatchResponse
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultPolymarketWithdrawRepository @Inject constructor(
    private val polymarketApi: PolymarketApi,
    private val errorResolver: PredictionWithdrawErrorResolver,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketWithdrawRepository {

    override suspend fun submit(
        walletId: String,
        batch: PredictionWithdrawalBatch,
    ): Either<PredictionWithdrawError, PredictionWithdrawalReceipt> = withContext(dispatchers.io) {
        safeApiCall(
            call = {
                polymarketApi.submitWithdraw(walletId = walletId, request = batch.toRequest())
                    .bind()
                    .toReceipt()
                    .right()
            },
            onError = { errorResolver.resolve(it).left() },
        )
    }

    override suspend fun getState(
        walletId: String,
        transactionId: String,
    ): Either<PredictionWithdrawError, PredictionWithdrawalReceipt> = withContext(dispatchers.io) {
        safeApiCall(
            call = {
                polymarketApi.getWithdrawState(walletId = walletId, transactionId = transactionId)
                    .bind()
                    .toReceipt()
                    .right()
            },
            onError = { errorResolver.resolve(it).left() },
        )
    }

    private fun PredictionWithdrawalBatch.toRequest() = PolymarketRelayedBatchRequest(
        ownerAddress = ownerAddress,
        depositWalletAddress = depositWalletAddress,
        nonce = nonce,
        deadline = deadline,
        calls = listOf(PolymarketApprovalCallDto(target = call.target, value = call.value, data = call.data)),
        signature = signature,
    )

    private fun PolymarketRelayedBatchResponse.toReceipt() = PredictionWithdrawalReceipt(
        transactionId = transactionId,
        state = PredictionWithdrawalState.fromRaw(state),
        transactionHash = transactionHash,
    )
}