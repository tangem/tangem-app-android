package com.tangem.blockchain.blockchains.litecoin

import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinAddressResponse
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinProvider
import com.tangem.blockchain.network.blockcypher.BlockcypherProvider
import com.tangem.blockchain.network.blockcypher.BlockcypherApi
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.API_BLOCKCHAIR
import com.tangem.blockchain.network.API_BLOCKCYPHER
import com.tangem.blockchain.network.blockchair.BlockchairApi
import com.tangem.blockchain.network.blockchair.BlockchairProvider
import com.tangem.blockchain.network.createRetrofitInstance
import retrofit2.HttpException
import java.io.IOException


class LitecoinNetworkManager : BitcoinProvider {
    private val blockchain = Blockchain.Litecoin

    private val blockchairProvider by lazy {
        val api = createRetrofitInstance(API_BLOCKCHAIR)
                .create(BlockchairApi::class.java)
        BlockchairProvider(api, blockchain)
    }

    private val blockcypherProvider by lazy {
        val api = createRetrofitInstance(API_BLOCKCYPHER)
                .create(BlockcypherApi::class.java)
        BlockcypherProvider(api, blockchain)
    }

    private var provider: BitcoinProvider = blockchairProvider

    private fun changeProvider() {
        provider = if (provider == blockchairProvider) blockcypherProvider else blockchairProvider
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