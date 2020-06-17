package com.tangem.blockchain.blockchains.bitcoin.network

import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.blockchains.bitcoin.network.blockchaininfo.BlockchainInfoApi
import com.tangem.blockchain.blockchains.bitcoin.network.blockchaininfo.BlockchainInfoProvider
import com.tangem.blockchain.network.blockcypher.BlockcypherApi
import com.tangem.blockchain.blockchains.bitcoin.network.blockchaininfo.EstimatefeeApi
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.API_BLOCKCHAIN_INFO
import com.tangem.blockchain.network.API_BLOCKCYPHER
import com.tangem.blockchain.network.API_ESTIMATEFEE
import com.tangem.blockchain.network.blockcypher.BlockcypherProvider
import com.tangem.blockchain.network.createRetrofitInstance
import retrofit2.HttpException
import java.io.IOException
import java.math.BigDecimal


class BitcoinNetworkManager(blockchain: Blockchain) : BitcoinProvider {

    private val blockcypherProvider by lazy {
        val api = createRetrofitInstance(API_BLOCKCYPHER)
                .create(BlockcypherApi::class.java)
        BlockcypherProvider(api, blockchain)
    }

    private val blockchainInfoProvider by lazy {
        val api = createRetrofitInstance(API_BLOCKCHAIN_INFO)
                .create(BlockchainInfoApi::class.java)
        val estimateFeeApi = createRetrofitInstance(API_ESTIMATEFEE)
                .create(EstimatefeeApi::class.java)
        BlockchainInfoProvider(api, estimateFeeApi)
    }

    private var provider: BitcoinProvider = blockchainInfoProvider

    private fun changeProvider() {
        provider = if (provider == blockchainInfoProvider) {
            blockcypherProvider
        } else {
            blockchainInfoProvider
        }
    }

    override suspend fun getInfo(address: String): Result<BitcoinAddressResponse> {
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

    override suspend fun getFee(): Result<BitcoinFee> {
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

    override suspend fun sendTransaction(transaction: String): SimpleResult {
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

data class BitcoinAddressResponse(
        val balance: BigDecimal,
        val hasUnconfirmed: Boolean,
        val unspentOutputs: List<BitcoinUnspentOutput>?
)

data class BitcoinFee(
        val minimalPerKb: BigDecimal,
        val normalPerKb: BigDecimal,
        val priorityPerKb: BigDecimal
)