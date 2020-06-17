package com.tangem.blockchain.network.blockcypher

import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinAddressResponse
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinProvider
import com.tangem.blockchain.network.blockcypher.BlockcypherApi
import com.tangem.blockchain.network.blockcypher.BlockcypherBody
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.retryIO
import com.tangem.common.extensions.hexToBytes

class BlockcypherProvider(private val api: BlockcypherApi, blockchain: Blockchain) : BitcoinProvider {

    private val blockchainPath = when (blockchain) {
        Blockchain.Bitcoin, Blockchain.BitcoinTestnet -> "btc"
        Blockchain.Litecoin -> "ltc"
        else -> throw Exception("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
    }

    private val network = when (blockchain) {
        Blockchain.BitcoinTestnet -> "test3"
        else -> "main"
    }

    private val decimals = blockchain.decimals()

    override suspend fun getInfo(address: String): Result<BitcoinAddressResponse> {
        try {
            val addressData: BlockcypherResponse = retryIO { api.getAddressData(blockchainPath, network, address) }
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
            val receivedFee: BlockcypherFee = retryIO { api.getFee(blockchainPath, network) }
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
                        blockchainPath, network, BlockcypherBody(transaction), BlockcypherToken.getToken())
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