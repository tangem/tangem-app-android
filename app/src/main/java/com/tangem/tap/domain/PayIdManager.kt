package com.tangem.tap.domain

import com.tangem.blockchain.common.Blockchain
import com.tangem.commands.common.network.Result
import com.tangem.tap.network.payid.PayIdService
import com.tangem.tap.network.payid.SetPayIdResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.util.*

class PayIdManager {
    private val payIdService = PayIdService()

    suspend fun getPayId(cardId: String, publicKey: String): Result<String?> = withContext(Dispatchers.IO) {
        val result = payIdService.getPayId(cardId, publicKey)
        when (result) {
            is Result.Success -> return@withContext Result.Success(result.data.payId)
            is Result.Failure -> {
                (result.error as? HttpException)?.let {
                    if (it.code() == 404) return@withContext Result.Success(null)
                }
                return@withContext result
            }
        }
    }


    suspend fun setPayId(
            cardId: String, publicKey: String, payId: String, address: String, blockchain: Blockchain
    ): Result<SetPayIdResponse> = withContext(Dispatchers.IO) {
        val result = payIdService.setPayId(cardId, publicKey, payId, address, blockchain.getPayIdNetwork())
        when (result) {
            is Result.Success -> return@withContext result
            is Result.Failure -> {
                (result.error as? HttpException)?.let {
                    if (it.code() == 409) return@withContext Result.Failure(TapError.PayIdAlreadyCreated)
                }
                return@withContext result
            }
        }
    }


    private fun Blockchain.getPayIdNetwork(): String {
        return when (this) {
            Blockchain.XRP -> "XRPL"
            Blockchain.RSK -> "RSK"
            else -> this.currency
        }
    }

    companion object {
        val payIdRegExp = "^[a-z0-9!#@%&*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#@%&*+/=?^_`{|}~-]+)*\\\$(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z-]*[a-z0-9])?|(?:[0-9]{1,3}\\.){3}[0-9]{1,3})\$".toRegex()

        val payIdSupported: EnumSet<Blockchain> = EnumSet.of(
                Blockchain.XRP,
                Blockchain.Ethereum,
                Blockchain.Bitcoin,
                Blockchain.Litecoin,
                Blockchain.Stellar,
                Blockchain.Cardano,
                Blockchain.Ducatus,
                Blockchain.BitcoinCash,
                Blockchain.Binance,
                Blockchain.RSK,
        )

        fun isPayId(value: String?): Boolean = value?.contains(payIdRegExp) ?: false
    }
}

fun Blockchain.isPayIdSupported(): Boolean {
    return PayIdManager.payIdSupported.contains(this)
}