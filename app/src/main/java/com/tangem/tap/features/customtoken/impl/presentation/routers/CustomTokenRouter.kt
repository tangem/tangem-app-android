package com.tangem.tap.features.customtoken.impl.presentation.routers

/**
 * Custom token feature router
 *
[REDACTED_AUTHOR]
 */
internal interface CustomTokenRouter {

    /** Return to last screen */
    fun popBackStack()

    /** Open wallet (main) screen */
    fun openWalletScreen()
}