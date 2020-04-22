package com.tangem.blockchain.blockchains.cardano.network.adalite

import com.squareup.moshi.Json
import com.tangem.blockchain.blockchains.cardano.UnspentOutput
import com.tangem.blockchain.blockchains.cardano.network.CardanoAddressResponse
import com.tangem.blockchain.blockchains.cardano.network.api.AdaliteApi
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.retryIO
import com.tangem.common.extensions.hexToBytes
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class AdaliteProvider(private val api: AdaliteApi) {

    suspend fun getInfo(address: String): Result<CardanoAddressResponse> {
        return try {
            coroutineScope {
                val addressDeferred = retryIO { async { api.getAddress(address) } }
                val unspentsDeferred = retryIO { async { api.getUnspents(listOf(address)) } }

                val addressData = addressDeferred.await()
                val unspents = unspentsDeferred.await()

                val cardanoUnspents = unspents.data.map {
                    UnspentOutput(
                            it.amountData!!.amount!!,
                            it.outputIndex!!.toLong(),
                            it.hash!!.hexToBytes()
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
            val response = retryIO { api.sendTransaction(AdaliteSendBody(transaction)) }
            SimpleResult.Success
        } catch (exception: Exception) {
            SimpleResult.Failure(exception)
        }
    }
}

data class AdaliteSendBody(
        @Json(name = "signedTx")
        val signedTransaction: String
)