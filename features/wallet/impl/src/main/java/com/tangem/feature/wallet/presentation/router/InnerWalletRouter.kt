package com.tangem.feature.wallet.presentation.router

import androidx.compose.runtime.Composable
import com.tangem.features.wallet.navigation.WalletRouter

/**
 * Interface of inner wallet feature router
 *
[REDACTED_AUTHOR]
 */
internal interface InnerWalletRouter : WalletRouter {

    /** Initialize router */
    @Suppress("TopLevelComposableFunctions")
    @Composable
    fun Initialize()

    /** Pop back stack */
    fun popBackStack()

    /** Open organize tokens screen */
    fun openOrganizeTokensScreen()
}