package com.tangem.core.ui.components.fields

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.R
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Composable
fun SearchBar(state: SearchBarUM, modifier: Modifier = Modifier, colors: TextFieldColors = TangemSearchBarColors) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    TextField(
        modifier = modifier
            .fillMaxWidth()
            .height(TangemTheme.dimens.size48)
            .onFocusChanged {
                if (it.isFocused) {
                    state.onActiveChange(true)
                } else {
                    state.onActiveChange(false)
                }
            },
        value = state.query,
        onValueChange = state.onQueryChange,
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
        singleLine = true,
        maxLines = 1,
        textStyle = TangemTheme.typography.body2,
        leadingIcon = {
            Icon(
                modifier = Modifier.size(TangemTheme.dimens.size20),
                painter = painterResource(id = R.drawable.ic_search_24),
                tint = TangemTheme.colors.icon.informative,
                contentDescription = null,
            )
        },
        trailingIcon = {
            ClearButton(
                state = state,
                focusManager = focusManager,
                keyboardController = keyboardController,
            )
        },
        placeholder = {
            Text(
                text = state.placeholderText.resolveReference(),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.body2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        shape = TangemTheme.shapes.roundedCornersXLarge,
        colors = colors,
    )
}

@Composable
private fun ClearButton(
    state: SearchBarUM,
    focusManager: FocusManager,
    keyboardController: SoftwareKeyboardController?,
    modifier: Modifier = Modifier,
) {
    if (state.query.isNotEmpty() || state.isActive) {
        IconButton(
            modifier = modifier,
            onClick = {
                if (state.query.isNotEmpty()) {
                    state.onQueryChange("")
                }
                focusManager.clearFocus()
                keyboardController?.hide()
                state.onActiveChange(false)
            },
        ) {
            Icon(
                modifier = Modifier.size(TangemTheme.dimens.size20),
                painter = painterResource(id = R.drawable.ic_close_24),
                tint = TangemTheme.colors.icon.informative,
                contentDescription = null,
            )
        }
    }
}

private val TangemSearchBarColors: TextFieldColors
    @Composable
    get() = TextFieldDefaults.colors().copy(
        focusedContainerColor = TangemTheme.colors.field.primary,
        unfocusedContainerColor = TangemTheme.colors.field.primary,
        focusedTextColor = TangemTheme.colors.text.primary1,
        unfocusedTextColor = TangemTheme.colors.text.primary1,
        cursorColor = TangemTheme.colors.icon.primary1,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
    )

// region Previews
@Preview(widthDp = 328)
@Preview(widthDp = 328, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TokensSearchBar(@PreviewParameter(SearchBarkConfigProvider::class) state: SearchBarUM) {
    TangemThemePreview {
        SearchBar(state)
    }
}

private class SearchBarkConfigProvider : CollectionPreviewParameterProvider<SearchBarUM>(
    collection = listOf(
        SearchBarUM(
            placeholderText = resourceReference(R.string.manage_tokens_search_placeholder),
            query = "BTC",
            onQueryChange = {},
            isActive = true,
            onActiveChange = {},
        ),
        SearchBarUM(
            placeholderText = resourceReference(R.string.manage_tokens_search_placeholder),
            query = "",
            onQueryChange = {},
            isActive = false,
            onActiveChange = {},
        ),
    ),
)
// endregion