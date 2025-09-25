package com.tangem.domain.tokens

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

class IsCryptoCurrencyCoinCouldHideUseCase(
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, cryptoCurrencyCoin: CryptoCurrency.Coin): Boolean {
        return multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
            params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId),
        )
            .orEmpty()
            .none { it is CryptoCurrency.Token && it.network == cryptoCurrencyCoin.network }
    }
}