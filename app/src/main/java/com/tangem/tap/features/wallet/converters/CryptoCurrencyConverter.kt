package com.tangem.tap.features.wallet.converters

import com.tangem.data.tokens.utils.CryptoCurrencyFactory
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.store
import com.tangem.utils.converter.Converter

class CryptoCurrencyConverter : Converter<Currency, CryptoCurrency> {

    private val cryptoCurrencyFactory by lazy { CryptoCurrencyFactory() }

    override fun convert(value: Currency): CryptoCurrency {
        return when (value) {
            is Currency.Blockchain -> requireNotNull(
                cryptoCurrencyFactory.createCoin(
                    blockchain = value.blockchain,
                    derivationStyleProvider = requireNotNull(
                        store.state.globalState
                            .userWalletsListManager
                            ?.selectedUserWalletSync
                            ?.scanResponse
                            ?.derivationStyleProvider,
                    ),
                ),
            )
            is Currency.Token -> requireNotNull(
                cryptoCurrencyFactory.createToken(
                    sdkToken = value.token,
                    blockchain = value.blockchain,
                    derivationStyleProvider = requireNotNull(
                        store.state.globalState
                            .userWalletsListManager
                            ?.selectedUserWalletSync
                            ?.scanResponse
                            ?.derivationStyleProvider,
                    ),
                ),
            )
        }
    }
}