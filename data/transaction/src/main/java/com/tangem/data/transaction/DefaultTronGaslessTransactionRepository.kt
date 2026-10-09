package com.tangem.data.transaction

import com.squareup.moshi.JsonAdapter
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.grow.datasource.gasless.TronGaslessApi
import com.tangem.grow.datasource.gasless.models.GaslessErrorResponse
import com.tangem.grow.datasource.gasless.models.tron.TronEstimateRequestBody
import com.tangem.grow.datasource.gasless.models.tron.TronSubmitRequestBody
import com.tangem.domain.transaction.TronGaslessTransactionRepository
import com.tangem.domain.transaction.error.GaslessServiceException
import com.tangem.domain.transaction.models.tron.TronGaslessEstimateParams
import com.tangem.domain.transaction.models.tron.TronGaslessQuote
import com.tangem.domain.transaction.models.tron.TronGaslessSubmitResult
import com.tangem.domain.transaction.models.tron.TronGaslessToken
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

internal class DefaultTronGaslessTransactionRepository(
    private val api: TronGaslessApi,
    private val errorAdapter: JsonAdapter<GaslessErrorResponse>,
    private val dispatchers: CoroutineDispatcherProvider,
) : TronGaslessTransactionRepository {

    private val supportedTokensMutex = Mutex()

    @Volatile
    private var supportedTokens: List<TronGaslessToken>? = null

    override suspend fun getSupportedTokens(): List<TronGaslessToken> = withContext(dispatchers.io) {
        supportedTokens ?: supportedTokensMutex.withLock {
            supportedTokens ?: fetchSupportedTokens().also { supportedTokens = it }
        }
    }

    private suspend fun fetchSupportedTokens(): List<TronGaslessToken> {
        val response = api.getSupportedTokens().getOrThrowGaslessError()
        if (!response.isSuccess) throw GaslessServiceException(httpCode = null, message = GASLESS_UNSUCCESSFUL)
        return response.result.tokens.map {
            TronGaslessToken(contractAddress = it.address, symbol = it.symbol, decimals = it.decimals)
        }
    }

    override suspend fun estimate(params: TronGaslessEstimateParams): TronGaslessQuote = withContext(dispatchers.io) {
        val response = api.estimate(
            userWalletId = params.userWalletId.stringValue,
            body = TronEstimateRequestBody(
                fromAddress = params.fromAddress,
                toAddress = params.toAddress,
                tokenContract = params.tokenContract,
                amount = params.amount.toString(),
                feeTokenContract = params.feeTokenContract,
            ),
        ).getOrThrowGaslessError(errorAdapter)
        if (!response.isSuccess) throw GaslessServiceException(httpCode = null, message = GASLESS_UNSUCCESSFUL)

        val res = response.result
        TronGaslessQuote(
            quoteId = res.quoteId,
            quotedAmountRaw = params.amount,
            quotedToAddress = params.toAddress,
            feeRecipient = res.feeRecipient,
            compensationToken = res.compensationToken,
            compensationAmountRaw = BigInteger(res.compensationAmountRaw),
            compensationAmountDecimal = BigDecimal(res.compensationAmount),
            energy = res.estimate.energy,
            bandwidth = res.estimate.bandwidth,
            trxCost = BigDecimal(res.estimate.trxCost),
            expiresAtEpochMs = Instant.parse(res.expiresAt).toEpochMilli(),
        )
    }

    override suspend fun submit(
        userWalletId: UserWalletId,
        quoteId: String,
        signedCompensationTx: String,
        signedOriginalTx: String,
    ): TronGaslessSubmitResult = withContext(dispatchers.io) {
        val response = api.submit(
            userWalletId = userWalletId.stringValue,
            body = TronSubmitRequestBody(
                quoteId = quoteId,
                signedCompensationTx = signedCompensationTx,
                signedOriginalTx = signedOriginalTx,
            ),
        ).getOrThrowGaslessError(errorAdapter)
        if (!response.isSuccess) throw GaslessServiceException(httpCode = null, message = GASLESS_UNSUCCESSFUL)

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