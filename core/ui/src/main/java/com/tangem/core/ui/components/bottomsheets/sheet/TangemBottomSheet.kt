package com.tangem.core.ui.components.bottomsheets.sheet

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
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.internal.ModalBottomSheetWithBackHandling
import com.tangem.core.ui.components.bottomsheets.internal.collapse
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.LocalBottomSheetAlwaysVisible
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.WindowInsetsZero

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
    modifier: Modifier = Modifier,
    noinline onBack: (() -> Unit)? = null,
    crossinline title: @Composable (BoxScope.(T) -> Unit),
    crossinline content: @Composable (ColumnScope.(T) -> Unit),
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
            scrimColor = TangemTheme.colors.overlay.secondary,
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
            scrimColor = TangemTheme.colors.overlay.secondary,
        )
    }
}