package com.tangem.tap.network.exchangeServices

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.domain.model.Currency
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store
import com.tangem.utils.converter.TwoWayConverter

internal class CryptoCurrencyConverter : TwoWayConverter<Currency, CryptoCurrency> {

    private val cryptoCurrencyFactory by lazy { CryptoCurrencyFactory() }

    override fun convert(value: Currency): CryptoCurrency {
        return when (value) {
            is Currency.Blockchain -> requireNotNull(
                cryptoCurrencyFactory.createCoin(
                    blockchain = value.blockchain,
                    extraDerivationPath = value.derivationPath,
                    derivationStyleProvider = requireNotNull(
                        store.inject(DaggerGraphState::generalUserWalletsListManager).selectedUserWalletSync
                            ?.scanResponse
                            ?.derivationStyleProvider,
                    ),
                ),
            )
            is Currency.Token -> requireNotNull(
                cryptoCurrencyFactory.createToken(
                    sdkToken = value.token,
                    blockchain = value.blockchain,
                    extraDerivationPath = value.derivationPath,
                    derivationStyleProvider = requireNotNull(
                        store.inject(DaggerGraphState::generalUserWalletsListManager).selectedUserWalletSync
                            ?.scanResponse
                            ?.derivationStyleProvider,
                    ),
                ),
            )
        }
    }

    override fun convertBack(value: CryptoCurrency): Currency {
        val blockchain = Blockchain.fromId(value.network.id.value)
        if (blockchain == Blockchain.Unknown) error("CryptoCurrencyConverter convertBack Unknown blockchain")
        return when (value) {
            is CryptoCurrency.Coin -> Currency.Blockchain(
                blockchain = blockchain,
                derivationPath = value.network.derivationPath.value,
            )
            is CryptoCurrency.Token -> Currency.Token(
                token = Token(
                    name = value.name,
                    symbol = value.symbol,
                    contractAddress = value.contractAddress,
                    decimals = value.decimals,
                    id = value.id.value,
                ),
                blockchain = blockchain,
                derivationPath = value.network.derivationPath.value,
            )
        }
    }
}