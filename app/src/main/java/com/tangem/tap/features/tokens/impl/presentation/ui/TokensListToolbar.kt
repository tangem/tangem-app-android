package com.tangem.tap.features.tokens.impl.presentation.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.tap.features.tokens.impl.presentation.states.TokensListToolbarState
import com.tangem.tap.features.tokens.impl.presentation.states.TokensListToolbarState.InputField
import com.tangem.tap.features.tokens.impl.presentation.states.TokensListToolbarState.Title
import com.tangem.wallet.R
import kotlinx.coroutines.delay

/**
* [REDACTED_AUTHOR]
 */
@Composable
internal fun TokensListToolbar(state: TokensListToolbarState) {
    val toolbarElevation = if (isSystemInDarkTheme()) {
        TangemTheme.dimens.elevation0
    } else {
        AppBarDefaults.TopAppBarElevation
    }
    val statusBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getTop(this).toDp() }

    TopAppBar(
        backgroundColor = TangemTheme.colors.background.secondary,
        contentPadding = PaddingValues(
            start = TangemTheme.dimens.spacing4,
            end = TangemTheme.dimens.spacing4,
            top = statusBarHeight,
        ),
        elevation = toolbarElevation,
    ) {
        IconButton(onClick = state.onBackButtonClick) {
            Icon(
                painter = painterResource(id = R.drawable.ic_back_24),
                contentDescription = "Go back",
                tint = TangemTheme.colors.icon.primary1,
            )
        }

        when (state) {
            is Title -> TitleContent(state = state, modifier = Modifier.weight(1f))
            is InputField -> InputContent(state = state, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun TitleContent(state: Title, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(id = state.titleResId),
        modifier = modifier.padding(horizontal = TangemTheme.dimens.spacing26),
        color = TangemTheme.colors.text.primary1,
        maxLines = 1,
        style = TangemTheme.typography.h3,
    )

    IconButton(onClick = state.onSearchButtonClick) {
        Icon(
            painter = painterResource(id = R.drawable.ic_search_24),
            contentDescription = "Search",
            tint = TangemTheme.colors.icon.primary1,
        )
    }

    if (state is Title.Manage) {
        IconButton(onClick = state.onAddCustomTokenClick) {
            Icon(
                painter = painterResource(id = R.drawable.ic_plus_24),
                contentDescription = "Add custom token",
                tint = TangemTheme.colors.icon.primary1,
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun InputContent(state: InputField, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    BasicTextField(
        value = state.value,
        onValueChange = state.onValueChange,
        modifier = modifier.focusRequester(focusRequester),
        textStyle = TangemTheme.typography.subtitle1.copy(
            fontWeight = FontWeight.Normal,
            color = TangemTheme.colors.text.primary1,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
        singleLine = true,
        maxLines = 1,
        cursorBrush = SolidColor(value = TangemTheme.colors.stroke.secondary),
    ) { innerTextField ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = TangemTheme.dimens.spacing28,
                        top = TangemTheme.dimens.spacing2,
                        end = TangemTheme.dimens.spacing16,
                    ),
            ) {
                Hint(value = state.value)
                innerTextField()
            }

            IconButton(onClick = state.onCleanButtonClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_cross_rounded_24),
                    contentDescription = "Clean entered value",
                    tint = TangemTheme.colors.icon.secondary,
                )
            }
        }
    }

    // This is a hack because Compose can't show the keyboard if the input field is not found.
    // So first I request focus and make a delay and then show the keyboard.
    val keyboardManager = LocalSoftwareKeyboardController.current
    LaunchedEffect(keyboardManager) {
        focusRequester.requestFocus()
        delay(timeMillis = 100)
        keyboardManager?.show()
    }
}

@Composable
private fun Hint(value: String) {
    if (value.isBlank()) {
        Row(
            modifier = Modifier.padding(start = TangemTheme.dimens.spacing4),
            horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_search_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.secondary,
            )
            Text(
                text = stringResource(id = R.string.common_search),
                modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing2),
                color = TangemTheme.colors.text.tertiary,
                maxLines = 1,
// [REDACTED_TODO_COMMENT]
                style = TangemTheme.typography.subtitle1.copy(fontSize = 18.sp, fontWeight = FontWeight.Normal),
            )
        }
    }
}

@Preview
@Composable
private fun Preview_AddTokensToolbar_EditAccess() {
    TangemThemePreview {
        TokensListToolbar(
            state = Title.Manage(
                titleResId = R.string.main_manage_tokens,
                onBackButtonClick = {},
                onSearchButtonClick = {},
                onAddCustomTokenClick = {},
            ),
        )
    }
}

@Preview
@Composable
private fun Preview_AddTokensToolbar_ReadAccess() {
    TangemThemePreview {
        TokensListToolbar(
            state = Title.Read(
                titleResId = R.string.common_search_tokens,
                onBackButtonClick = {},
                onSearchButtonClick = {},
            ),
        )
    }
}

@Preview
@Composable
private fun Preview_AddTokensToolbar_SearchInputField() {
    var value by remember { mutableStateOf("") }

    TangemThemePreview {
        TokensListToolbar(
            state = InputField(
                onBackButtonClick = {},
                value = value,
                onValueChange = { value = it },
                onCleanButtonClick = { value = "" },
            ),
        )
    }
}
