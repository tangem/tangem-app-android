package com.tangem.data.transaction

import com.squareup.moshi.JsonAdapter
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.grow.datasource.gasless.TronGaslessApi
import com.tangem.grow.datasource.gasless.models.GaslessErrorResponse
import com.tangem.grow.datasource.gasless.models.tron.TronApproveRequest
import com.tangem.grow.datasource.gasless.models.tron.TronEstimateRequestBody
import com.tangem.grow.datasource.gasless.models.tron.TronEstimateResponse
import com.tangem.grow.datasource.gasless.models.tron.TronSubmitRequestBody
import com.tangem.grow.datasource.gasless.models.tron.TronSwapRequest
import com.tangem.domain.transaction.TronGaslessTransactionRepository
import com.tangem.domain.transaction.error.GaslessServiceException
import com.tangem.domain.transaction.models.tron.TronGaslessApproveEstimateParams
import com.tangem.domain.transaction.models.tron.TronGaslessEstimateParams
import com.tangem.domain.transaction.models.tron.TronGaslessQuote
import com.tangem.domain.transaction.models.tron.TronGaslessSubmitResult
import com.tangem.domain.transaction.models.tron.TronGaslessSwapEstimateParams
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

        response.result.toQuote(quotedAmountRaw = params.amount, quotedToAddress = params.toAddress)
    }

    override suspend fun estimateSwap(params: TronGaslessSwapEstimateParams): TronGaslessQuote =
        withContext(dispatchers.io) {
            val swap = params.swap
            val response = api.estimate(
                userWalletId = params.userWalletId.stringValue,
                body = TronEstimateRequestBody(
                    fromAddress = swap.fromAddress,
                    toAddress = null,
                    tokenContract = swap.tokenContract,
                    amount = swap.amount.toString(),
                    feeTokenContract = params.feeTokenContract,
                    swap = TronSwapRequest(
                        contractAddress = swap.routerAddress,
                        data = swap.callDataHex,
                        spender = swap.spenderAddress,
                    ),
                ),
            ).getOrThrowGaslessError(errorAdapter)
            if (!response.isSuccess) throw GaslessServiceException(httpCode = null, message = GASLESS_UNSUCCESSFUL)

            response.result.toQuote(quotedAmountRaw = swap.amount, quotedToAddress = swap.routerAddress)
                .copy(quotedSwap = swap)
        }

    override suspend fun estimateApprove(params: TronGaslessApproveEstimateParams): TronGaslessQuote =
        withContext(dispatchers.io) {
            val approve = params.approve
            val response = api.estimate(
                userWalletId = params.userWalletId.stringValue,
                body = TronEstimateRequestBody(
                    fromAddress = approve.fromAddress,
                    toAddress = null,
                    tokenContract = approve.tokenContract,
                    amount = approve.amount.toString(),
                    feeTokenContract = params.feeTokenContract,
                    approve = TronApproveRequest(spender = approve.spenderAddress),
                ),
            ).getOrThrowGaslessError(errorAdapter)
            if (!response.isSuccess) throw GaslessServiceException(httpCode = null, message = GASLESS_UNSUCCESSFUL)

            response.result.toQuote(quotedAmountRaw = approve.amount, quotedToAddress = approve.tokenContract)
                .copy(quotedApprove = approve)
        }

    private fun TronEstimateResponse.toQuote(quotedAmountRaw: BigInteger, quotedToAddress: String) = TronGaslessQuote(
        quoteId = quoteId,
        quotedAmountRaw = quotedAmountRaw,
        quotedToAddress = quotedToAddress,
        feeRecipient = feeRecipient,
        compensationToken = compensationToken,
        compensationAmountRaw = BigInteger(compensationAmountRaw),
        compensationAmountDecimal = BigDecimal(compensationAmount),
        energy = estimate.energy,
        bandwidth = estimate.bandwidth,
        trxCost = BigDecimal(estimate.trxCost),
        expiresAtEpochMs = Instant.parse(expiresAt).toEpochMilli(),
    )

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