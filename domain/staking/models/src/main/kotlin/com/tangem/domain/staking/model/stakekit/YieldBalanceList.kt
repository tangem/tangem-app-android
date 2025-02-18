package com.tangem.domain.staking.model.stakekit

sealed class YieldBalanceList {

    data class Data(val balances: List<YieldBalance>) : YieldBalanceList() {

        fun getBalance(address: String?, integrationId: String?): YieldBalance {
            return balances.firstOrNull { yieldBalance ->
                val data = yieldBalance as? YieldBalance.Data
                val balance = data?.balance

                val isCorrectAddress = address != null && address == data?.address
                val isCorrectIntegration = integrationId != null && balance?.integrationId == integrationId

                isCorrectIntegration && isCorrectAddress
            } ?: YieldBalance.Error(integrationId = integrationId, address = address)
        }
    }

    data object Empty : YieldBalanceList()

    data object Error : YieldBalanceList()
}