package com.tangem.features.tokendetails.navigation

import androidx.fragment.app.Fragment

interface TokenDetailsRouter {

    fun getEntryFragment(): Fragment

    companion object {
        const val CRYPTO_CURRENCY_KEY = "token_details_crypto_currency"
    }
}