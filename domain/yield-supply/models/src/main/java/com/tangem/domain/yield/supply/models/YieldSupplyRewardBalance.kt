package com.tangem.domain.yield.supply.models

data class YieldSupplyRewardBalance(
    val fiatBalance: String?,
    val cryptoBalance: String?,
) {
    companion object {
        fun empty() = YieldSupplyRewardBalance(null, null)
    }
}