package com.tangem.tap.domain.walletconnect2.domain.models.binance

data class WcBinanceTradePair(val from: String, val to: String) {
    companion object {
        fun from(symbol: String): WcBinanceTradePair? {
            val pair = symbol.split("_")

            return if (pair.size > 1) {
                val firstParts = pair[0].split("-")
                val secondParts = pair[1].split("-")
                WcBinanceTradePair(firstParts[0], secondParts[0])
            } else {
                null
            }
        }
    }
}