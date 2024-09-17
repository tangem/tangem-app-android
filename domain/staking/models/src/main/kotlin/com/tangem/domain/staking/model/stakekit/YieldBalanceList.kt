package com.tangem.domain.staking.model.stakekit

sealed class YieldBalanceList {

    data class Data(
        val balances: List<YieldBalance>,
    ) : YieldBalanceList() {

        fun getBalance(address: String?, rawCurrencyId: String?): YieldBalance {
            return balances.firstOrNull { yieldBalance ->
                val data = yieldBalance as? YieldBalance.Data
                data?.balance?.items?.any {
                    address == data.address && rawCurrencyId == it.rawCurrencyId
                } == true
            } ?: YieldBalance.Error
        }
    }

    data object Empty : YieldBalanceList()

    data object Error : YieldBalanceList()
}