package com.tangem.domain.walletconnect.model.pay

data class WcPayAmount(
    val value: String,
    val unit: String,
    val display: Display?,
) {
    data class Display(
        val assetSymbol: String,
        val assetName: String,
        val decimals: Int,
        val iconUrl: String?,
        val networkName: String?,
        val networkIconUrl: String?,
    )
}