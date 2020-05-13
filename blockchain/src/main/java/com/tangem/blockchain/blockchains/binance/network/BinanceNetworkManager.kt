package com.tangem.blockchain.blockchains.binance.network

import com.tangem.blockchain.blockchains.binance.client.BinanceDexApiClientFactory
import com.tangem.blockchain.blockchains.binance.client.BinanceDexApiRestClient
import com.tangem.blockchain.blockchains.binance.client.BinanceDexEnvironment
import com.tangem.blockchain.blockchains.binance.client.encoding.message.TransactionRequestAssemblerExtSign
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.API_BINANCE
import com.tangem.blockchain.network.API_BINANCE_TESTNET
import com.tangem.blockchain.network.createRetrofitInstance
import okhttp3.RequestBody
import java.math.BigDecimal

class BinanceNetworkManager(isTestNet: Boolean = false) {
    val api: BinanceApi by lazy {
        createRetrofitInstance(if (!isTestNet) API_BINANCE else API_BINANCE_TESTNET)
                .create(BinanceApi::class.java)
    }
    val client: BinanceDexApiRestClient by lazy {
        BinanceDexApiClientFactory.newInstance().newRestClient(
                if (!isTestNet) BinanceDexEnvironment.PROD.baseUrl else BinanceDexEnvironment.TEST_NET.baseUrl
        )
    }

    suspend fun getInfo(address: String, assetCode: String? = null): Result<BinanceInfoResponse> {
        return try {
            val accountData = retryIO { client.getAccount(address) }

            var coinBalance = BigDecimal.ZERO
            var assetBalance = BigDecimal.ZERO
            for (balance in accountData.balances) {
                when (balance.symbol) {
                    "BNB" -> coinBalance = balance.free.toBigDecimal()
                    assetCode -> assetBalance = balance.free.toBigDecimal()
                }
            }

            Result.Success(BinanceInfoResponse(
                    balance = coinBalance,
                    assetBalance = assetBalance,
                    accountNumber = accountData.accountNumber.toLong(),
                    sequence = accountData.sequence
            ))
        } catch (exception: Exception) {
            if (exception.message == "account not found") {
                Result.Success(BinanceInfoResponse(
                        balance = BigDecimal.ZERO, //TODO check account not found logic
                        assetBalance = null,
                        accountNumber = null,
                        sequence = null
                ))
            } else {
                Result.Failure(exception)
            }
        }
    }

    suspend fun getFee(): Result<BigDecimal> {
        return try {
            val feeData = api.getFees()
            var fee: BigDecimal? = null
            for (binanceFee in feeData) {
                if (binanceFee.transactionFee != null) {
                    fee = binanceFee.transactionFee?.value?.toBigDecimal()
                            ?.movePointLeft(Blockchain.Binance.decimals())
                    break
                }
            }
            return Result.Success(fee ?: throw Exception("Invalid fee response"))
        } catch (exception: Exception) {
            Result.Failure(exception)
        }
    }

    suspend fun sendTransaction(transaction: ByteArray): SimpleResult {
        return try {
            val requestBody: RequestBody = TransactionRequestAssemblerExtSign.createRequestBody(transaction)
            val response = retryIO { client.broadcastNoWallet(requestBody, true) }
            if (response.isNotEmpty() && response[0].isOk) {
                SimpleResult.Success
            } else {
                SimpleResult.Failure(Exception("transaction failed"))
            }
        } catch (error: Exception) {
            SimpleResult.Failure(error)
        }
    }
}

data class BinanceInfoResponse(
        val balance: BigDecimal,
        val assetBalance: BigDecimal?,
        val accountNumber: Long?,
        val sequence: Long?
)