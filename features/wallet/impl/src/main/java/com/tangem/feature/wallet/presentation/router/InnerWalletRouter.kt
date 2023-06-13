package com.tangem.feature.wallet.presentation.router

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.tangem.features.wallet.navigation.WalletRouter

/**
 * Interface of inner wallet feature router
 *
 * Annotated as [Stable] because implementation of this interface passed as argument to [Initialize] method by compose
 * compiler
 *
[REDACTED_AUTHOR]
 */
@Stable
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