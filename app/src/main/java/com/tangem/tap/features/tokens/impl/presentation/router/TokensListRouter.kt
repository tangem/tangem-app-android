package com.tangem.tap.features.tokens.impl.presentation.router

import com.tangem.blockchain.common.Blockchain

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
     * @param networkName blockchain network full name
     */
    fun openUnableHideMainTokenAlert(tokenName: String, tokenSymbol: String, networkName: String)

    /**
     * Open alert to remove wallet
     *
     * @param tokenName token name
     * @param onOkClick lambda be invoked if OkButton is been clicked
     */
    fun openRemoveWalletAlert(tokenName: String, onOkClick: () -> Unit)

    /** Open alert if solana network is unsupported
     *
     * @param blockchain blockchain to show alert
     */
    fun openUnsupportedNetworkAlert(blockchain: Blockchain)

    fun showGenericErrorAlertAndPopBack()

    /**
     * Open alert with unsupported networks tokens error
     */
    fun openNetworkTokensNotSupportAlert(networkName: String)
}