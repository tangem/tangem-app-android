package com.tangem.blockchain.xrp.network

import com.tangem.blockchain.common.extensions.Result
import com.tangem.blockchain.common.extensions.SimpleResult
import com.tangem.blockchain.common.network.API_RIPPLED
import com.tangem.blockchain.common.network.API_RIPPLED_RESERVE
import com.tangem.blockchain.common.network.createRetrofitInstance
import com.tangem.blockchain.xrp.network.rippled.RippledApi
import com.tangem.blockchain.xrp.network.rippled.RippledProvider
import retrofit2.HttpException
import java.io.IOException
import java.math.BigDecimal

class XrpNetworkManager {
    private val rippledProvider by lazy {
        val api = createRetrofitInstance(API_RIPPLED)
                .create(RippledApi::class.java)
        RippledProvider(api)
    }

    private val rippledReserveProvider by lazy {
        val api = createRetrofitInstance(API_RIPPLED_RESERVE)
                .create(RippledApi::class.java)
        RippledProvider(api)
    }

    var provider = rippledProvider

    private fun changeProvider() {
        provider = if (provider == rippledProvider) rippledReserveProvider else rippledProvider
    }

    suspend fun getInfo(address: String): Result<XrpInfoResponse> {
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

    suspend fun getFee(): Result<XrpFeeResponse> {
        val result = provider.getFee()
        when (result) {
            is Result.Success -> return result
            is Result.Failure -> {
                if (result.error is IOException || result.error is HttpException) {
                    changeProvider()
                    return provider.getFee()
                } else {
                    return result
                }
            }
        }
    }
}

data class XrpInfoResponse(
        val balance: BigDecimal = BigDecimal.ZERO,
        val sequence: Long = 0,
        val hasUnconfirmed: Boolean = false,
        val reserveBase: BigDecimal,
        val accountFound: Boolean = true
)

data class XrpFeeResponse(
        val minimalFee: BigDecimal,
        val normalFee: BigDecimal,
        val priorityFee: BigDecimal
)