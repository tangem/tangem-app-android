package com.tangem.tap.features.tokens.presentation.router

/** Tokens list feature router */
internal interface TokensListRouter {

    /** Return to last screen */
    fun popBackStack()

    /** Open screen for adding custom token */
    fun openAddCustomTokenScreen()
}