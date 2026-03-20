package com.tangem.core.ui.components.bottomsheets

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType.Default
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType.Modal
import com.tangem.core.ui.components.bottomsheets.internal.InternalBottomSheet
import com.tangem.core.ui.components.bottomsheets.internal.collapse
import com.tangem.core.ui.components.bottomsheets.modal.MODAL_SHEET_MAX_HEIGHT
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheetDraggableHeader
import com.tangem.core.ui.res.LocalBottomSheetAlwaysVisible
import com.tangem.core.ui.res.LocalWindowSize
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.utils.WindowInsetsZero

/**
 * Type of [TangemBottomSheet] that defines its behavior and appearance.
 * - [Default]: Standard bottom sheet with a draggable header
 * - [Modal]: Modal bottom sheet without a draggable header
 */
enum class TangemBottomSheetType {
    Default, Modal;

    fun getDragHandle(): (@Composable (() -> Unit))? = when (this) {
        Default -> {
            { TangemBottomSheetDraggableHeader() }
        }
        Modal -> null
    }
}

/**
 * Modal bottom sheet with [content] and optional [title] and [footer].
 *
 * @param config Configuration for the bottom sheet, including visibility and content data.
 * @param type Type of the bottom sheet that defines its behavior and appearance.
 * @param containerColor Background color of the bottom sheet container.
 * @param skipPartiallyExpanded Whether to skip the partially expanded state when dragging the sheet.
 * @param onBack Optional callback for handling back press when the sheet is visible.
 * @param title Optional composable for rendering the title section of the sheet, receiving the content
 * model as a parameter.
 * @param content Composable for rendering the main content of the sheet, receiving the content model
 * as a parameter.
 * @param footer Optional composable for rendering the footer section of the sheet, receiving the content
 * model as a parameter.
 *
 * [Show in Figma](https://www.figma.com/design/RU7AIgwHtGdMfy83T5UOoR/Core-Library?node-id=8454-23749&m=dev)
 */
@Composable
inline fun <reified T : TangemBottomSheetConfigContent> TangemBottomSheet(
    config: TangemBottomSheetConfig,
    type: TangemBottomSheetType = Default,
    containerColor: Color = TangemTheme.colors2.surface.level2,
    skipPartiallyExpanded: Boolean = true,
    noinline onBack: (() -> Unit)? = null,
    crossinline title: @Composable BoxScope.(T) -> Unit = {},
    crossinline content: @Composable (T) -> Unit,
    noinline footer: @Composable (BoxScope.(T) -> Unit)? = null,
) {
    val isAlwaysVisible = LocalBottomSheetAlwaysVisible.current

    if (isAlwaysVisible) {
        PreviewModalBottomSheetWithFooter<T>(
            config = config,
            containerColor = containerColor,
            type = type,
            title = title,
            content = content,
            footer = footer,
            skipPartiallyExpanded = skipPartiallyExpanded,
        )
    } else {
        DefaultModalBottomSheetWithFooter<T>(
            config = config,
            containerColor = containerColor,
            type = type,
            title = title,
            content = content,
            footer = footer,
            onBack = onBack,
            skipPartiallyExpanded = skipPartiallyExpanded,
        )
    }
}

