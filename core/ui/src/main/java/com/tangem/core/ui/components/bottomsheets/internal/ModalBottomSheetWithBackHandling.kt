package com.tangem.core.ui.components.bottomsheets.internal

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalBottomSheetWithBackHandling(
    onDismissRequest: () -> Unit,
    onBack: (() -> Unit),
    modifier: Modifier = Modifier,
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
    BackHandler(enabled = sheetState.targetValue != SheetValue.Hidden) {
        onBack()
    }

    val requester = remember { FocusRequester() }
    val backPressedDispatcherOwner = LocalOnBackPressedDispatcherOwner.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
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
        sheetState = sheetState,
        sheetMaxWidth = sheetMaxWidth,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        scrimColor = scrimColor,
        dragHandle = dragHandle,
        contentWindowInsets = contentWindowInsets,
        properties = ModalBottomSheetDefaults.properties(
            // Set false otherwise the onPreviewKeyEvent doesn't work at all.
            // The functionality of shouldDismissOnBackPress is achieved by the BackHandler.
            shouldDismissOnBackPress = false,
        ),
        content = content,
    )

    LaunchedEffect(Unit) {
        delay(timeMillis = 200)
        requester.requestFocus()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
suspend fun SheetState.collapse(onCollapsed: () -> Unit) {
    coroutineScope {
        launch { hide() }.invokeOnCompletion { onCollapsed() }
    }
}