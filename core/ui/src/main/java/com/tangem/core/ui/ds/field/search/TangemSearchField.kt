package com.tangem.core.ui.ds.field.search

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.R
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.ds.button.GhostTangemButton
import com.tangem.core.ui.ds.button.TangemButtonSize
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.BaseSearchBarTestTags
import com.tangem.core.ui.test.SearchBarTestTags

/**
 * Shape options for [TangemSearchField].
 */
enum class TangemFieldShape {
    RoundedCorners,
    Circle,
    ;

    @ReadOnlyComposable
    @Composable
    internal fun toShape(): Shape {
        return when (this) {
            RoundedCorners -> RoundedCornerShape(TangemTheme.dimens2.x4)
            Circle -> CircleShape
        }
    }
}

/**
 * Custom search field component that provides a user-friendly interface for searching and filtering content.
 *
 * [Figma](https://www.figma.com/design/RU7AIgwHtGdMfy83T5UOoR/Core-Library?node-id=8445-19217&m=dev)
 *
 * @param state The state of the search field, including the current query, placeholder text, and active status.
 * @param shape The shape of the search field, which can be either rounded corners or a circle.
 * @param modifier The modifier to be applied to the search field.
 * @param enabled Whether the search field is enabled or not.
 * @param focusRequester The [FocusRequester] used to request focus on the search field when it becomes active.
 */
@Composable
fun TangemSearchField(
    state: SearchBarUM,
    shape: TangemFieldShape,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    var isInitialComposition by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        isInitialComposition = false
    }
    BasicTextField(
        modifier = modifier
            .heightIn(min = TangemTheme.dimens2.x11)
            .onFocusChanged { focusState ->
                if (!isInitialComposition) {
                    if (focusState.isFocused) {
                        state.onActiveChange(true)
                    } else {
                        state.onActiveChange(false)
                    }
                }
            }
            .focusRequester(focusRequester)
            .testTag(BaseSearchBarTestTags.SEARCH_BAR),
        enabled = enabled,
        value = state.query,
        onValueChange = state.onQueryChange,
        interactionSource = interactionSource,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search,
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                keyboardController?.hide()
                focusManager.clearFocus()
            },
        ),
        singleLine = true,
        maxLines = 1,
        textStyle = TangemTheme.typography2.bodySemibold16.copy(
            color = TangemTheme.colors2.text.neutral.primary,
        ),
        cursorBrush = SolidColor(TangemTheme.colors2.graphic.neutral.primary),
        decorationBox = @Composable { innerTextField ->
            DecorationBox(
                state = state,
                shape = shape,
                innerTextField = innerTextField,
                focusManager = focusManager,
                interactionSource = interactionSource,
                color = TangemTheme.colors2.button.backgroundSecondary,
                keyboardController = keyboardController,
            )
        },
    )
}

@Suppress("LongParameterList")
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DecorationBox(
    state: SearchBarUM,
    shape: TangemFieldShape,
    focusManager: FocusManager,
    interactionSource: MutableInteractionSource,
    color: Color,
    keyboardController: SoftwareKeyboardController?,
    innerTextField: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        val isFocused by interactionSource.collectIsFocusedAsState()
        val alignmentBias by animateFloatAsState(
            targetValue = if (isFocused) -1f else 0f,
            animationSpec = tween(),
            label = "AlignmentBias",
        )

        Box(
            contentAlignment = BiasAlignment(horizontalBias = alignmentBias, verticalBias = 0f),
            modifier = Modifier
                .weight(1f)
                .background(color, shape.toShape())
                .padding(TangemTheme.dimens2.x3),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
            ) {
                Icon(
                    modifier = Modifier
                        .size(TangemTheme.dimens2.x5)
                        .testTag(SearchBarTestTags.ICON),
                    painter = painterResource(id = R.drawable.ic_search_default_24),
                    tint = TangemTheme.colors2.graphic.neutral.tertiaryConstant,
                    contentDescription = null,
                )
                Field(
                    state = state,
                    innerTextField = innerTextField,
                )
                ClearButton(
                    state = state,
                )
            }
        }
        CancelButton(
            state = state,
            keyboardController = keyboardController,
            focusManager = focusManager,
            isActive = isFocused,
        )
    }
}

@Composable
private fun Field(state: SearchBarUM, innerTextField: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .heightIn(min = TangemTheme.dimens2.x5)
            .width(IntrinsicSize.Max),
    ) {
        innerTextField()

        val placeholderOpacity by remember(state.query) {
            derivedStateOf {
                if (state.query.isNotEmpty()) {
                    0f
                } else {
                    1f
                }
            }
        }

        Text(
            text = state.placeholderText.resolveReference(),
            color = TangemTheme.colors2.text.neutral.tertiary,
            style = TangemTheme.typography2.bodySemibold16,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .testTag(SearchBarTestTags.PLACEHOLDER_TEXT)
                .alpha(placeholderOpacity),
        )
    }
}

@Composable
private fun ClearButton(state: SearchBarUM) {
    if (state.query.isNotEmpty()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_close_new_20),
                tint = TangemTheme.colors2.graphic.neutral.tertiary,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(TangemTheme.dimens2.x5)
                    .clip(CircleShape)
                    .clickable(
                        onClick = state.onClearClick,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false),
                    )
                    .testTag(SearchBarTestTags.CLEAR_BUTTON),
            )
        }
    }
}

@Composable
private fun CancelButton(
    state: SearchBarUM,
    isActive: Boolean,
    focusManager: FocusManager,
    keyboardController: SoftwareKeyboardController?,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isActive,
        enter = expandHorizontally(),
        exit = shrinkHorizontally(),
        modifier = modifier,
    ) {
        GhostTangemButton(
            text = resourceReference(R.string.common_cancel),
            size = TangemButtonSize.X9,
            modifier = Modifier.padding(start = TangemTheme.dimens2.x3),
            onClick = {
                if (state.query.isNotEmpty()) {
                    state.onQueryChange("")
                }
                keyboardController?.hide()
                state.onClearClick()
                focusManager.clearFocus()
            },
        )
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TangemSearchField_Preview(@PreviewParameter(TangemSearchFieldPreviewProvider::class) params: SearchBarUM) {
    var state by remember { mutableStateOf(params) }
    TangemThemePreviewRedesign {
        Column(
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x4),
            modifier = Modifier
                .background(TangemTheme.colors2.surface.level1)
                .padding(TangemTheme.dimens2.x4),
        ) {
            TangemFieldShape.entries.fastForEach { shape ->
                TangemSearchField(
                    state = state.copy(
                        onActiveChange = { state = state.copy(isActive = it) },
                        onQueryChange = { state = state.copy(query = it) },
                        onClearClick = { state = state.copy(query = "") },
                    ),
                    shape = shape,
                )
            }
        }
    }
}

private class TangemSearchFieldPreviewProvider : PreviewParameterProvider<SearchBarUM> {
    override val values: Sequence<SearchBarUM>
        get() = sequenceOf(
            SearchBarUM(
                placeholderText = stringReference("Crypto, news and more"),
                query = "BTC",
                onQueryChange = {},
                isActive = true,
                onActiveChange = {},
            ),
            SearchBarUM(
                placeholderText = stringReference("Crypto, news and more"),
                query = "",
                onQueryChange = {},
                isActive = false,
                onActiveChange = {},
            ),
        )
}
// endregion