package com.tangem.domain.tokens

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWalletId

class IsCryptoCurrencyCoinCouldHideUseCase(
    private val currenciesRepository: CurrenciesRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, cryptoCurrencyCoin: CryptoCurrency.Coin): Boolean {
        return currenciesRepository.getMultiCurrencyWalletCurrenciesSync(
            userWalletId = userWalletId,
            refresh = false,
        ).none { it is CryptoCurrency.Token && it.network == cryptoCurrencyCoin.network }
    }
}