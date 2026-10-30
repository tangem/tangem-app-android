package com.tangem.domain.polymarket.model

/** What a CLOB balance-allowance call is about: the collateral, or the shares of one outcome token. */
sealed interface PolymarketClobAsset {

    data object Collateral : PolymarketClobAsset

    /**
     * @property tokenId the outcome token as the order names it
     * @property version the market's protocol; the CLOB keys V2 shares under their own asset type
     */
    data class Shares(val tokenId: String, val version: PolymarketMarketVersion) : PolymarketClobAsset
}