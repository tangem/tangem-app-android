package com.tangem.data.yield.supply.status

import com.tangem.data.yield.supply.status.store.YieldModuleStatusStore
import com.tangem.domain.common.wallets.CryptoCurrencyDataCleaner
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.models.status.YieldModuleStatus
import javax.inject.Inject

internal class YieldModuleStatusDataCleaner @Inject constructor(
    private val store: YieldModuleStatusStore,
) : CryptoCurrencyDataCleaner {

    override suspend fun invoke(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        val keys = currencies
            .filterIsInstance<CryptoCurrency.Token>()
            .mapTo(hashSetOf()) { YieldModuleStatus.Key(it) }

        store.clear(userWalletId = userWalletId, keys = keys)
    }

    override suspend fun clear(userWalletIds: List<UserWalletId>) {
        store.remove(userWalletIds = userWalletIds)
    }
}