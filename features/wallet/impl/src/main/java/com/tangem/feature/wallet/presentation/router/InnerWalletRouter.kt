package com.tangem.feature.wallet.presentation.router

import androidx.compose.runtime.Composable
import com.tangem.features.wallet.navigation.WalletRouter

/**
 * Interface of inner wallet feature router
 *
 * @author Andrew Khokhlov on 31/05/2023
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
