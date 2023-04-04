package com.tangem.tap.features.tokens.presentation.router

/** Add tokens feature router */
internal interface AddTokensRouter {

    /** Return to last screen */
    fun popBackStack()

    /** Open screen for adding custom token */
    fun openAddCustomTokenScreen()
}