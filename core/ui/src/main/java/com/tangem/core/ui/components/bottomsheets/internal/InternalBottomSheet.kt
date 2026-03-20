package com.tangem.core.ui.components.bottomsheets.internal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.components.snackbar.TangemTopSnackbarHost
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.LocalTopSnackbarHostState
import com.tangem.core.ui.res.TangemTheme
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InternalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState(),
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

            BackHandler(enabled = onBack != null && sheetState.targetValue != SheetValue.Hidden) {
                onBack?.invoke()
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
suspend fun SheetState.collapse(onCollapsed: () -> Unit) {
    coroutineScope {
        launch { hide() }.invokeOnCompletion { onCollapsed() }
    }
}