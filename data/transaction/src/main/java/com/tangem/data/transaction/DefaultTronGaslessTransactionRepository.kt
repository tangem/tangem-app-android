package com.tangem.data.transaction

import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.gasless.TronGaslessApi
import com.tangem.datasource.api.gasless.models.tron.TronEstimateRequestBody
import com.tangem.datasource.api.gasless.models.tron.TronSubmitRequestBody
import com.tangem.domain.transaction.TronGaslessTransactionRepository
import com.tangem.domain.transaction.models.tron.TronGaslessEstimateParams
import com.tangem.domain.transaction.models.tron.TronGaslessQuote
import com.tangem.domain.transaction.models.tron.TronGaslessSubmitResult
import com.tangem.domain.transaction.models.tron.TronGaslessToken
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

internal class DefaultTronGaslessTransactionRepository(
    private val api: TronGaslessApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : TronGaslessTransactionRepository {

    override suspend fun getSupportedTokens(): List<TronGaslessToken> = withContext(dispatchers.io) {
        val response = api.getSupportedTokens().getOrThrow()
        check(response.isSuccess) { GASLESS_UNSUCCESSFUL }
        response.result.tokens.map {
            TronGaslessToken(contractAddress = it.address, symbol = it.symbol, decimals = it.decimals)
        }
    }

    override suspend fun estimate(params: TronGaslessEstimateParams): TronGaslessQuote = withContext(dispatchers.io) {
        val response = api.estimate(
            TronEstimateRequestBody(
                fromAddress = params.fromAddress,
                toAddress = params.toAddress,
                tokenContract = params.tokenContract,
                amount = params.amount,
                feeTokenContract = params.feeTokenContract,
            ),
        ).getOrThrow()
        check(response.isSuccess) { GASLESS_UNSUCCESSFUL }

        val res = response.result
        TronGaslessQuote(
            quoteId = res.quoteId,
            feeRecipient = res.feeRecipient,
            compensationToken = res.compensationToken,
            compensationAmountRaw = BigInteger(res.compensationAmountRaw),
            compensationAmountDecimal = BigDecimal(res.compensationAmount),
            energy = res.estimate.energy,
            bandwidth = res.estimate.bandwidth,
            trxCostSun = BigInteger(res.estimate.trxCost),
            expiresAtEpochMs = Instant.parse(res.expiresAt).toEpochMilli(),
        )
    }

    override suspend fun submit(
        quoteId: String,
        signedCompensationTx: String,
        signedOriginalTx: String,
    ): TronGaslessSubmitResult = withContext(dispatchers.io) {
        val response = api.submit(
            TronSubmitRequestBody(
                quoteId = quoteId,
                signedCompensationTx = signedCompensationTx,
                signedOriginalTx = signedOriginalTx,
            ),
        ).getOrThrow()
        check(response.isSuccess) { GASLESS_UNSUCCESSFUL }

        val res = response.result
        TronGaslessSubmitResult(
            compensationTxHash = res.compensationTxHash,
            originalTxHash = res.originalTxHash,
            status = res.status,
        )
    }

    private companion object {
        const val GASLESS_UNSUCCESSFUL = "Tron gasless service returned unsuccessful response"
    }
}