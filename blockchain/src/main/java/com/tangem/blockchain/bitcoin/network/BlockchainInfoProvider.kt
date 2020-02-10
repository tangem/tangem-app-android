package com.tangem.blockchain.bitcoin.network

import com.tangem.blockchain.bitcoin.UnspentTransaction
import com.tangem.blockchain.bitcoin.network.BitcoinNetworkManager.Companion.SATOSHI_IN_BTC
import com.tangem.blockchain.bitcoin.network.api.BlockchainInfoApi
import com.tangem.blockchain.bitcoin.network.api.EstimatefeeApi
import com.tangem.blockchain.common.extensions.Result
import com.tangem.blockchain.common.extensions.SimpleResult
import com.tangem.blockchain.common.extensions.retryIO
import com.tangem.common.extensions.hexToBytes
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope


class BlockchainInfoProvider(
        private val blockchainApi: BlockchainInfoApi,
        private val estimatefeeApi: EstimatefeeApi
) : BitcoinProvider {

    override suspend fun getInfo(address: String): Result<BitcoinAddressResponse> {
        return try {
            coroutineScope {
                val addressDeferred = retryIO { async { blockchainApi.getAddress(address) } }
                val unspentsDeferred = retryIO { async { blockchainApi.getUnspents(address) } }

                val addressData = addressDeferred.await()
                val unspents = unspentsDeferred.await()
                val unconfirmedTransactions = addressData.transactions?.find { it.blockHeight == 0L } != null

                val bitcoinUnspents = unspents.unspentOutputs.map {
                    UnspentTransaction(
                            it.amount!!.toBigDecimal().divide(SATOSHI_IN_BTC),
                            it.outputIndex!!.toLong(),
                            it.hash!!.hexToBytes(),
                            it.outputScript!!.hexToBytes())
                }

                Result.Success(
                        BitcoinAddressResponse(
                                addressData.finalBalance?.toBigDecimal()?.divide(SATOSHI_IN_BTC)
                                        ?: 0.toBigDecimal(), unconfirmedTransactions, bitcoinUnspents))
            }
        } catch (exception: Exception) {
            Result.Failure(exception)
        }
    }

    override suspend fun getFee(): Result<BitcoinFee> {
        return try {
            coroutineScope {
                val minFeeDeferred = retryIO { async { estimatefeeApi.getEstimateFeeMinimal() } }
                val normalFeeDeferred = retryIO { async { estimatefeeApi.getEstimateFeeNormal() } }
                val priorityFeeDeferred = retryIO { async { estimatefeeApi.getEstimateFeePriority() } }

                val minFee = minFeeDeferred.await()
                val normalFee = normalFeeDeferred.await()
                val priorityFee = priorityFeeDeferred.await()

                Result.Success(BitcoinFee(
                        minFee.toBigDecimal(),
                        normalFee.toBigDecimal(),
                        priorityFee.toBigDecimal()))
            }
        } catch (exception: Exception) {
            Result.Failure(exception)
        }
    }


    override suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            retryIO { blockchainApi.sendTransaction(transaction) }
            SimpleResult.Success
        } catch (exception: Exception) {
            SimpleResult.Failure(exception)
        }
    }


}