package com.tangem.core.ui.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import timber.log.Timber

/**
 * The ViewModel is scoped to the parent route Navigation graph
 * and is provided using the Hilt-generated ViewModel factory
 *
 * ```
 * val navController = rememberNavController()
 *
 * navigation(
 *  route = "parent",
 *  startDestination = "parent/1"
 * ) {
 *   composable("route/1") { entry ->
 *      val viewModel = entry.parentHiltViewModel(navController)
 *   }
 *   composable("route/2") { entry ->
 *      val viewModel = entry.parentHiltViewModel(navController)
 *   }
 *   composable("route/3") { entry ->
 *      val viewModel = entry.parentHiltViewModel(navController)
 *   }
 * }
 * ```
 *
 * @param navController NavController within the common NavGraph
 * @throws Exception if there is no parent route
 */
@Composable
inline fun <reified T : ViewModel> NavBackStackEntry.parentHiltViewModel(navController: NavController): T {
    val viewModelStoreOwner = remember(this) {
        try {
            navController.getBackStackEntry(this.destination.parent!!.id)
        } catch (e: Exception) {
            Timber.tag("scopedViewModel").e(e, "There is no parent route'")
            throw e
        }
    }

    return hiltViewModel<T>(viewModelStoreOwner)
}