package com.tangem.managetokens.presentation.common.utils

import com.tangem.domain.tokens.model.CryptoCurrency

internal object CurrencyUtils {
    fun isAdded(address: String?, networkId: String, currencies: Collection<CryptoCurrency>): Boolean {
        return if (address != null) {
            currencies.any {
                !it.isCustom && it is CryptoCurrency.Token && it.contractAddress == address &&
                    it.network.backendId == networkId
            }
        } else {
            currencies.any {
                !it.isCustom && it is CryptoCurrency.Coin && it.network.backendId == networkId
            }
        }
    }
}