package com.tangem.blockchain.cardano.network.adalite

import com.tangem.blockchain.cardano.network.CardanoAddressResponse
import com.tangem.blockchain.cardano.UnspentOutput
import com.tangem.blockchain.cardano.network.api.AdaliteApi
import com.tangem.blockchain.common.extensions.Result
import com.tangem.blockchain.common.extensions.SimpleResult
import com.tangem.blockchain.common.extensions.retryIO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class AdaliteProvider(private val api: AdaliteApi) {

    suspend fun getInfo(address: String): Result<CardanoAddressResponse> {
        return try {
            coroutineScope {
                val addressDeferred = retryIO { async { api.getAddress(address) } }
                val unspentsDeferred = retryIO { async { api.getUnspents(address) } }

                val addressData = addressDeferred.await()
                val unspents = unspentsDeferred.await()

                val cardanoUnspents = unspents.data.map {
                    UnspentOutput(
                            it.amountData!!.amount!!,
                            it.outputIndex!!.toLong(),
                            it.hash!!.toByteArray()
                    )
                }

                Result.Success(
                        CardanoAddressResponse(
                                addressData.data!!.balanceData!!.amount!!,
                                cardanoUnspents
                        )
                )
            }
        } catch (exception: Exception) {
            Result.Failure(exception)
        }
    }

    suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            retryIO { api.sendTransaction(AdaliteSendBody(transaction)) }
            SimpleResult.Success
        } catch (exception: Exception) {
            SimpleResult.Failure(exception)
        }
    }
}

data class AdaliteSendBody(val signedTransaction: String)