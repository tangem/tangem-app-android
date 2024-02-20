package com.tangem.managetokens.presentation.addcustomtoken.router

import androidx.compose.runtime.Stable
import androidx.navigation.NavController

@Stable
internal class AddCustomTokenRouter(
    private val navController: NavController,
) {
    /** Pop back stack */
    fun popBackStack() {
        navController.popBackStack()
    }

    /** Open custom token choose network screen */
    fun openCustomTokenChooseNetwork() {
        navController.navigate(AddCustomTokenRoute.ChooseNetwork.route)
    }

    /** Open custom token choose derivation screen */
    fun openCustomTokenChooseDerivation() {
        navController.navigate(AddCustomTokenRoute.ChooseDerivation.route)
    }

    /** Open custom token choose wallet screen */
    fun openCustomTokenChooseWallet() {
        navController.navigate(AddCustomTokenRoute.ChooseWallet.route)
    }
}