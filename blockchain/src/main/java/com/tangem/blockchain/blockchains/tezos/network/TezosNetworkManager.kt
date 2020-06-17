package com.tangem.blockchain.blockchains.tezos.network

import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.API_TEZOS
import com.tangem.blockchain.network.API_TEZOS_RESERVE
import com.tangem.blockchain.network.createRetrofitInstance
import retrofit2.HttpException
import java.io.IOException
import java.math.BigDecimal

class TezosNetworkManager {
    private val tezosProvider by lazy {
        val api = createRetrofitInstance(API_TEZOS)
                .create(TezosApi::class.java)
        TezosProvider(api)
    }

    private val tezosReserveProvider by lazy {
        val api = createRetrofitInstance(API_TEZOS_RESERVE)
                .create(TezosApi::class.java)
        TezosProvider(api)
    }

    var provider = tezosProvider

    private fun changeProvider() {
        provider = if (provider == tezosProvider) tezosReserveProvider else tezosProvider
    }

    suspend fun getInfo(address: String): Result<TezosInfoResponse> {
        val result = provider.getInfo(address)
        when (result) {
            is Result.Success -> return result
            is Result.Failure -> {
                if (result.error is IOException || result.error is HttpException) {
                    changeProvider()
                    return provider.getInfo(address)
                } else {
                    return result
                }
            }
        }
    }

    suspend fun isPublicKeyRevealed(address: String): Result<Boolean> {
        val result = provider.isPublicKeyRevealed(address)
        when (result) {
            is Result.Success -> return result
            is Result.Failure -> {
                if (result.error is IOException || result.error is HttpException) {
                    changeProvider()
                    return provider.isPublicKeyRevealed(address)
                } else {
                    return result
                }
            }
        }
    }

    suspend fun getHeader(): Result<TezosHeader> {
        val result = provider.getHeader()
        when (result) {
            is Result.Success -> return result
            is Result.Failure -> {
                if (result.error is IOException || result.error is HttpException) {
                    changeProvider()
                    return provider.getHeader()
                } else {
                    return result
                }
            }
        }
    }

    suspend fun forgeContents(headerHash: String, contents: List<TezosOperationContent>): Result<String> {
        val result = provider.forgeContents(headerHash, contents)
        when (result) {
            is Result.Success -> return result
            is Result.Failure -> {
                if (result.error is IOException || result.error is HttpException) {
                    changeProvider()
                    return provider.forgeContents(headerHash, contents)
                } else {
                    return result
                }
            }
        }
    }

    suspend fun checkTransaction(
            header: TezosHeader,
            contents: List<TezosOperationContent>,
            signature: ByteArray
    ): SimpleResult {
        val result = provider.checkTransaction(header, contents, signature)
        when (result) {
            is SimpleResult.Success -> return result
            is SimpleResult.Failure -> {
                if (result.error is IOException || result.error is HttpException) {
                    changeProvider()
                    return provider.checkTransaction(header, contents, signature)
                } else {
                    return result
                }
            }
        }
    }

    suspend fun sendTransaction(transaction: String): SimpleResult {
        val result = provider.sendTransaction(transaction)
        when (result) {
            is SimpleResult.Success -> return result
            is SimpleResult.Failure -> {
                if (result.error is IOException || result.error is HttpException) {
                    changeProvider()
                    return provider.sendTransaction(transaction)
                } else {
                    return result
                }
            }
        }
    }
}

data class TezosInfoResponse(
        val balance: BigDecimal,
        val counter: Long
)

data class TezosHeader(
        val hash: String,
        val protocol: String
)