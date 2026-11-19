package com.tangem.data.moneyaccount.currency

import com.tangem.domain.account.status.provider.NonPortfolioCurrenciesFetcher
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusSupplier
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

internal class MoneyAccountCurrenciesFetcher @Inject constructor(
    private val moneyAccountStatusSupplier: MoneyAccountStatusSupplier,
    private val moneyAccountStatusFetcher: MoneyAccountStatusFetcher,
) : NonPortfolioCurrenciesFetcher {

    override suspend fun fetch(userWalletId: UserWalletId, network: Network) {
        val account = moneyAccountStatusSupplier(userWalletId).firstOrNull()?.value as? MoneyAccountStatusValue.Account
        if (account?.cryptoCurrency?.network?.id != network.id) return

        moneyAccountStatusFetcher(MoneyAccountStatusFetcher.Params(userWalletId))
            .onLeft { TangemLogger.e("Unable to refresh the Money account status", it) }
    }
}