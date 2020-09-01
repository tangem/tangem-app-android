package com.tangem.tap.domain

import com.tangem.blockchain.common.Blockchain
import com.tangem.commands.common.network.Result
import com.tangem.tap.network.payid.PayIdService
import com.tangem.tap.network.payid.SetPayIdResponse
import retrofit2.HttpException
import java.util.*

class PayIdManager {
    private val payIdService = PayIdService()

    suspend fun getPayId(cardId: String, publicKey: String): Result<String?> {
        val result = payIdService.getPayId(cardId, publicKey)
        when (result) {
            is Result.Success -> return Result.Success(result.data.payId)
            is Result.Failure -> {
                (result.error as? HttpException)?.let {
                    if (it.code() == 404) return Result.Success(null)
                }
                return result
            }
        }
    }

    suspend fun setPayId(
            cardId: String, publicKey: String, payId: String, address: String, blockchain: Blockchain
    ): Result<SetPayIdResponse> {
        val result = payIdService.setPayId(cardId, publicKey, payId, address, blockchain.getPayIdNetwork())
        when (result) {
            is Result.Success -> return result
            is Result.Failure -> {
                (result.error as? HttpException)?.let {
                    if (it.code() == 409) return Result.Failure(TapError.PayIdAlreadyCreated)
                }
                return result
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
    }
}

fun Blockchain.isPayIdSupported(): Boolean {
    return PayIdManager.payIdSupported.contains(this)
}