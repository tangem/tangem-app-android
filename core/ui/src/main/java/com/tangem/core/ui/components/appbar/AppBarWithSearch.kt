package com.tangem.core.ui.components.appbar

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.TangemTextFieldsDefault
import com.tangem.core.ui.res.TangemTheme

/**
 * App bar with close icon and search functionality
 *
 * @param title optional title
 * @param placeholderSearchText optional text placeholder for search TextField
 * @param expandedInitially whether the search is expanded on launch
 * @param tint tint for most of the visual elements of toolbar
 * @param onBackClick action when close button is clicked
 * @param onSearchChange action when search is modified
 * @param onSearchDisplayClose action when search is closed
 * @param subtitle additional text under the title (optional)
 * @param icon additional icon under the title (optional)
 *
 * @see <a href =
 * "https://www.figma.com/file/Vs6SkVsFnUPsSCNwlnVf5U/Android-%E2%80%93-UI?node-id=1123%3A4068&t=xj8BBj5DfCWn2Mli-1"
 * >Figma component</a>
 */
@Composable
fun ExpandableSearchView(
    onBackClick: () -> Unit,
    onSearchChange: (String) -> Unit,
    onSearchDisplayClose: () -> Unit,
    title: String? = null,
    placeholderSearchText: String = "",
    expandedInitially: Boolean = false,
    tint: Color = TangemTheme.colors.icon.primary1,
    subtitle: String? = null,
    icon: Painter? = null,
) {
    val (expanded, onExpandedChanged) = remember { mutableStateOf(expandedInitially) }

    Crossfade(targetState = expanded) { isSearchFieldVisible ->
        if (isSearchFieldVisible) {
            ExpandedSearchView(
                placeholderSearchText = placeholderSearchText,
                onSearchChange = onSearchChange,
                onSearchDisplayClose = onSearchDisplayClose,
                onExpandedChange = onExpandedChanged,
                tint = tint,
            )
        } else {
            CollapsedSearchView(
                title = title,
                onBackClick = onBackClick,
                onExpandedChange = onExpandedChanged,
                tint = tint,
                subtitle = subtitle,
                icon = icon,
            )
        }
    }
}

@Composable
private fun CollapsedSearchView(
    tint: Color,
    onBackClick: () -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    title: String? = null,
    subtitle: String? = null,
    icon: Painter? = null,
) {
    Row(
        modifier = Modifier
            .background(TangemTheme.colors.background.secondary)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_close_24),
            contentDescription = null,
            modifier = Modifier
                .padding(TangemTheme.dimens.spacing16)
                .size(TangemTheme.dimens.size24)
                .clickable { onBackClick() },
            tint = MaterialTheme.colors.onPrimary,
        )
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    color = TangemTheme.colors.text.primary1,
                    maxLines = 1,
                    style = TangemTheme.typography.subtitle1,
                    modifier = Modifier.defaultMinSize(minHeight = TangemTheme.dimens.size24),
                )
            }
            if (subtitle != null) {
                SubtitleView(subtitle = subtitle, icon = icon)
            }
        }

        SpacerWMax()
        Icon(
            painter = painterResource(id = R.drawable.ic_search_24),
            tint = tint,
            contentDescription = null,
            modifier = Modifier
                .clickable { onExpandedChange(true) }
                .padding(end = TangemTheme.dimens.spacing16),
        )
    }
}

@Composable
private fun SubtitleView(subtitle: String, icon: Painter?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Image(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(TangemTheme.dimens.size16),
            )
        }
        Text(
            text = subtitle,
            color = TangemTheme.colors.text.secondary,
            maxLines = 1,
            style = TangemTheme.typography.caption,
        )
    }
}

@Composable
private fun ExpandedSearchView(
    placeholderSearchText: String,
    onSearchChange: (String) -> Unit,
    onSearchDisplayClose: () -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    tint: Color,
) {
    val focusManager = LocalFocusManager.current
    val textFieldFocusRequester = remember { FocusRequester() }

    SideEffect {
        textFieldFocusRequester.requestFocus()
    }

    var textFieldValue by remember { mutableStateOf(TextFieldValue("", TextRange("".length))) }

    Row(
        modifier = Modifier
            .background(TangemTheme.colors.background.secondary)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = {
                onExpandedChange(false)
                onSearchDisplayClose()
            },
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_back_24),
                contentDescription = null,
                tint = tint,
            )
        }
        TextField(
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it
                onSearchChange(it.text)
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(textFieldFocusRequester),
            placeholder = {
                Text(text = placeholderSearchText)
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() },
            ),
            colors = TangemTextFieldsDefault.defaultTextFieldColors.copy(
                placeholderColor = TangemTheme.colors.text.disabled,
                cursorColor = TangemTheme.colors.text.tertiary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
        )
    }
}

@Preview
@Composable
private fun CollapsedSearchViewPreview() {
    TangemTheme {
        ExpandableSearchView(
            title = "Choose Token",
            onBackClick = {},
            placeholderSearchText = "Search",
            onSearchChange = {},
            onSearchDisplayClose = {},
            subtitle = "Ethereum",
            icon = painterResource(id = R.drawable.img_eth_22),
        )
    }
}

@Preview
@Composable
private fun ExpandedSearchViewPreview() {
    TangemTheme {
        ExpandableSearchView(
            title = "Choose Token",
            onBackClick = {},
            placeholderSearchText = "Search",
            onSearchChange = {},
            expandedInitially = true,
            onSearchDisplayClose = {},
        )
    }
}
