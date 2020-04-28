package com.tangem.blockchain.blockchains.bitcoin.network

import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.blockchains.bitcoin.network.api.BlockcypherApi
import com.tangem.blockchain.blockchains.bitcoin.network.api.BlockcypherBody
import com.tangem.blockchain.blockchains.bitcoin.network.response.BlockcypherFee
import com.tangem.blockchain.blockchains.bitcoin.network.response.BlockcypherResponse
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.retryIO
import com.tangem.common.extensions.hexToBytes

class BlockcypherProvider(private val api: BlockcypherApi, isTestNet: Boolean) : BitcoinProvider {

    private val blockchain = "btc"
    private val decimals = Blockchain.Bitcoin.decimals()

    private val network = if (isTestNet) {
        BlockcypherNetwork.Test.network
    } else {
        BlockcypherNetwork.Main.network
    }

    override suspend fun getInfo(address: String): Result<BitcoinAddressResponse> {
        try {
            val addressData: BlockcypherResponse = retryIO { api.getAddressData(blockchain, network, address) }
            val unspents = addressData.txrefs?.map {
                BitcoinUnspentOutput(
                        it.amount!!.toBigDecimal().movePointLeft(decimals),
                        it.outputIndex!!.toLong(),
                        it.hash!!.hexToBytes(),
                        it.outputScript!!.hexToBytes()
                )
            }
            return Result.Success(BitcoinAddressResponse(
                    addressData.balance!!.toBigDecimal().movePointLeft(decimals),
                    addressData.unconfirmedBalance != 0L,
                    unspents))

        } catch (error: Exception) {
            return Result.Failure(error)
        }
    }

    override suspend fun getFee(): Result<BitcoinFee> {
        return try {
            val receivedFee: BlockcypherFee = retryIO { api.getFee(blockchain, network) }
            Result.Success(
                    BitcoinFee(receivedFee.minFeePerKb!!.toBigDecimal().movePointLeft(decimals),
                            receivedFee.normalFeePerKb!!.toBigDecimal().movePointLeft(decimals),
                            receivedFee.priorityFeePerKb!!.toBigDecimal().movePointLeft(decimals))
            )
        } catch (error: Exception) {
            Result.Failure(error)
        }
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            retryIO {
                api.sendTransaction(
                        blockchain, network, BlockcypherBody(transaction), BlockcypherToken.getToken())
            }
            SimpleResult.Success
        } catch (error: Exception) {
            SimpleResult.Failure(error)
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