package com.tangem.feature.swap.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.models.SwapPermissionState
import com.tangem.feature.swap.models.SwapStateHolder
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun SwapScreen(stateHolder: SwapStateHolder) {
    val coroutineScope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true,
    )

    TangemTheme {
        ModalBottomSheetLayout(
            sheetContent = {
                if (stateHolder.permissionState is SwapPermissionState.ReadyForRequest) {
                    SwapPermissionBottomSheetContent(
                        data = stateHolder.permissionState,
                        onCancel = {
                            coroutineScope.launch {
                                stateHolder.onCancelPermissionBottomSheet.invoke()
                                bottomSheetState.hide()
                            }
                        },
                    )
                } else {
                    // Required "else" block to prevent compose crash
                    // always close BS if its empty, cause user should not see this
                    LaunchedEffect(Unit) {
                        coroutineScope.launch { bottomSheetState.hide() }
                    }
                    Box(modifier = Modifier.fillMaxSize())
                }
            },
            sheetState = bottomSheetState,
            sheetShape = RoundedCornerShape(
                topStart = TangemTheme.dimens.radius16,
                topEnd = TangemTheme.dimens.radius16,
            ),
            sheetElevation = TangemTheme.dimens.elevation24,
            content = {
                SwapScreenContent(
                    state = stateHolder,
                    onPermissionWarningClick = {
                        val isBottomSheetReady = !bottomSheetState.isVisible &&
                            stateHolder.permissionState is SwapPermissionState.ReadyForRequest
                        coroutineScope.launch {
                            if (isBottomSheetReady) {
                                bottomSheetState.show()
                                stateHolder.onShowPermissionBottomSheet.invoke()
                            } else {
                                bottomSheetState.hide()
                            }
                        }
                    },
                )
            },
        )
    }
}
