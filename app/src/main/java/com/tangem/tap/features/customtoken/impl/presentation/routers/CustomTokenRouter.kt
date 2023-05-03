package com.tangem.tap.features.customtoken.impl.presentation.routers

/**
 * Custom token feature router
 *
 * @author Andrew Khokhlov on 19/04/2023
 */
internal interface CustomTokenRouter {

    /** Return to last screen */
    fun popBackStack()

    /** Open wallet (main) screen */
    fun openWalletScreen()
}
