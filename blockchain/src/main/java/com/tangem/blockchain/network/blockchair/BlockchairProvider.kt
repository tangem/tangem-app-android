package com.tangem.blockchain.network.blockchair

import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinAddressResponse
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinFee
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.retryIO
import com.tangem.common.extensions.hexToBytes
import java.math.BigDecimal
import java.math.RoundingMode

class BlockchairProvider(private val api: BlockchairApi, blockchain: Blockchain) : BitcoinProvider {
    private val blockchainPath = when (blockchain) {
        Blockchain.BitcoinCash -> "bitcoin-cash"
        Blockchain.Litecoin -> "litecoin"
        else -> throw Exception("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
    }
    private val decimals = blockchain.decimals()

    override suspend fun getInfo(address: String): Result<BitcoinAddressResponse> {
        return try {
            val blockchairAddress = retryIO { api.getAddressData(address, blockchainPath, API_KEY) }

            val addressData = blockchairAddress.data!!.getValue(address)
            val addressInfo = addressData.addressInfo!!
            val script = addressInfo.script!!.hexToBytes()

            val hasUnconfirmed = checkHasUnconfirmed(addressData)

            val unspentTransactions = addressData.unspentOutputs!!.map {
                BitcoinUnspentOutput(
                        amount = it.amount!!.toBigDecimal().movePointLeft(decimals),
                        outputIndex = it.index!!.toLong(),
                        transactionHash = it.transactionHash!!.hexToBytes(),
                        outputScript = script
                )
            }

            Result.Success(BitcoinAddressResponse(
                    balance = addressInfo.balance!!.toBigDecimal().movePointLeft(decimals),
                    hasUnconfirmed = hasUnconfirmed,
                    unspentOutputs = unspentTransactions
            ))
        } catch (error: Exception) {
            Result.Failure(error)
        }

    }

    private suspend fun checkHasUnconfirmed(addressData: BlockchairAddressData): Boolean {
        for (utxo in addressData.unspentOutputs!!) { //check utxos first
            if (utxo.block == -1) return true
        }

        return if (addressData.addressInfo!!.balance != 0L) { // if balance is not zero, unconfirmed tx should have unconfirmed utxo
            false
        } else {
            if (addressData.transactions!!.isEmpty()) { // no transactions from this address ever
                false
            } else { // check last transaction in case it spent all funds
                val lastTransactionHash = addressData.transactions[0]
                val blockchairTransaction = retryIO {
                    api.getTransaction(lastTransactionHash, blockchainPath, API_KEY)
                }
                blockchairTransaction.data!!.getValue(lastTransactionHash).transaction!!.block == -1
            }
        }
    }

    override suspend fun getFee(): Result<BitcoinFee> {
        return try {
            val stats = retryIO { api.getBlockchainStats(blockchainPath, API_KEY) }
            val feePerKb = (stats.data!!.feePerByte!! * 1024).toBigDecimal().movePointLeft(decimals)
            Result.Success(BitcoinFee(
                    minimalPerKb = (feePerKb * BigDecimal.valueOf(0.8)).setScale(decimals, RoundingMode.DOWN),
                    normalPerKb = feePerKb.setScale(decimals, RoundingMode.DOWN),
                    priorityPerKb = (feePerKb * BigDecimal.valueOf(1.2)).setScale(decimals, RoundingMode.DOWN)
            ))
        } catch (error: Exception) {
            Result.Failure(error)
        }
    }

    override suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            retryIO { api.sendTransaction(BlockchairBody(transaction), blockchainPath, API_KEY) }
            SimpleResult.Success
        } catch (error: Exception) {
            SimpleResult.Failure(error)
        }
    }
}

private const val API_KEY = "A___0Shpsu4KagE7oSabrw20DfXAqWlT"