package com.tangem.domain.moneyaccount.yield

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.yield.supply.YieldSupplyStatus

interface MoneyAccountYieldModeStatusProvider {

    suspend fun get(
        userWalletId: UserWalletId,
        token: CryptoCurrency.Token,
        networkStatus: NetworkStatus.Verified,
    ): YieldSupplyStatus?
}