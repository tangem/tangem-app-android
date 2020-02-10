package com.tangem.blockchain.cardano.network

import com.tangem.blockchain.cardano.UnspentOutput
import com.tangem.blockchain.cardano.network.adalite.AdaliteProvider
import com.tangem.blockchain.cardano.network.api.AdaliteApi
import com.tangem.blockchain.common.extensions.Result
import com.tangem.blockchain.common.extensions.SimpleResult
import com.tangem.blockchain.common.network.API_ADALITE
import com.tangem.blockchain.common.network.API_ADALITE_RESERVE
import com.tangem.blockchain.common.network.createRetrofitInstance
import retrofit2.HttpException
import java.io.IOException

class CardanoNetworkManager {
    private val adaliteProvider by lazy {
        val api = createRetrofitInstance(API_ADALITE)
                .create(AdaliteApi::class.java)
        AdaliteProvider(api)
    }

    private val adaliteReserveProvider by lazy {
        val api = createRetrofitInstance(API_ADALITE_RESERVE)
                .create(AdaliteApi::class.java)
        AdaliteProvider(api)
    }

    private var provider = adaliteProvider

    private fun changeProvider() {
        provider = if (provider == adaliteProvider) adaliteReserveProvider else adaliteProvider
    }

    suspend fun getInfo(address: String): Result<CardanoAddressResponse> {
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
}

data class CardanoAddressResponse(
        val balance: Long,
        val unspentOutputs: List<UnspentOutput>
)