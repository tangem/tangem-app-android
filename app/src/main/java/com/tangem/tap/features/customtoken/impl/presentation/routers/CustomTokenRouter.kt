package com.tangem.tap.features.customtoken.impl.presentation.routers

import com.tangem.blockchain.common.Blockchain

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

    /** Open alert if solana network is unsupported
     *
     * @param blockchain blockchain to show alert
     */
    fun openUnsupportedNetworkAlert(blockchain: Blockchain)
}