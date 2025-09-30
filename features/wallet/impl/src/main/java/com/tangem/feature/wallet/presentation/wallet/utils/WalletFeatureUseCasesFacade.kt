package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.domain.models.PortfolioId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.IsCryptoCurrencyCoinCouldHideUseCase
import com.tangem.domain.tokens.RemoveCurrencyUseCase
import javax.inject.Inject

class WalletFeatureUseCasesFacade @Inject constructor(
    private val isCryptoCurrencyCoinCouldHide: IsCryptoCurrencyCoinCouldHideUseCase,
    private val removeCurrencyUseCase: RemoveCurrencyUseCase,
) {

    suspend fun isCryptoCurrencyCoinCouldHide(portfolioId: PortfolioId, cryptoCurrencyCoin: CryptoCurrency.Coin) =
        when (portfolioId) {
            is PortfolioId.Account -> TODO("account")
            is PortfolioId.Wallet -> isCryptoCurrencyCoinCouldHide(portfolioId.userWalletId, cryptoCurrencyCoin)
        }

    suspend fun removeCurrencyUseCase(portfolioId: PortfolioId, currency: CryptoCurrency) = when (portfolioId) {
        is PortfolioId.Account -> TODO("account")
        is PortfolioId.Wallet -> removeCurrencyUseCase(portfolioId.userWalletId, currency)
    }
}