package com.tangem.blockchain.bitcoin.network

import com.tangem.blockchain.bitcoin.UnspentTransaction
import com.tangem.blockchain.bitcoin.network.api.BlockchainInfoApi
import com.tangem.blockchain.bitcoin.network.api.BlockcypherApi
import com.tangem.blockchain.bitcoin.network.api.EstimatefeeApi
import com.tangem.blockchain.common.extensions.Result
import com.tangem.blockchain.common.extensions.SimpleResult
import com.tangem.blockchain.common.network.API_BLOCKCHAIN_INFO
import com.tangem.blockchain.common.network.API_BLOCKCYPHER
import com.tangem.blockchain.common.network.API_ESTIMATEFEE
import com.tangem.blockchain.common.network.createRetrofitInstance
import retrofit2.HttpException
import java.io.IOException
import java.math.BigDecimal


class BitcoinNetworkManager(private val isTestNet: Boolean) : BitcoinProvider {

    private val blockcypherProvider by lazy {
        val api = createRetrofitInstance(API_BLOCKCYPHER)
                .create(BlockcypherApi::class.java)
        BlockcypherProvider(api, isTestNet)
    }

    private val blockchainInfoProvider by lazy {
        val api = createRetrofitInstance(API_BLOCKCHAIN_INFO)
                .create(BlockchainInfoApi::class.java)
        val estimateFeeApi = createRetrofitInstance(API_ESTIMATEFEE)
                .create(EstimatefeeApi::class.java)
        BlockchainInfoProvider(api, estimateFeeApi)
    }

    private var bitcoinProvider: BitcoinProvider = blockchainInfoProvider

    private fun changeProvider() {
        bitcoinProvider = if (bitcoinProvider == blockchainInfoProvider) {
            blockcypherProvider
        } else {
            blockchainInfoProvider
        }
    }

    override suspend fun getInfo(address: String): Result<BitcoinAddressResponse> {
        val result = bitcoinProvider.getInfo(address)
        when (result) {
            is Result.Success -> return result
            is Result.Failure -> {
                if (result.error is IOException || result.error is HttpException) {
                    changeProvider()
                    return bitcoinProvider.getInfo(address)
                } else {
                    return result
                }
            }
        }
    }

    override suspend fun getFee(): Result<BitcoinFee> {
        val result = bitcoinProvider.getFee()
        when (result) {
            is Result.Success -> return result
            is Result.Failure -> {
                if (result.error is IOException || result.error is HttpException) {
                    changeProvider()
                    return bitcoinProvider.getFee()
                } else {
                    return result
                }
            }
        }
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        val result = bitcoinProvider.sendTransaction(transaction)
        when (result) {
            is SimpleResult.Success -> return result
            is SimpleResult.Failure -> {
                if (result.error is IOException || result.error is HttpException) {
                    changeProvider()
                    return bitcoinProvider.sendTransaction(transaction)
                } else {
                    return result
                }
            }
        }
    }

    companion object {
        val SATOSHI_IN_BTC = 100000000.toBigDecimal()
    }
}

data class BitcoinAddressResponse(
        val balance: BigDecimal,
        val hasUnconfirmed: Boolean,
        val unspentTransactions: List<UnspentTransaction>?
)

data class BitcoinFee(
        val minimalPerKb: BigDecimal,
        val normalPerKb: BigDecimal,
        val priorityPerKb: BigDecimal
)