@Suppress("LongParameterList")
@Composable
@OptIn(ExperimentalMaterial3Api::class)
inline fun <reified T : TangemBottomSheetConfigContent> DefaultModalBottomSheetWithFooter(
    config: TangemBottomSheetConfig,
    containerColor: Color,
    type: TangemBottomSheetType,
    skipPartiallyExpanded: Boolean = true,
    noinline onBack: (() -> Unit)? = null,
    crossinline title: @Composable BoxScope.(T) -> Unit,
    crossinline content: @Composable (T) -> Unit,
    noinline footer: @Composable (BoxScope.(T) -> Unit)?,
) {
    var isVisible by remember { mutableStateOf(value = config.isShown) }

    val sheetState = if (config.dismissOnClickOutside == null) {
        rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)
    } else {
        rememberModalBottomSheetState(
            skipPartiallyExpanded = skipPartiallyExpanded,
            confirmValueChange = { sheetValue ->
                if (config.dismissOnClickOutside().not()) {
                    // Ignore transitions to hidden (prevents dismiss on outside click/back press)
                    sheetValue != SheetValue.Hidden
                } else {
                    true
                }
            },
        )
    }

    if (isVisible && config.content is T) {
        BasicBottomSheet<T>(
            config = config,
            sheetState = sheetState,
            containerColor = containerColor,
            type = type,
            title = title,
            onBack = onBack,
            content = content,
            footer = footer,
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

@Suppress("LongParameterList")
@Composable
@OptIn(ExperimentalMaterial3Api::class)
inline fun <reified T : TangemBottomSheetConfigContent> PreviewModalBottomSheetWithFooter(
    config: TangemBottomSheetConfig,
    containerColor: Color,
    type: TangemBottomSheetType,
    skipPartiallyExpanded: Boolean = true,
    crossinline title: @Composable BoxScope.(T) -> Unit,
    crossinline content: @Composable (T) -> Unit,
    noinline footer: @Composable (BoxScope.(T) -> Unit)?,
) {
    BasicBottomSheet<T>(
        config = config,
        sheetState = SheetState(
            skipPartiallyExpanded = skipPartiallyExpanded,
            initialValue = Expanded,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
        ),
        onBack = null,
        containerColor = containerColor,
        type = type,
        title = title,
        content = content,
        footer = footer,
    )
}

@Suppress("LongParameterList", "LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
inline fun <reified T : TangemBottomSheetConfigContent> BasicBottomSheet(
    config: TangemBottomSheetConfig,
    sheetState: SheetState,
    containerColor: Color,
    type: TangemBottomSheetType,
    modifier: Modifier = Modifier,
    noinline onBack: (() -> Unit)? = null,
    crossinline title: @Composable BoxScope.(T) -> Unit,
    crossinline content: @Composable (T) -> Unit,
    noinline footer: @Composable (BoxScope.(T) -> Unit)?,
) {
    val model = config.content as? T ?: return
    val windowSize = LocalWindowSize.current

    val bsContent: @Composable ColumnScope.() -> Unit = {
        val maxHeight = when (type) {
            Default -> Dp.Unspecified
            Modal -> windowSize.height * MODAL_SHEET_MAX_HEIGHT
        }

        val contentModifier = when (type) {
            Default -> Modifier.clip(
                RoundedCornerShape(
                    topStart = TangemTheme.dimens2.x8,
                    topEnd = TangemTheme.dimens2.x8,
                ),
            )
            Modal -> Modifier
                .padding(
                    start = TangemTheme.dimens2.x2,
                    end = TangemTheme.dimens2.x2,
                    bottom = TangemTheme.dimens2.x2,
                )
                .clip(RoundedCornerShape(TangemTheme.dimens2.x8))
        }

        Column(
            modifier = contentModifier
                .background(containerColor)
                .heightIn(max = maxHeight),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                title(model)
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                content(model)
                if (footer != null) {
                    BottomFade(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        gradientBrush = Brush.verticalGradient(
                            listOf(
                                TangemTheme.colors2.shadow.fadeMin,
                                TangemTheme.colors2.shadow.fadeMax,
                            ),
                        ),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                ) {
                    if (footer != null) {
                        footer(model)
                    }
                }
            }
        }
    }

    InternalBottomSheet(
        modifier = modifier.statusBarsPadding(),
        onDismissRequest = config.onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentWindowInsets = { WindowInsetsZero },
        onBack = onBack,
        dragHandle = type.getDragHandle(),
        content = bsContent,
        scrimColor = TangemTheme.colors2.overlay.overlaySecondary,
    )
}

// region Preview
@Suppress("LongMethod")
@Composable
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TangemModalBottomSheetWithFooter_Preview(
    @PreviewParameter(TangemBottomSheetPreviewProvider::class) params: TangemBottomSheetType,
) {
    TangemThemePreviewRedesign {
        TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = TangemBottomSheetConfigContent.Empty,
            ),
            type = params,
            title = { TangemModalBottomSheetTitle(endIconRes = R.drawable.ic_close_24, onEndClick = {}) },
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
                    Text(
                        text = "Long text to show scrollable content and bottom fade." +
                            "\nLorem ipsum dolor sit amet, consectetur adipiscing elit. In imperdiet metus non leo " +
                            "ultricies pulvinar. Pellentesque sed condimentum odio. Sed venenatis ac felis non " +
                            "consequat. Nunc erat dolor, maximus nec mattis a, tempus at eros. Duis sit amet neque " +
                            "dui. Donec consectetur nisl id dui convallis, in posuere dolor eleifend. Pellentesque " +
                            "habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. " +
                            "Pellentesque consequat scelerisque justo quis tristique. Mauris laoreet venenatis " +
                            "pharetra. Morbi sed faucibus leo. Praesent elementum pretium posuere. Morbi et felis a " +
                            "turpis pellentesque rhoncus.",
                        style = TangemTheme.typography.body1,
                        color = TangemTheme.colors.text.secondary,
                        textAlign = TextAlign.Center,
                    )
                }
            },
            footer = {
                PrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    text = "Go it",
                    onClick = {},
                )
            },
        )
    }
}

@Suppress("LongMethod")
@Composable
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TangemModalBottomSheetWithFooter_Preview2(
    @PreviewParameter(TangemBottomSheetPreviewProvider::class) params: TangemBottomSheetType,
) {
    TangemThemePreviewRedesign {
        TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = TangemBottomSheetConfigContent.Empty,
            ),
            type = params,
            title = { TangemModalBottomSheetTitle(endIconRes = R.drawable.ic_close_24, onEndClick = {}) },
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
                    Text(
                        text = "Long text to show scrollable content and bottom fade." +
                            "\nLorem ipsum dolor sit amet, consectetur adipiscing elit. In imperdiet metus non leo " +
                            "ultricies pulvinar. Pellentesque sed condimentum odio. Sed venenatis ac felis non " +
                            "consequat. Nunc erat dolor, maximus nec mattis a, tempus at eros. Duis sit amet neque " +
                            "dui. Donec consectetur nisl id dui convallis, in posuere dolor eleifend. Pellentesque " +
                            "habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. " +
                            "Pellentesque consequat scelerisque justo quis tristique. Mauris laoreet venenatis " +
                            "pharetra. Morbi sed faucibus leo. Praesent elementum pretium posuere. Morbi et felis a " +
                            "turpis pellentesque rhoncus.",
                        style = TangemTheme.typography.body1,
                        color = TangemTheme.colors.text.secondary,
                        textAlign = TextAlign.Center,
                    )
                }
            },
        )
    }
}

private class TangemBottomSheetPreviewProvider : PreviewParameterProvider<TangemBottomSheetType> {
    override val values: Sequence<TangemBottomSheetType>
        get() = sequenceOf(
            Default,
            Modal,
        )
}
// endregion