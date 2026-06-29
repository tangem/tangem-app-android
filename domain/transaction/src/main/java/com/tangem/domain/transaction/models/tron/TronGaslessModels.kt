package com.tangem.domain.transaction.models.tron

import java.math.BigDecimal
import java.math.BigInteger

data class TronGaslessEstimateParams(
    val fromAddress: String,
    val toAddress: String,
    val tokenContract: String,
    val amount: String,
    val feeTokenContract: String?,
)

data class TronGaslessQuote(
    val quoteId: String,
    val feeRecipient: String,
    val compensationToken: String,
    val compensationAmountRaw: BigInteger,
    val compensationAmountDecimal: BigDecimal,
    val energy: Long,
    val bandwidth: Long,
    val trxCostSun: BigInteger,
    val expiresAtEpochMs: Long,
)

data class TronGaslessSubmitResult(
    val compensationTxHash: String,
    val originalTxHash: String,
    val status: String,
)

data class TronGaslessToken(
    val contractAddress: String,
    val symbol: String,
    val decimals: Int,
)