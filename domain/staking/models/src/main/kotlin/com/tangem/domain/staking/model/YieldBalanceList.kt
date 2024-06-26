package com.tangem.domain.staking.model

sealed class YieldBalanceList {

    data class Data(
        val balances: List<YieldBalance>,
    ) : YieldBalanceList() {
        fun getBalance(rawCurrencyId: String?): YieldBalance? {
            return balances.firstOrNull { yield ->
                (yield as? YieldBalance.Data)?.balance?.items
                    ?.any { it.rawCurrencyId == rawCurrencyId } == true
            }
        }
    }

    data object Empty : YieldBalanceList()
}
