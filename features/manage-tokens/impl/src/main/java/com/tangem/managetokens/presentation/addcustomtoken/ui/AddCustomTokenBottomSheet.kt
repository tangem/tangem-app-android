package com.tangem.managetokens.presentation.addcustomtoken.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetDraggableHeader
import com.tangem.core.ui.components.bottomsheets.collapse
import com.tangem.core.ui.res.TangemTheme
import com.tangem.managetokens.presentation.addcustomtoken.router.AddCustomTokenRoute
import com.tangem.managetokens.presentation.addcustomtoken.router.AddCustomTokenRouter
import com.tangem.managetokens.presentation.addcustomtoken.viewmodels.AddCustomTokenViewModel
import com.tangem.managetokens.presentation.common.state.ChooseWalletState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomTokenBottomSheet(config: TangemBottomSheetConfig) {
    val viewModel = hiltViewModel<AddCustomTokenViewModel>()

    var isVisible by remember { mutableStateOf(value = config.isShow) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (isVisible) {
        // ViewModel cannot be scoped to ModalBottomSheet's lifecycle,
        // so we have to manually initialize and dispose its state when bottom sheet enters and leaves a composition
        DisposableEffect(viewModel) {
            viewModel.onInitialize()
            onDispose { viewModel.onDispose() }
        }

        // FIXME: handle back presses after updating material3 to 1.2.0
        ModalBottomSheet(
            onDismissRequest = config.onDismissRequest,
            sheetState = sheetState,
            containerColor = TangemTheme.colors.background.tertiary,
            shape = TangemTheme.shapes.bottomSheetLarge,
            windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
            dragHandle = { TangemBottomSheetDraggableHeader(color = TangemTheme.colors.background.tertiary) },
        ) {
            Content(onDismissRequest = config.onDismissRequest, viewModel = viewModel)
        }
    }

    LaunchedEffect(key1 = config.isShow) {
        if (config.isShow) {
            isVisible = true
        } else {
            sheetState.collapse { isVisible = false }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun Content(viewModel: AddCustomTokenViewModel, onDismissRequest: () -> Unit) {
    val navController = rememberNavController()

    LaunchedEffect(navController) {
        navController.currentBackStack
            .collect {
                if (it.isEmpty()) {
                    onDismissRequest()
                }
            }
    }

    val router = remember(navController) { AddCustomTokenRouter(navController) }

    viewModel.router = router

    NavHost(
        modifier = Modifier.fillMaxSize(),
        navController = navController,
        startDestination = AddCustomTokenRoute.Main.route,
    ) {
        composable(
            route = AddCustomTokenRoute.Main.route,
        ) {
            AddCustomTokenScreen(state = viewModel.uiState)
        }
        composable(
            route = AddCustomTokenRoute.ChooseNetwork.route,
        ) {
            ChooseNetworkCustomScreen(state = viewModel.uiState.chooseNetworkState)
        }
        composable(
            route = AddCustomTokenRoute.ChooseDerivation.route,
        ) {
            ChooseDerivationScreen(state = requireNotNull(viewModel.uiState.chooseDerivationState))
        }
        composable(
            route = AddCustomTokenRoute.ChooseWallet.route,
        ) {
            CustomTokensChooseWalletScreen(state = viewModel.uiState.chooseWalletState as ChooseWalletState.Choose)
        }
    }
}