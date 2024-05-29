package com.tangem.features.details

import androidx.fragment.app.Fragment

interface DetailsEntryPoint {

    fun entryFragment(): Fragment

    companion object {

        const val USER_WALLET_ID_KEY = "details_user_wallet_id"
    }
}
