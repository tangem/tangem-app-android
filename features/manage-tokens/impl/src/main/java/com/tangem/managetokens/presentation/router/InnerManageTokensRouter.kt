package com.tangem.managetokens.presentation.router

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelStoreOwner
import com.tangem.core.navigation.AppScreen
import com.tangem.features.managetokens.navigation.ManageTokensRouter

internal interface InnerManageTokensRouter : ManageTokensRouter {
    /**
     * Initialize router
     **/
    @Suppress("TopLevelComposableFunctions")
    @Composable
    fun Initialize(viewModelStoreOwner: ViewModelStoreOwner)

    /** Pop back stack */
    fun popBackStack(screen: AppScreen? = null)

    /** Open manage tokens screen */
    fun openManageTokensScreen()

    /** Open custom tokens screen */
    fun openCustomTokensScreen()

    fun openCustomTokensChooseNetwork()

    fun openCustomTokensChooseDerivation()

    fun openCustomTokensChooseWallet()
}