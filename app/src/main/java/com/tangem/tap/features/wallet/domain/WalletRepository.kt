package com.tangem.tap.features.wallet.domain

import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse

/** Repository for Wallet feature */
interface WalletRepository {

    /** Get list of currency */
    suspend fun getCurrencyList(): CurrenciesResponse
}
