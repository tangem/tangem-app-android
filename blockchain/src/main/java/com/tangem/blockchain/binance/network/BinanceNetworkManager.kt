package com.tangem.blockchain.binance.network

import com.tangem.blockchain.binance.client.BinanceDexApiClientFactory
import com.tangem.blockchain.binance.client.BinanceDexEnvironment
import com.tangem.blockchain.binance.client.encoding.message.TransactionRequestAssemblerExtSign
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.extensions.Result
import com.tangem.blockchain.common.extensions.SimpleResult
import com.tangem.blockchain.common.extensions.retryIO
import com.tangem.blockchain.common.network.API_BINANCE
import com.tangem.blockchain.common.network.API_BINANCE_TESTNET
import com.tangem.blockchain.common.network.createRetrofitInstance
import okhttp3.RequestBody
import java.math.BigDecimal

class BinanceNetworkManager(isTestNet: Boolean) {
    val api = createRetrofitInstance(if (!isTestNet) API_BINANCE else API_BINANCE_TESTNET)
            .create(BinanceApi::class.java)
    val client = BinanceDexApiClientFactory.newInstance().newRestClient(
            if (!isTestNet) BinanceDexEnvironment.PROD.baseUrl else BinanceDexEnvironment.TEST_NET.baseUrl
    )

    suspend fun getInfo(address: String): Result<BinanceInfoResponse> {
        return try {
            val accountData = retryIO { client.getAccount(address) }

            var coinBalance = BigDecimal.ZERO
            for (balance in accountData.balances) {
                if (balance.symbol == "BNB") {
                    coinBalance = balance.free.toBigDecimal()
                    break
                }
            }

            Result.Success(BinanceInfoResponse(
                    balance = coinBalance,
                    accountNumber = accountData.accountNumber.toLong(),
                    sequence = accountData.sequence
            ))
        } catch (exception: Exception) {
            if (exception.message == "account not found") {
                Result.Success(BinanceInfoResponse(
                        balance = BigDecimal.ZERO, //TODO check account not found logic
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
                            ?.movePointLeft(Blockchain.Binance.decimals.toInt())
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
        val balance: BigDecimal?,
        val accountNumber: Long?,
        val sequence: Long?
)