package com.tangem.core.ui.components.bottomsheets.internal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import com.tangem.core.ui.components.bottomsheets.copy.internal.ModalBottomSheetProperties
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.copy.ModalBottomSheet
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.components.sheetscaffold.TangemSheetState
import com.tangem.core.ui.components.sheetscaffold.TangemSheetValue
import com.tangem.core.ui.components.sheetscaffold.rememberSheetState
import com.tangem.core.ui.components.snackbar.TangemTopSnackbarHost
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.LocalTopSnackbarHostState
import com.tangem.core.ui.res.TangemTheme
import dev.chrisbanes.haze.rememberHazeState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList", "LongMethod", "ComposableParametersOrdering")
fun ModalBottomSheetWithBackHandling(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    sheetState: TangemSheetState = rememberSheetState(),
    peekHeightDp: Dp,
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets },
    content: @Composable ColumnScope.() -> Unit,
) {
    val topSnackbarHostState = LocalTopSnackbarHostState.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        sheetMaxWidth = sheetMaxWidth,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        scrimColor = scrimColor,
        dragHandle = dragHandle,
        contentWindowInsets = contentWindowInsets,
        peekHeightDp = peekHeightDp,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = onBack == null,
        ),
        content = {
            Box {
                val hazeState = rememberHazeState()

                Column(Modifier.hazeSourceTangem(hazeState)) {
                    content()
                }

                CompositionLocalProvider(LocalHazeState provides hazeState) {
                    TangemTopSnackbarHost(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(horizontal = TangemTheme.dimens2.x4)
                            .padding(top = TangemTheme.dimens2.x6),
                        hostState = topSnackbarHostState,
                    )
                }
            }

            BackHandler(enabled = onBack != null && sheetState.targetValue != TangemSheetValue.Hidden) {
                onBack?.invoke()
            }
        },
    )
}