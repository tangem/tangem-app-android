package com.tangem.blockchain.blockchains.ethereum.network

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.retryIO
import com.tangem.blockchain.network.API_INFURA
import com.tangem.blockchain.network.API_RSK
import com.tangem.blockchain.network.createRetrofitInstance
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.kethereum.ETH_IN_WEI
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode


class EthereumNetworkManager(blockchain: Blockchain) {
    private val infuraPath = "v3/"

    private val api: EthereumApi by lazy {
        val baseUrl = when (blockchain) {
            Blockchain.Ethereum -> API_INFURA + infuraPath
            Blockchain.RSK -> API_RSK
            else -> throw Exception("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
        }
        createRetrofitInstance(baseUrl).create(EthereumApi::class.java)
    }

    private val apiKey = when (blockchain) {
        Blockchain.Ethereum -> INFURA_API_KEY
        Blockchain.RSK -> ""
        else -> throw Exception("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
    }

    private val provider: EthereumProvider by lazy { EthereumProvider(api, apiKey) }

    suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            val response = retryIO { provider.sendTransaction(transaction) }
            if (response.error == null) {
                SimpleResult.Success
            } else {
                SimpleResult.Failure(Exception("Code: ${(response.error.code)}, ${(response.error.message)}"))
            }
        } catch (error: Exception) {
            SimpleResult.Failure(error)
        }
    }

    suspend fun getFee(gasLimit: Long): Result<List<BigDecimal>> {
        return try {
            Result.Success(
                    provider.getGasPrice().result!!.parseFee(gasLimit)
            )
        } catch (error: Exception) {
            Result.Failure(error)
        }
    }

    suspend fun getInfo(address: String, contractAddress: String? = null): Result<EthereumInfoResponse> {
        return try {
            coroutineScope {
                val balanceResponse = retryIO { async { provider.getBalance(address) } }
                val txCountResponse = retryIO { async { provider.getTxCount(address) } }
                val pendingTxCountResponse = retryIO { async { provider.getPendingTxCount(address) } }
                var tokenBalanceResponse: Deferred<EthereumResponse>? = null
                if (contractAddress != null) {
                    tokenBalanceResponse = retryIO { async { provider.getTokenBalance(address, contractAddress) } }
                }
                Result.Success(EthereumInfoResponse(
                        balanceResponse.await().result!!.parseAmount(),
                        tokenBalanceResponse?.await()?.result?.parseAmount(),
                        txCountResponse.await().result?.responseToNumber()?.toLong() ?: 0,
                        pendingTxCountResponse.await().result?.responseToNumber()?.toLong() ?: 0
                ))
            }
        } catch (error: Exception) {
            Result.Failure(error)
        }
    }

    private fun String.parseFee(gasLimit: Long): List<BigDecimal> {
        val gasPrice = this.responseToNumber().toBigDecimal()
        val minFee = gasPrice.multiply(gasLimit.toBigDecimal())
        val normalFee = minFee.multiply(BigDecimal(1.2)).setScale(0, RoundingMode.HALF_UP)
        val priorityFee = minFee.multiply(BigDecimal(1.5)).setScale(0, RoundingMode.HALF_UP)
        return listOf(
                minFee.convertFeeToEth(),
                normalFee.convertFeeToEth(),
                priorityFee.convertFeeToEth()
        )
    }

    private fun String.responseToNumber(): BigInteger = this.substring(2).toBigInteger(16)

    private fun String.parseAmount(): BigDecimal =
            this.responseToNumber().toBigDecimal().divide(ETH_IN_WEI.toBigDecimal())

    private fun BigDecimal.convertFeeToEth(): BigDecimal {
        return this.divide(ETH_IN_WEI.toBigDecimal())
                .setScale(12, BigDecimal.ROUND_DOWN).stripTrailingZeros()
    }

}

data class EthereumInfoResponse(
        val balance: BigDecimal,
        val tokenBalance: BigDecimal?,
        val txCount: Long,
        val pendingTxCount: Long
)

private const val INFURA_API_KEY = "613a0b14833145968b1f656240c7d245"