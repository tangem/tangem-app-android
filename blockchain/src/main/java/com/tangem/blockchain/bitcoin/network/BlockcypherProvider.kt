package com.tangem.blockchain.bitcoin.network

import com.tangem.blockchain.bitcoin.UnspentTransaction
import com.tangem.blockchain.bitcoin.network.api.BlockcypherApi
import com.tangem.blockchain.bitcoin.network.response.BlockcypherBody
import com.tangem.blockchain.bitcoin.network.response.BlockcypherFee
import com.tangem.blockchain.bitcoin.network.response.BlockcypherResponse
import com.tangem.blockchain.common.extensions.Result
import com.tangem.blockchain.common.extensions.SimpleResult
import com.tangem.blockchain.common.extensions.retryIO

class BlockcypherProvider(private val api: BlockcypherApi, isTestNet: Boolean) : BitcoinProvider {

    private val blockchain = "btc"

    private val network = if (isTestNet) {
        BlockcypherNetwork.Test.network
    } else {
        BlockcypherNetwork.Main.network
    }

    override suspend fun getInfo(address: String): Result<BitcoinAddressResponse> {
        try {
            val addressData: BlockcypherResponse = retryIO { api.getAddressData(blockchain, network, address) }
            val unspents = addressData.txrefs!!.map {
                UnspentTransaction(
                        it.amount!!,
                        it.outputIndex!!.toLong(),
                        it.hash!!.toByteArray(),
                        it.outputScript!!.toByteArray()
                )
            }
            return Result.Success(BitcoinAddressResponse(
                    addressData.balance!!,
                    addressData.unconfirmedBalance != 0L,
                    unspents))

        } catch (error: Exception) {
            return Result.Failure(error)
        }
    }

    override suspend fun getFee(): Result<BitcoinFee> {
        try {
            val receivedFee: BlockcypherFee = retryIO { api.getFee(blockchain, network) }
            return Result.Success(
                    BitcoinFee(receivedFee.minFeePerKb!!.toBigDecimal() / satoshiInBtc,
                            receivedFee.normalFeePerKb!!.toBigDecimal() / satoshiInBtc,
                            receivedFee.priorityFeePerKb!!.toBigDecimal() / satoshiInBtc)
            )
        } catch (error: Exception) {
            return Result.Failure(error)
        }
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        try {
            retryIO {
                api.sendTransaction(
                        blockchain, network, BlockcypherBody(transaction), BlockcypherToken.getToken())
            }
            return SimpleResult.Success
        } catch (error: Exception) {
            return SimpleResult.Failure(error)
        }
    }
}

private object BlockcypherToken {
    private val tokens = listOf(
            "aa8184b0e0894b88a5688e01b3dc1e82",
            "56c4ca23c6484c8f8864c32fde4def8d",
            "66a8a37c5e9d4d2c9bb191acfe7f93aa")

    fun getToken(): String = tokens.random()
}

private enum class BlockcypherNetwork(val network: String) {
    Main("main"),
    Test("test3")
}

val satoshiInBtc = 100000000.toBigDecimal()
