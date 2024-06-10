package com.tangem.domain.transaction.error

import com.tangem.domain.tokens.model.CryptoCurrency

sealed class AssociateAssetError {
    data class NotEnoughBalance(val feeCurrency: CryptoCurrency) : AssociateAssetError()

    data class DataError(val message: String?) : AssociateAssetError()
}