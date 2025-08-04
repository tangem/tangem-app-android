package com.tangem.domain.tokens

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.models.wallet.UserWalletId

class IsCryptoCurrencyCoinCouldHideUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    private val tokensFeatureToggles: TokensFeatureToggles,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, cryptoCurrencyCoin: CryptoCurrency.Coin): Boolean {
        return if (tokensFeatureToggles.isWalletBalanceFetcherEnabled) {
            multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
                params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId),
            )
                .orEmpty()
        } else {
            currenciesRepository.getMultiCurrencyWalletCurrenciesSync(
                userWalletId = userWalletId,
                refresh = false,
            )
        }
            .none { it is CryptoCurrency.Token && it.network == cryptoCurrencyCoin.network }
    }
}