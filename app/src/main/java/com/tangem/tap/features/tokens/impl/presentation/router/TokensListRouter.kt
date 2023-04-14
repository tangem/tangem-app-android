package com.tangem.tap.features.tokens.impl.presentation.router

/** Tokens list feature router */
internal interface TokensListRouter {

    /** Return to last screen */
    fun popBackStack()

    /** Open adding custom token screen */
    fun openAddCustomTokenScreen()

    /** Show notification to inform about copied address */
    fun showAddressCopiedNotification()

    /**
     * Open alert if unable to hide the main token
     *
     * @param tokenName   token name
     * @param tokenSymbol token brief name
     */
    fun openUnableHideMainTokenAlert(tokenName: String, tokenSymbol: String)

    /**
     * Open alert to remove wallet
     *
     * @param tokenName token name
     * @param onOkClick lambda be invoked if OkButton is been clicked
     */
    fun openRemoveWalletAlert(tokenName: String, onOkClick: () -> Unit)

    /** Open alert if solana network is unsupported */
    fun openUnsupportedSoltanaNetworkAlert()
}
