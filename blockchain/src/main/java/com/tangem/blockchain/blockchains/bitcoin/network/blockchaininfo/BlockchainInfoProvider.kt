package com.tangem.blockchain.blockchains.bitcoin.network.blockchaininfo

import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinAddressResponse
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.retryIO
import com.tangem.common.extensions.hexToBytes
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope


class BlockchainInfoProvider(
        private val blockchainApi: BlockchainInfoApi,
        private val estimatefeeApi: EstimatefeeApi
) : BitcoinProvider {
    val decimals = Blockchain.Bitcoin.decimals()

    override suspend fun getInfo(address: String): Result<BitcoinAddressResponse> {
        return try {
            coroutineScope {
                val addressDeferred = retryIO { async { blockchainApi.getAddress(address) } }
                val unspentsDeferred = retryIO { async { blockchainApi.getUnspents(address) } }

                val addressData = addressDeferred.await()
                val unspents = unspentsDeferred.await()
                val unconfirmedTransactions = addressData.transactions?.find { it.blockHeight == 0L } != null

                val bitcoinUnspents = unspents.unspentOutputs.map {
                    BitcoinUnspentOutput(
                            it.amount!!.toBigDecimal().movePointLeft(decimals),
                            it.outputIndex!!.toLong(),
                            it.hash!!.hexToBytes(),
                            it.outputScript!!.hexToBytes())
                }

                Result.Success(
                        BitcoinAddressResponse(
                                addressData.finalBalance?.toBigDecimal()?.movePointLeft(decimals)
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