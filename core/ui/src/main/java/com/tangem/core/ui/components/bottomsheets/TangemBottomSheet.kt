package com.tangem.core.ui.components.bottomsheets

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.LocalBottomSheetAlwaysVisible
import com.tangem.core.ui.res.LocalWindowSize
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.WindowInsetsZero
import kotlinx.coroutines.coroutineScope
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
    crossinline content: @Composable ColumnScope.(T) -> Unit,
) {
    TangemBottomSheet(
        config = config,
        containerColor = containerColor,
        addBottomInsets = addBottomInsets,
        title = { TangemBottomSheetTitle(title = titleText, endButton = titleAction) },
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
        )
    } else {
        DefaultBottomSheet<T>(
            config = config,
            containerColor = containerColor,
            addBottomInsets = addBottomInsets,
            title = title,
            content = content,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
inline fun <reified T : TangemBottomSheetConfigContent> DefaultBottomSheet(
    config: TangemBottomSheetConfig,
    containerColor: Color,
    addBottomInsets: Boolean,
    crossinline title: @Composable (BoxScope.(T) -> Unit),
    crossinline content: @Composable (ColumnScope.(T) -> Unit),
) {
    var isVisible by remember { mutableStateOf(value = config.isShow) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (isVisible && config.content is T) {
        BasicBottomSheet<T>(
            config = config,
            sheetState = sheetState,
            containerColor = containerColor,
            addBottomInsets = addBottomInsets,
            title = title,
            content = content,
        )
    }

    LaunchedEffect(key1 = config.isShow) {
        if (config.isShow) {
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
    crossinline title: @Composable (BoxScope.(T) -> Unit),
    crossinline content: @Composable (ColumnScope.(T) -> Unit),
) {
    BasicBottomSheet<T>(
        config = config,
        sheetState = SheetState(
            skipPartiallyExpanded = true,
            initialValue = Expanded,
            density = LocalDensity.current,
        ),
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
    crossinline title: @Composable (BoxScope.(T) -> Unit),
    crossinline content: @Composable (ColumnScope.(T) -> Unit),
) {
    val model = config.content as? T ?: return

    val statusBarHeight = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }

    ModalBottomSheet(
// [REDACTED_TODO_COMMENT]
        modifier = Modifier.sizeIn(maxHeight = LocalWindowSize.current.height - statusBarHeight),
        onDismissRequest = config.onDismissRequest,
        sheetState = sheetState,
        containerColor = containerColor,
        shape = TangemTheme.shapes.bottomSheetLarge,
        windowInsets = WindowInsetsZero,
        dragHandle = { TangemBottomSheetDraggableHeader(color = containerColor) },
    ) {
        Column(
            modifier = Modifier.let {
                if (addBottomInsets) {
                    it.navigationBarsPadding()
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
}

@OptIn(ExperimentalMaterial3Api::class)
suspend fun SheetState.collapse(onCollapsed: () -> Unit) {
    coroutineScope {
        launch { hide() }.invokeOnCompletion { onCollapsed() }
    }
}
