package com.tangem.core.ui.components.bottomsheets.modal

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.internal.ModalBottomSheetWithBackHandling
import com.tangem.core.ui.components.bottomsheets.internal.collapse
import com.tangem.core.ui.res.LocalBottomSheetAlwaysVisible
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.WindowInsetsZero
import com.tangem.core.ui.utils.toPx

/**
 * Modal bottom sheet with [content], [footer] and optional [title].
 *
 * Maximum height of sheet is 80% screen height
 *
 * [Show in Figma](https://www.figma.com/design/09KKG4ZVuFDZhj8WLv5rGJ/%F0%9F%9A%A7-App-experience?node-id=3254-61208&t=vixa7id6ggALcxfF-4)
 */
@Composable
inline fun <reified T : TangemBottomSheetConfigContent> TangemModalBottomSheetWithFooter(
    config: TangemBottomSheetConfig,
    containerColor: Color = TangemTheme.colors.background.primary,
    skipPartiallyExpanded: Boolean = true,
    noinline onBack: (() -> Unit)? = null,
    crossinline title: @Composable BoxScope.(T) -> Unit = {},
    crossinline content: @Composable (T) -> Unit,
    noinline footer: @Composable (BoxScope.(T) -> Unit)?,
) {
    val isAlwaysVisible = LocalBottomSheetAlwaysVisible.current

    if (isAlwaysVisible) {
        PreviewModalBottomSheetWithFooter<T>(
            config = config,
            containerColor = containerColor,
            title = title,
            content = content,
            footer = footer,
            skipPartiallyExpanded = skipPartiallyExpanded,
        )
    } else {
        DefaultModalBottomSheetWithFooter<T>(
            config = config,
            containerColor = containerColor,
            title = title,
            content = content,
            footer = footer,
            onBack = onBack,
            skipPartiallyExpanded = skipPartiallyExpanded,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
inline fun <reified T : TangemBottomSheetConfigContent> DefaultModalBottomSheetWithFooter(
    config: TangemBottomSheetConfig,
    containerColor: Color,
    skipPartiallyExpanded: Boolean = true,
    noinline onBack: (() -> Unit)? = null,
    crossinline title: @Composable BoxScope.(T) -> Unit,
    crossinline content: @Composable (T) -> Unit,
    noinline footer: @Composable (BoxScope.(T) -> Unit)?,
) {
    var isVisible by remember { mutableStateOf(value = config.isShown) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)

    if (isVisible && config.content is T) {
        BasicModalBottomSheetWithFooter<T>(
            config = config,
            sheetState = sheetState,
            containerColor = containerColor,
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
inline fun <reified T : TangemBottomSheetConfigContent> PreviewModalBottomSheetWithFooter(
    config: TangemBottomSheetConfig,
    containerColor: Color,
    skipPartiallyExpanded: Boolean = true,
    crossinline title: @Composable BoxScope.(T) -> Unit,
    crossinline content: @Composable (T) -> Unit,
    noinline footer: @Composable (BoxScope.(T) -> Unit)?,
) {
    BasicModalBottomSheetWithFooter<T>(
        config = config,
        sheetState = SheetState(
            skipPartiallyExpanded = skipPartiallyExpanded,
            initialValue = Expanded,
            density = LocalDensity.current,
        ),
        onBack = null,
        containerColor = containerColor,
        title = title,
        content = content,
        footer = footer,
    )
}

@Suppress("LongParameterList", "LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
inline fun <reified T : TangemBottomSheetConfigContent> BasicModalBottomSheetWithFooter(
    config: TangemBottomSheetConfig,
    sheetState: SheetState,
    containerColor: Color,
    modifier: Modifier = Modifier,
    noinline onBack: (() -> Unit)? = null,
    crossinline title: @Composable BoxScope.(T) -> Unit,
    crossinline content: @Composable (T) -> Unit,
    noinline footer: @Composable (BoxScope.(T) -> Unit)?,
) {
    val model = config.content as? T ?: return

    val bsContent: @Composable ColumnScope.() -> Unit = {
        val maxHeight = LocalConfiguration.current.screenHeightDp * MODAL_SHEET_MAX_HEIGHT
        val initial = 0
        val scrollState = rememberScrollState(initial = initial)

        val isKeyboardOpen by rememberIsKeyboardVisible()
        val buttonHeight by animateDpAsState(
            if (footer != null) {
                80.dp
            } else {
                0.dp
            },
        )
        // Offset calculation for keyboard scroll adjustment:
        // 1) Button height (footer)
        // 2) Column content bottom padding
        // 3) Additional spacing (40dp) for visual comfort when keyboard is open
        val scrollOffset = buttonHeight.toPx() + buttonHeight.toPx() + 40.dp.toPx()

        LaunchedEffect(isKeyboardOpen) {
            if (isKeyboardOpen) {
                scrollState.animateScrollTo(scrollState.value + scrollOffset.toInt())
            }
        }

        Column(
            modifier = Modifier
                .systemBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .clip(TangemTheme.shapes.roundedCornersLarge)
                .background(containerColor)
                .heightIn(max = maxHeight.dp)
                .fillMaxWidth(),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                title(model)
            }
            // Title bottom shadow(elevation) while content is scrolling
            if (scrollState.value != initial) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    TangemTheme.colors.background.secondary.copy(alpha = 0.9F),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
            }
            Box(modifier = Modifier.weight(1f, fill = false)) {
                Column(
                    modifier = Modifier
                        .verticalScroll(state = scrollState)
                        .padding(bottom = buttonHeight),
                ) {
                    content(model)
                }
                if (scrollState.canScrollForward && scrollState.maxValue != Int.MAX_VALUE) {
                    BottomFade(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        backgroundColor = TangemTheme.colors.background.primary,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight)
                        .align(Alignment.BottomCenter),
                ) {
                    if (footer != null) {
                        footer(model)
                    }
                }
            }
        }
    }

    if (onBack != null) {
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
            scrimColor = TangemTheme.colors.overlay.secondary,
        )
    } else {
        ModalBottomSheet(
            modifier = modifier,
            onDismissRequest = config.onDismissRequest,
            sheetState = sheetState,
            containerColor = Color.Transparent,
            shape = TangemTheme.shapes.roundedCornersLarge,
            contentWindowInsets = { WindowInsetsZero },
            dragHandle = null,
            content = bsContent,
            scrimColor = TangemTheme.colors.overlay.secondary,
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemModalBottomSheetWithFooter_Preview() {
    TangemThemePreview {
        TangemModalBottomSheetWithFooter<TangemBottomSheetConfigContent.Empty>(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = TangemBottomSheetConfigContent.Empty,
            ),
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