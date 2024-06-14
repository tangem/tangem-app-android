package com.tangem.features.staking.api.navigation

import androidx.fragment.app.Fragment

interface StakingRouter {

    fun getEntryFragment(): Fragment

    companion object {
        const val CRYPTO_CURRENCY_ID_KEY = "CRYPTO_CURRENCY_ID_KEY"
        const val USER_WALLET_ID_KEY = "USER_WALLET_ID_KEY"
        const val YIELD_KEY = "YIELD_KEY"
    }
}