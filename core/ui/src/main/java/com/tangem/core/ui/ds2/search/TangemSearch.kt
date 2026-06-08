package com.tangem.core.ui.ds2.search

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_cross_20
import com.tangem.core.ui.res.generated.icons.ic_cross_circle_20_filled
import com.tangem.core.ui.res.generated.icons.ic_search_20

/**
 * Search component (DS V3)
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=3953-249&m=dev)
 *
 * @param state Hoisted state holding the query, active flag, placeholder, and callbacks.
 *   When [TangemSearch.State.onCloseClick] is `null`, the trailing close button is omitted.
 * @param modifier Modifier applied to the outer row container.
 * @param focusRequester Focus requester wired to the text field. Pass a hoisted instance to
 *   programmatically focus the field from the caller (e.g., on screen entry). Tapping anywhere on
 *   the surface also invokes [FocusRequester.requestFocus] on this instance.
 */
@Composable
fun TangemSearch(
    state: TangemSearch.State,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val openingEasing = CubicBezierEasing(a = 0.8f, b = 0f, c = 0.2f, d = 1f)
    val closingEasing = CubicBezierEasing(a = 0.8f, b = 0f, c = 0.8f, d = 1f)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TangemSurface(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = TangemTheme.dimens3.size.s550),
            isMaterial = true,
            shape = CircleShape,
            onClick = focusRequester::requestFocus,
        ) {
            SearchField(state = state, focusRequester = focusRequester)
        }

        AnimatedVisibility(
            visible = state.onCloseClick != null && (state.isActive || state.query.isNotEmpty()),
            enter = expandHorizontally(animationSpec = tween(durationMillis = 150, easing = openingEasing)) +
                fadeIn(animationSpec = tween(durationMillis = 150, delayMillis = 150, easing = openingEasing)) +
                scaleIn(animationSpec = tween(durationMillis = 150, delayMillis = 150, easing = openingEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 150, easing = closingEasing)) +
                scaleOut(animationSpec = tween(durationMillis = 150, easing = closingEasing)) +
                shrinkHorizontally(animationSpec = tween(durationMillis = 150, easing = closingEasing)),
        ) {
            CloseButton(onClick = state.onCloseClick ?: {})
        }
    }
}

object TangemSearch {

    @Immutable
    data class State(
        val placeholderText: TextReference,
        val query: String,
        val onQueryChange: (String) -> Unit,
        val isActive: Boolean,
        val onActiveChange: (Boolean) -> Unit,
        val onClearClick: () -> Unit = { },
        val onCloseClick: (() -> Unit)? = null,
    )
}

@Composable
private fun SearchField(state: TangemSearch.State, focusRequester: FocusRequester) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = TangemTheme.dimens3.spacing.s150,
                    top = TangemTheme.dimens3.spacing.s150,
                    bottom = TangemTheme.dimens3.spacing.s150,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.padding(end = TangemTheme.dimens3.spacing.s100),
                imageVector = Icons.ic_search_20,
                tint = TangemTheme.colors3.icon.primary,
                contentDescription = null,
            )
            QueryTextField(state = state, focusRequester = focusRequester)
        }

        AnimatedVisibility(
            visible = state.query.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ClearButton(
                onClick = {
                    state.onQueryChange("")
                    state.onClearClick()
                },
            )
        }
    }
}

@Composable
private fun QueryTextField(state: TangemSearch.State, focusRequester: FocusRequester) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    // Initial composition reports isFocused = false. Without this guard the field would
    // clobber a parent-supplied `state.isActive = true` on the first frame.
    var isInitialComposition by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { isInitialComposition = false }

    val placeholder = state.placeholderText.resolveReference()
    val sharedTextStyle = TangemTheme.typography3.body.medium.copy(
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
    BasicTextField(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .semantics { contentDescription = placeholder }
            .onFocusChanged { focusState ->
                if (!isInitialComposition) {
                    state.onActiveChange(focusState.isFocused)
                }
            },
        value = state.query,
        onValueChange = state.onQueryChange,
        singleLine = true,
        textStyle = sharedTextStyle.copy(color = TangemTheme.colors3.text.primary),
        cursorBrush = SolidColor(TangemTheme.colors3.icon.brand),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search,
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                keyboardController?.hide()
                focusManager.clearFocus()
            },
        ),
        decorationBox = { innerTextField ->
            Box {
                if (state.query.isEmpty()) {
                    Text(
                        modifier = Modifier.padding(end = TangemTheme.dimens3.spacing.s250),
                        text = placeholder,
                        style = sharedTextStyle,
                        color = TangemTheme.colors3.text.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun ClearButton(onClick: () -> Unit) {
    TangemButton(
        modifier = Modifier.padding(end = TangemTheme.dimens3.spacing.s050),
        size = TangemButton.Size.X9,
        variant = TangemButton.Variant.Ghost,
        iconStart = TangemIconUM.Icon(Icons.ic_cross_circle_20_filled),
        onClick = onClick,
    )
}

@Composable
private fun CloseButton(onClick: () -> Unit) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    TangemButton(
        modifier = Modifier.padding(start = TangemTheme.dimens3.spacing.s100),
        size = TangemButton.Size.X11,
        variant = TangemButton.Variant.Material,
        iconStart = TangemIconUM.Icon(Icons.ic_cross_20),
        onClick = {
            keyboardController?.hide()
            focusManager.clearFocus()
            onClick()
        },
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview(@PreviewParameter(TangemSearchStateProvider::class) state: TangemSearch.State) {
    TangemThemePreviewRedesign {
        Box(modifier = Modifier.background(TangemTheme.colors3.bg.secondary)) {
            TangemSearch(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}

private class TangemSearchStateProvider : CollectionPreviewParameterProvider<TangemSearch.State>(
    collection = listOf(
        // Idle: empty + inactive — no clear, no close.
        TangemSearch.State(
            placeholderText = stringReference("Search"),
            query = "",
            onQueryChange = {},
            isActive = false,
            onActiveChange = {},
            onCloseClick = {},
        ),
        // Focused empty: active + empty — close visible, clear hidden.
        TangemSearch.State(
            placeholderText = stringReference("Search"),
            query = "",
            onQueryChange = {},
            isActive = true,
            onActiveChange = {},
            onCloseClick = {},
        ),
        // Typing: active + query — clear and close both visible.
        TangemSearch.State(
            placeholderText = stringReference("Search"),
            query = "Bitcoin",
            onQueryChange = {},
            isActive = true,
            onActiveChange = {},
            onClearClick = {},
            onCloseClick = {},
        ),
        // Filled but blurred: query persists without focus — close stays visible.
        TangemSearch.State(
            placeholderText = stringReference("Search"),
            query = "Ethereum",
            onQueryChange = {},
            isActive = false,
            onActiveChange = {},
            onClearClick = {},
            onCloseClick = {},
        ),
        // Embedded variant: no close affordance at all (onCloseClick = null).
        TangemSearch.State(
            placeholderText = stringReference("Filter tokens"),
            query = "USDT",
            onQueryChange = {},
            isActive = true,
            onActiveChange = {},
            onClearClick = {},
            onCloseClick = null,
        ),
        // Long query that exercises clipping inside the surface.
        TangemSearch.State(
            placeholderText = stringReference("Search"),
            query = "A very long search query that should clip nicely",
            onQueryChange = {},
            isActive = true,
            onActiveChange = {},
            onClearClick = {},
            onCloseClick = {},
        ),
    ),
)