package com.tangem.features.send.api.navigation

import androidx.fragment.app.Fragment

interface SendRouter {

    fun getEntryFragment(): Fragment

    companion object {
        const val CRYPTO_CURRENCY_KEY = "send_crypto_currency"
        const val USER_WALLET_ID_KEY = "send_user_wallet_id"
    }
}