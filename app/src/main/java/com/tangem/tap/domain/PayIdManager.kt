package com.tangem.tap.domain

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.services.Result
import com.tangem.tap.network.payid.PayIdVerifyService
import com.tangem.tap.network.payid.VerifyPayIdResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class PayIdManager {

    @Suppress("MagicNumber")
    suspend fun verifyPayId(
        payId: String,
        blockchain: Blockchain,
    ): Result<VerifyPayIdResponse> = withContext(Dispatchers.IO) {
        val splitPayId = payId.split("\$")
        val user = splitPayId[0]
        val baseUrl = "https://${splitPayId[1]}/"
        return@withContext PayIdVerifyService(baseUrl).verifyAddress(user, blockchain.getPayIdNetwork())
    }

    private fun Blockchain.getPayIdNetwork(): String {
        return when (this) {
            Blockchain.XRP -> "XRPL"
            Blockchain.RSK -> "RSK"
            else -> this.currency
        }.lowercase(Locale.getDefault())
    }

    companion object {
        private val payIdRegExp = ("^[a-z0-9!#@%&*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#@%&*+/=?^_`{|}~-]+)*\\\$(?:(?:[a-z0-9]" +
            "(?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z-]*[a-z0-9])?|(?:[0-9]{1,3}\\.){3}[0-9]{1,3})\$").toRegex()

        val payIdSupported: EnumSet<Blockchain> = EnumSet.of(
            Blockchain.XRP,
            Blockchain.Ethereum,
            Blockchain.Bitcoin,
            Blockchain.Litecoin,
            Blockchain.Stellar,
            Blockchain.Cardano,
            Blockchain.CardanoShelley,
            Blockchain.Ducatus,
            Blockchain.BitcoinCash,
            Blockchain.Binance,
            Blockchain.RSK,
            Blockchain.Tezos,
        )

        fun isPayId(value: String?): Boolean = value?.contains(payIdRegExp) ?: false
    }
}

fun Blockchain.isPayIdSupported(): Boolean {
    return PayIdManager.payIdSupported.contains(this)
}
