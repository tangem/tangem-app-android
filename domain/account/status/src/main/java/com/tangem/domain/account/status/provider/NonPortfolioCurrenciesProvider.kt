package com.tangem.domain.account.status.provider

import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

interface NonPortfolioCurrenciesProvider {

    fun observe(userWalletId: UserWalletId, currency: CryptoCurrency): Flow<Pair<Account, CryptoCurrencyStatus>>

    suspend fun getStatuses(userWalletId: UserWalletId, network: Network): List<CryptoCurrencyStatus>

    suspend fun findCurrency(userWalletId: UserWalletId, currencyId: CryptoCurrency.ID): CryptoCurrency?
}