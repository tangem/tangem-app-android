package com.tangem.features.wallet.navigation

import androidx.fragment.app.Fragment

/**
 * Wallet feature router
 *
 * @author Andrew Khokhlov on 01/06/2023
 */
interface WalletRouter {

    /** Get feature entry point [Fragment] */
    fun getEntryFragment(): Fragment
}
