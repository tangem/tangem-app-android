package com.tangem.core.ui.components.bottomsheets.modal

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.internal.ModalBottomSheetWithBackHandling
import com.tangem.core.ui.components.bottomsheets.internal.collapse
import com.tangem.core.ui.components.sheetscaffold.TangemSheetState
import com.tangem.core.ui.components.sheetscaffold.TangemSheetValue
import com.tangem.core.ui.components.sheetscaffold.rememberSheetState
import com.tangem.core.ui.res.*
import com.tangem.core.ui.utils.WindowInsetsZero

const val MODAL_SHEET_MAX_HEIGHT = 0.8f

/**
 * Modal bottom sheet with [content] and optional [title].
 *
 * Maximum height of sheet is 80% screen height
 *
 * [Show in Figma](https://www.figma.com/design/09KKG4ZVuFDZhj8WLv5rGJ/%F0%9F%9A%A7-App-experience?node-id=3254-61208&t=vixa7id6ggALcxfF-4)
 */
@Composable
inline fun <reified T : TangemBottomSheetConfigContent> TangemModalBottomSheet(
    config: TangemBottomSheetConfig,
    containerColor: Color = TangemTheme.colors.background.primary,
    noinline onBack: (() -> Unit)? = null,
    skipPartiallyExpanded: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    scrollableContent: Boolean = true,
    crossinline title: @Composable BoxScope.(T) -> Unit = {},
    crossinline content: @Composable ColumnScope.(T) -> Unit,
) {
    val isAlwaysVisible = LocalBottomSheetAlwaysVisible.current

    if (isAlwaysVisible) {
        PreviewModalBottomSheet<T>(
            config = config,
            containerColor = containerColor,
            title = title,
            content = content,
            scrollableContent = scrollableContent,
            skipPartiallyExpanded = skipPartiallyExpanded,
        )
    } else {
        DefaultModalBottomSheet<T>(
            config = config,
            containerColor = containerColor,
            title = title,
            content = content,
            dismissOnClickOutside = dismissOnClickOutside,
            scrollableContent = scrollableContent,
            onBack = onBack,
            skipPartiallyExpanded = skipPartiallyExpanded,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
inline fun <reified T : TangemBottomSheetConfigContent> DefaultModalBottomSheet(
    config: TangemBottomSheetConfig,
    containerColor: Color,
    skipPartiallyExpanded: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    scrollableContent: Boolean = true,
    noinline onBack: (() -> Unit)? = null,
    crossinline title: @Composable (BoxScope.(T) -> Unit),
    crossinline content: @Composable (ColumnScope.(T) -> Unit),
) {
    var isVisible by remember { mutableStateOf(value = config.isShown) }
    val sheetState = rememberSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded,
        confirmValueChange = { sheetValue ->
            if (!dismissOnClickOutside) {
                // Ignore transitions to hidden (prevents dismiss on outside click/back press)
                sheetValue != TangemSheetValue.Hidden
            } else {
                true
            }
        },
    )
    val windowSize = LocalWindowSize.current
    val maxHeight = windowSize.height * MODAL_SHEET_MAX_HEIGHT

    if (isVisible && config.content is T) {
        BasicModalBottomSheet<T>(
            config = config,
            sheetState = sheetState,
            onBack = onBack,
            peekHeightDp = maxHeight,
            bsContent = {
                BsContent(
                    config = config,
                    containerColor = containerColor,
                    scrollableContent = scrollableContent,
                    title = title,
                    content = content,
                )
            },
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
inline fun <reified T : TangemBottomSheetConfigContent> PreviewModalBottomSheet(
    config: TangemBottomSheetConfig,
    containerColor: Color,
    skipPartiallyExpanded: Boolean = true,
    scrollableContent: Boolean = true,
    crossinline title: @Composable (BoxScope.(T) -> Unit),
    crossinline content: @Composable (ColumnScope.(T) -> Unit),
) {
    val windowSize = LocalWindowSize.current
    val maxHeight = windowSize.height * MODAL_SHEET_MAX_HEIGHT
    BasicModalBottomSheet<T>(
        config = config,
        sheetState = rememberSheetState(
            skipPartiallyExpanded = skipPartiallyExpanded,
            initialValue = TangemSheetValue.Expanded,
        ),
        onBack = null,
        peekHeightDp = maxHeight,
        bsContent = {
            BsContent(
                config = config,
                containerColor = containerColor,
                scrollableContent = scrollableContent,
                title = title,
                content = content,
            )
        },
    )
}

@Composable
inline fun <reified T : TangemBottomSheetConfigContent> BsContent(
    config: TangemBottomSheetConfig,
    containerColor: Color,
    scrollableContent: Boolean = true,
    crossinline title: @Composable (BoxScope.(T) -> Unit),
    crossinline content: @Composable (ColumnScope.(T) -> Unit),
) {
    val model = config.content as? T ?: return

    val maxHeight = LocalConfiguration.current.screenHeightDp * MODAL_SHEET_MAX_HEIGHT

    val canScrollBackward = LocalCanScrollBackward.current

    val nestedScrollConnection = remember(canScrollBackward) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
                if (canScrollBackward) available else Offset.Zero

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                if (canScrollBackward) available else Velocity.Zero
        }
    }

    Column(
        modifier = Modifier
            .systemBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .clip(TangemTheme.shapes.roundedCornersLarge)
            .background(containerColor)
            .heightIn(max = maxHeight.dp)
            .fillMaxWidth()
            .nestedScroll(nestedScrollConnection),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            title(model)
        }
        if (scrollableContent) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                content(model)
            }
        } else {
            content(model)
        }
    }
}

