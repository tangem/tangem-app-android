package com.tangem.managetokens.presentation.managetokens.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.managetokens.impl.R
import com.tangem.managetokens.presentation.managetokens.state.SearchBarState

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun TokensSearchBar(state: SearchBarState, modifier: Modifier = Modifier) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    TextField(
        value = state.query,
        onValueChange = state.onQueryChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                keyboardController?.hide()
                focusManager.clearFocus()
            },
        ),
        singleLine = true,
        maxLines = 1,
        textStyle = TangemTheme.typography.body2.copy(
            color = TangemTheme.colors.text.primary1,
        ),
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_search_24),
                tint = TangemTheme.colors.icon.informative,
                contentDescription = null,
                modifier = Modifier.clickable { state.onActiveChange(true) },
            )
        },
        trailingIcon = {
            if (state.query.isNotEmpty() || state.active) {
                IconButton(
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
                        painter = painterResource(id = R.drawable.ic_close),
                        tint = TangemTheme.colors.icon.informative,
                        contentDescription = null,
                    )
                }
            }
        },
        placeholder = {
            Text(
                text = stringResource(R.string.manage_tokens_search_placeholder),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.body2,
            )
        },
        shape = RoundedCornerShape(TangemTheme.dimens.radius36),
        colors = searchbarTextFieldColors(),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged {
                if (it.isFocused) {
                    state.onActiveChange(true)
                } else {
                    state.onActiveChange(false)
                }
            },
    )
}

@Composable
private fun searchbarTextFieldColors(): TextFieldColors {
    return TextFieldDefaults.textFieldColors(
        backgroundColor = TangemTheme.colors.field.primary,
        textColor = TangemTheme.colors.text.primary1,
        cursorColor = TangemTheme.colors.icon.primary1,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TokensSearchBar(
    @PreviewParameter(SearchBarkConfigProvider::class)
    state: SearchBarState,
) {
    TangemThemePreview {
        TokensSearchBar(state)
    }
}

private class SearchBarkConfigProvider : CollectionPreviewParameterProvider<SearchBarState>(
    collection = listOf(
        SearchBarState(
            query = "BTC",
            onQueryChange = {},
            active = true,
            onActiveChange = {},
        ),
        SearchBarState(
            query = "",
            onQueryChange = {},
            active = false,
            onActiveChange = {},
        ),
    ),
)