package com.tangem.domain.polymarket.model

enum class PolymarketChainFamily {
    EVM,
    SVM,
    BTC,
    TRON,
    ;

    companion object {

        private val EVM_CHAIN_IDS = listOf("1", "10", "56", "137", "143", "999", "4663", "8453", "42161", "57073")

        private const val SOLANA_CHAIN_ID = "1151111081099710"
        private const val TRON_CHAIN_ID = "728126428"
        private const val BITCOIN_CHAIN_ID = "8253038"

        private val BY_CHAIN_ID: Map<String, PolymarketChainFamily> = buildMap {
            EVM_CHAIN_IDS.forEach { put(it, EVM) }
            put(SOLANA_CHAIN_ID, SVM)
            put(TRON_CHAIN_ID, TRON)
            put(BITCOIN_CHAIN_ID, BTC)
        }

        /**
         * @return the family a deposit on [chainId] must be sent to, or `null` for any chain this build does
         *  not recognise — a deposit to the wrong family cannot be recovered
         */
        fun of(chainId: String): PolymarketChainFamily? = BY_CHAIN_ID[chainId]
    }
}