package com.tangem.core.ui.components.bottomsheets

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.LocalBottomSheetAlwaysVisible
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.WindowInsetsZero
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Bottom sheet with [content], [titleText] and optional [titleAction].
 * */
@Composable
inline fun <reified T : TangemBottomSheetConfigContent> TangemBottomSheet(
    config: TangemBottomSheetConfig,
    titleText: TextReference,
    titleAction: TopAppBarButtonUM? = null,
    containerColor: Color = TangemTheme.colors.background.primary,
    addBottomInsets: Boolean = true,
    skipPartiallyExpanded: Boolean = true,
    noinline onBack: (() -> Unit)? = null,
    crossinline content: @Composable ColumnScope.(T) -> Unit,
) {
    TangemBottomSheet(
        config = config,
        containerColor = containerColor,
        addBottomInsets = addBottomInsets,
        title = { TangemBottomSheetTitle(title = titleText, endButton = titleAction) },
        skipPartiallyExpanded = skipPartiallyExpanded,
        onBack = onBack,
        content = content,
    )
}

/**
 * Bottom sheet with [content] and optional [title].
 */
@Composable
inline fun <reified T : TangemBottomSheetConfigContent> TangemBottomSheet(
    config: TangemBottomSheetConfig,
    containerColor: Color = TangemTheme.colors.background.primary,
    addBottomInsets: Boolean = true,
    skipPartiallyExpanded: Boolean = true,
    noinline onBack: (() -> Unit)? = null,
    crossinline title: @Composable BoxScope.(T) -> Unit = {},
    crossinline content: @Composable ColumnScope.(T) -> Unit,
) {
    val isAlwaysVisible = LocalBottomSheetAlwaysVisible.current

    if (isAlwaysVisible) {
        PreviewBottomSheet<T>(
            config = config,
            containerColor = containerColor,
            addBottomInsets = addBottomInsets,
            title = title,
            content = content,
            skipPartiallyExpanded = skipPartiallyExpanded,
        )
    } else {
        DefaultBottomSheet<T>(
            config = config,
            containerColor = containerColor,
            addBottomInsets = addBottomInsets,
            title = title,
            content = content,
            onBack = onBack,
            skipPartiallyExpanded = skipPartiallyExpanded,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
inline fun <reified T : TangemBottomSheetConfigContent> DefaultBottomSheet(
    config: TangemBottomSheetConfig,
    containerColor: Color,
    addBottomInsets: Boolean,
    skipPartiallyExpanded: Boolean = true,
    noinline onBack: (() -> Unit)? = null,
    crossinline title: @Composable (BoxScope.(T) -> Unit),
    crossinline content: @Composable (ColumnScope.(T) -> Unit),
) {
    var isVisible by remember { mutableStateOf(value = config.isShown) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)

    if (isVisible && config.content is T) {
        BasicBottomSheet<T>(
            config = config,
            sheetState = sheetState,
            containerColor = containerColor,
            addBottomInsets = addBottomInsets,
            title = title,
            onBack = onBack,
            content = content,
        )
    }

    LaunchedEffect(key1 = config.isShown) {
        if (config.isShown) {
            isVisible = true
        } else {
            sheetState.collapse { isVisible = false }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
inline fun <reified T : TangemBottomSheetConfigContent> PreviewBottomSheet(
    config: TangemBottomSheetConfig,
    containerColor: Color,
    addBottomInsets: Boolean,
    skipPartiallyExpanded: Boolean = true,
    crossinline title: @Composable (BoxScope.(T) -> Unit),
    crossinline content: @Composable (ColumnScope.(T) -> Unit),
) {
    BasicBottomSheet<T>(
        modifier = Modifier.width(360.dp),
        config = config,
        sheetState = SheetState(
            skipPartiallyExpanded = skipPartiallyExpanded,
            initialValue = Expanded,
            density = LocalDensity.current,
        ),
        onBack = null,
        containerColor = containerColor,
        addBottomInsets = addBottomInsets,
        title = title,
        content = content,
    )
}

@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
inline fun <reified T : TangemBottomSheetConfigContent> BasicBottomSheet(
    config: TangemBottomSheetConfig,
    sheetState: SheetState,
    containerColor: Color,
    addBottomInsets: Boolean,
    noinline onBack: (() -> Unit)? = null,
    crossinline title: @Composable (BoxScope.(T) -> Unit),
    crossinline content: @Composable (ColumnScope.(T) -> Unit),
    modifier: Modifier = Modifier,
) {
    val model = config.content as? T ?: return

    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

    val bsContent: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier.let {
                if (addBottomInsets) {
                    it.padding(bottom = bottomBarHeight)
                } else {
                    it
                }
            },
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                title(model)
            }

            content(model)
        }
    }

    if (onBack != null) {
        ModalBottomSheetWithBackHandling(
            modifier = modifier.statusBarsPadding(),
            onDismissRequest = config.onDismissRequest,
            sheetState = sheetState,
            containerColor = containerColor,
            shape = TangemTheme.shapes.bottomSheetLarge,
            contentWindowInsets = { WindowInsetsZero },
            dragHandle = { TangemBottomSheetDraggableHeader(color = containerColor) },
            onBack = onBack,
            content = bsContent,
        )
    } else {
        ModalBottomSheet(
            modifier = modifier.statusBarsPadding(),
            onDismissRequest = config.onDismissRequest,
            sheetState = sheetState,
            containerColor = containerColor,
            shape = TangemTheme.shapes.bottomSheetLarge,
            contentWindowInsets = { WindowInsetsZero },
            dragHandle = { TangemBottomSheetDraggableHeader(color = containerColor) },
            content = bsContent,
        )
    }
}

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