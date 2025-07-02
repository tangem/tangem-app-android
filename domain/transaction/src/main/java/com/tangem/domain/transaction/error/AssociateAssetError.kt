package com.tangem.domain.transaction.error

import com.tangem.domain.models.currency.CryptoCurrency

sealed class AssociateAssetError {
    data class NotEnoughBalance(val feeCurrency: CryptoCurrency) : AssociateAssetError()

    data class DataError(val message: String?) : AssociateAssetError()
}