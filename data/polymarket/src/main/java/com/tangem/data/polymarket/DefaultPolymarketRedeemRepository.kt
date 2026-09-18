package com.tangem.data.polymarket

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.polymarket.error.PolymarketRedeemErrorResolver
import com.tangem.domain.polymarket.PolymarketRedeemRepository
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PolymarketRedeemError
import com.tangem.domain.polymarket.model.PredictionClaimBatch
import com.tangem.domain.polymarket.model.PredictionClaimReceipt
import com.tangem.domain.polymarket.model.PredictionClaimState
import com.tangem.grow.datasource.polymarket.PolymarketApi
import com.tangem.grow.datasource.polymarket.models.PolymarketApprovalCallDto
import com.tangem.grow.datasource.polymarket.models.PolymarketRelayedBatchRequest
import com.tangem.grow.datasource.polymarket.models.PolymarketRelayedBatchResponse
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultPolymarketRedeemRepository @Inject constructor(
    private val polymarketApi: PolymarketApi,
    private val errorResolver: PolymarketRedeemErrorResolver,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketRedeemRepository {

    override suspend fun getCall(
        walletId: String,
        conditionId: String,
        isNegRisk: Boolean,
    ): Either<PolymarketRedeemError, PolymarketApprovalCall> = withContext(dispatchers.io) {
        safeApiCall(
            call = {
                polymarketApi.getRedeemCall(
                    walletId = walletId,
                    conditionId = conditionId,
                    isNegRisk = isNegRisk,
                ).bind().toCall().right()
            },
            onError = { errorResolver.resolve(it).left() },
        )
    }

    override suspend fun submit(
        walletId: String,
        batch: PredictionClaimBatch,
    ): Either<PolymarketRedeemError, PredictionClaimReceipt> = withContext(dispatchers.io) {
        safeApiCall(
            call = {
                polymarketApi.submitRedeem(walletId = walletId, request = batch.toRequest())
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
    ): Either<PolymarketRedeemError, PredictionClaimReceipt> = withContext(dispatchers.io) {
        safeApiCall(
            call = {
                polymarketApi.getRedeemState(walletId = walletId, transactionId = transactionId)
                    .bind()
                    .toReceipt()
                    .right()
            },
            onError = { errorResolver.resolve(it).left() },
        )
    }

    private fun PolymarketApprovalCallDto.toCall() = PolymarketApprovalCall(
        target = target,
        value = value,
        data = data,
    )

    private fun PredictionClaimBatch.toRequest() = PolymarketRelayedBatchRequest(
        ownerAddress = ownerAddress,
        depositWalletAddress = depositWalletAddress,
        nonce = nonce,
        deadline = deadline,
        calls = listOf(PolymarketApprovalCallDto(target = call.target, value = call.value, data = call.data)),
        signature = signature,
    )

    private fun PolymarketRelayedBatchResponse.toReceipt() = PredictionClaimReceipt(
        transactionId = transactionId,
        state = PredictionClaimState.fromRaw(state),
        transactionHash = transactionHash,
    )
}