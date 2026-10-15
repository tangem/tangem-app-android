package com.tangem.data.polymarket

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.polymarket.converter.PolymarketMarketVersionConverter
import com.tangem.data.polymarket.error.PolymarketRedeemErrorResolver
import com.tangem.domain.polymarket.PolymarketRedeemRepository
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PolymarketRedeemError
import com.tangem.domain.polymarket.model.PredictionClaimBatch
import com.tangem.domain.polymarket.model.PredictionClaimReceipt
import com.tangem.domain.polymarket.model.PredictionClaimState
import com.tangem.domain.polymarket.model.PredictionRedeemCalls
import com.tangem.grow.datasource.polymarket.PolymarketApi
import com.tangem.grow.datasource.polymarket.models.PolymarketApprovalCallDto
import com.tangem.grow.datasource.polymarket.models.PolymarketRedeemCallResponse
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

    override suspend fun getCalls(
        walletId: String,
        conditionId: String,
        isNegRisk: Boolean,
    ): Either<PolymarketRedeemError, PredictionRedeemCalls> = withContext(dispatchers.io) {
        safeApiCall(
            call = {
                polymarketApi.getRedeemCall(
                    walletId = walletId,
                    conditionId = conditionId,
                    isNegRisk = isNegRisk,
                ).bind().toCalls()
            },
            onError = { errorResolver.resolveCallLookup(it).left() },
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

    /** A backend that predates `calls` sends only the flat call; one that sends `calls` means exactly what it lists. */
    private fun PolymarketRedeemCallResponse.toCalls(): Either<PolymarketRedeemError, PredictionRedeemCalls> {
        val calls = calls
        val target = target
        val value = value
        val data = data
        val served = when {
            calls != null -> calls.map { it.toCall() }
            target != null && value != null && data != null -> {
                listOf(PolymarketApprovalCall(target = target, value = value, data = data))
            }
            else -> return unexpected("Redeem call answer holds neither calls nor a complete flat call").left()
        }
        val nonEmpty = served.toNonEmptyListOrNull() ?: return unexpected("Redeem call answer holds no calls").left()
        return PredictionRedeemCalls(
            version = version?.let(PolymarketMarketVersionConverter::convert),
            calls = nonEmpty,
        ).right()
    }

    private fun unexpected(detail: String) = PolymarketRedeemError.Unexpected(httpCode = null, detail = detail)

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
        calls = calls.map { PolymarketApprovalCallDto(target = it.target, value = it.value, data = it.data) },
        signature = signature,
    )

    private fun PolymarketRelayedBatchResponse.toReceipt() = PredictionClaimReceipt(
        transactionId = transactionId,
        state = PredictionClaimState.fromRaw(state),
        transactionHash = transactionHash,
    )
}