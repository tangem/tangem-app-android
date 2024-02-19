package com.tangem.managetokens.presentation.addcustomtoken.ui

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import kotlinx.coroutines.launch

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

        ModalBottomSheetWithBackHandling(
            onDismissRequest = config.onDismissRequest,
            sheetState = sheetState,
            containerColor = TangemTheme.colors.background.tertiary,
            shape = TangemTheme.shapes.bottomSheetLarge,
            windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
            dragHandle = { TangemBottomSheetDraggableHeader(color = TangemTheme.colors.background.tertiary) },
            properties = ModalBottomSheetDefaults.properties(shouldDismissOnBackPress = false),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModalBottomSheetWithBackHandling(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    windowInsets: WindowInsets = BottomSheetDefaults.windowInsets,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    sheetState: SheetState = rememberModalBottomSheetState(),
    properties: ModalBottomSheetProperties = ModalBottomSheetDefaults.properties(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()

    BackHandler(enabled = sheetState.targetValue != SheetValue.Hidden) {
        // Always catch back here, but only let it dismiss if shouldDismissOnBackPress.
        // If not, it will have no effect.
        if (properties.shouldDismissOnBackPress) {
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) {
                    onDismissRequest()
                }
            }
        }
    }

    val requester = remember { FocusRequester() }
    val backPressedDispatcherOwner = LocalOnBackPressedDispatcherOwner.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = containerColor,
        shape = shape,
        windowInsets = windowInsets,
        dragHandle = dragHandle,
        sheetState = sheetState,
        modifier = modifier
            .focusRequester(requester)
            .focusable()
            .onPreviewKeyEvent {
                if (it.key == Key.Back && it.type == KeyEventType.KeyUp && !it.nativeKeyEvent.isCanceled) {
                    backPressedDispatcherOwner?.onBackPressedDispatcher?.onBackPressed()
                    return@onPreviewKeyEvent true
                }
                return@onPreviewKeyEvent false
            },
        properties = ModalBottomSheetDefaults.properties(
            securePolicy = properties.securePolicy,
            isFocusable = properties.isFocusable,
            // Set false otherwise the onPreviewKeyEvent doesn't work at all.
            // The functionality of shouldDismissOnBackPress is achieved by the BackHandler.
            shouldDismissOnBackPress = false,
        ),
        content = content,
    )

    LaunchedEffect(Unit) {
        requester.requestFocus()
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

    BackHandler(true) {
        navController.popBackStack()
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