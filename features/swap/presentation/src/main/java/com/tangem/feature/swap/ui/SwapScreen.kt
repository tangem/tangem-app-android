package com.tangem.feature.swap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.models.SwapPermissionState
import com.tangem.feature.swap.models.SwapStateHolder
import com.tangem.feature.swap.presentation.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun SwapScreen(stateHolder: SwapStateHolder) {
    val coroutineScope = rememberCoroutineScope()
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = BottomSheetState(BottomSheetValue.Collapsed),
    )

    TangemTheme {
        BottomSheetScaffold(
            modifier = Modifier.background(TangemTheme.colors.background.secondary),
            topBar = {
                AppBarWithBackButton(
                    text = stringResource(R.string.swapping_swap),
                    onBackClick = stateHolder.onBackClicked,
                    iconRes = R.drawable.ic_close_24,
                )
            },
            sheetContent = {
                SwapPermissionBottomSheetContent(
                    data = stateHolder.permissionState as? SwapPermissionState.ReadyForRequest,
                    onCancel = { coroutineScope.launch { bottomSheetScaffoldState.bottomSheetState.collapse() } },
                )
            },
            scaffoldState = bottomSheetScaffoldState,
            sheetShape = RoundedCornerShape(
                topStart = TangemTheme.dimens.radius16,
                topEnd = TangemTheme.dimens.radius16,
            ),
            sheetElevation = TangemTheme.dimens.elevation24,
            sheetPeekHeight = TangemTheme.dimens.size0,
            content = {
                SwapScreenContent(
                    state = stateHolder,
                    onPermissionWarningClick = {
                        coroutineScope.launch {
                            if (bottomSheetScaffoldState.bottomSheetState.isCollapsed &&
                                stateHolder.permissionState is SwapPermissionState.ReadyForRequest
                            ) {
                                bottomSheetScaffoldState.bottomSheetState.expand()
                            } else {
                                bottomSheetScaffoldState.bottomSheetState.collapse()
                            }
                        }
                    },
                )
            },
        )
    }
}
