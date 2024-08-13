package com.tangem.domain.staking.model.stakekit

sealed class YieldBalanceList {

    data class Data(
        val balances: List<YieldBalance>,
    ) : YieldBalanceList() {
        fun getBalance(rawCurrencyId: String?, networkName: String): YieldBalance {
            return balances.firstOrNull { yield ->
                (yield as? YieldBalance.Data)?.balance?.items
                    ?.any {
                        rawCurrencyId == it.rawCurrencyId &&
                            networkName.equals(it.rawNetworkId, ignoreCase = true)
                    } == true
            } ?: YieldBalance.Error
        }
    }

    data object Empty : YieldBalanceList()

    data object Error : YieldBalanceList()
}