@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
inline fun <reified T : TangemBottomSheetConfigContent> BasicModalBottomSheet(
    config: TangemBottomSheetConfig,
    sheetState: TangemSheetState,
    peekHeightDp: Dp,
    modifier: Modifier = Modifier,
    noinline onBack: (() -> Unit)? = null,
    noinline bsContent: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheetWithBackHandling(
        modifier = modifier,
        onDismissRequest = config.onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        shape = TangemTheme.shapes.roundedCornersLarge,
        contentWindowInsets = { WindowInsetsZero },
        onBack = onBack,
        dragHandle = null,
        content = bsContent,
        peekHeightDp = peekHeightDp,
        scrimColor = TangemTheme.colors.overlay.secondary,
    )
}

// region Preview
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemModalBottomSheet_Preview() {
    TangemThemePreview {
        Box(
            Modifier.background(TangemTheme.colors.background.tertiary),
        ) {
            TangemModalBottomSheet<TangemBottomSheetConfigContentPreviewConfig>(
                config = TangemBottomSheetConfig(
                    isShown = true,
                    onDismissRequest = {},
                    content = TangemBottomSheetConfigContentPreviewConfig(),
                ),
                title = {
                    TangemModalBottomSheetTitle(
                        endIconRes = R.drawable.ic_close_24,
                        onEndClick = {},
                    )
                },
                content = {
                    Column(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(100))
                                .background(TangemTheme.colors.icon.informative.copy(alpha = 0.1f))
                                .padding(12.dp),
                            painter = rememberVectorPainter(
                                ImageVector.vectorResource(R.drawable.ic_alert_24),
                            ),
                            tint = TangemTheme.colors.icon.informative,
                            contentDescription = null,
                        )
                        SpacerH24()
                        Text(
                            text = "Unsuported networks",
                            style = TangemTheme.typography.h3,
                            color = TangemTheme.colors.text.primary1,
                            textAlign = TextAlign.Center,
                        )
                        SpacerH8()
                        Text(
                            text = "Tangem does not currently support a required network by React App.",
                            style = TangemTheme.typography.body2,
                            color = TangemTheme.colors.text.secondary,
                            textAlign = TextAlign.Center,
                        )
                        SpacerH(48.dp)
                        PrimaryButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Go it",
                            onClick = {},
                        )
                    }
                },
            )
        }
    }
}

private class TangemBottomSheetConfigContentPreviewConfig : TangemBottomSheetConfigContent
// endregion