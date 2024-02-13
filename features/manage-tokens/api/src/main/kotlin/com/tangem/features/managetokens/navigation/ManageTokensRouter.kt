package com.tangem.features.managetokens.navigation

import androidx.compose.ui.unit.Dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController

interface ManageTokensRouter {

    val startDestination: String

    fun NavGraphBuilder.initialize(navController: NavHostController, onHeaderSizeChange: (Dp) -> Unit)
}