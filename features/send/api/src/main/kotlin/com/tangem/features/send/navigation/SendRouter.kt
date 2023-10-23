package com.tangem.features.send.navigation

import androidx.fragment.app.Fragment

interface SendRouter {

    fun getEntryFragment(): Fragment

    companion object {
        const val CRYPTO_CURRENCY_KEY = "send_crypto_currency"
    }
}