package com.tangem.tap.features.tokens.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.tokens.presentation.states.AddTokensToolbarState
import com.tangem.tap.features.tokens.presentation.states.AddTokensToolbarState.SearchInputField
import com.tangem.tap.features.tokens.presentation.states.AddTokensToolbarState.Title
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
@Composable
internal fun AddTokensToolbar(state: AddTokensToolbarState) {
    TopAppBar(backgroundColor = TangemTheme.colors.background.secondary) {
        IconButton(onClick = state.onBackButtonClick) {
            Icon(
                painter = painterResource(id = R.drawable.ic_back_24),
                contentDescription = "Go back",
                tint = TangemTheme.colors.icon.secondary,
            )
        }

        val contentModifier = Modifier
            .weight(1f)
            .padding(horizontal = TangemTheme.dimens.spacing16)
            .padding(bottom = TangemTheme.dimens.spacing4)

        when (state) {
            is Title -> TitleContent(state = state, modifier = contentModifier)
            is SearchInputField -> InputContent(state = state, modifier = contentModifier)
        }
    }
}

@Composable
private fun TitleContent(state: Title, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(id = state.titleResId),
        modifier = modifier,
        color = TangemTheme.colors.text.primary1,
        maxLines = 1,
        style = TangemTheme.typography.h3,
    )

    IconButton(onClick = state.onSearchButtonClick) {
        Icon(
            painter = painterResource(id = R.drawable.ic_search_24),
            contentDescription = "Search",
            tint = TangemTheme.colors.icon.secondary,
        )
    }

    if (state is Title.EditAccess) {
        IconButton(onClick = state.onAddCustomTokenClick) {
            Icon(
                painter = painterResource(id = R.drawable.ic_plus_24),
                contentDescription = "Add custom token",
                tint = TangemTheme.colors.icon.secondary,
            )
        }
    }
}

@Composable
fun InputContent(state: SearchInputField, modifier: Modifier = Modifier) {
    BasicTextField(
        value = state.value,
        onValueChange = state.onValueChange,
        modifier = modifier,
        textStyle = TangemTheme.typography.subtitle1.copy(fontWeight = FontWeight.Normal),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { state.onSearchButtonClick() }),
        singleLine = true,
        maxLines = 1,
        cursorBrush = SolidColor(value = TangemTheme.colors.stroke.secondary),
    ) { innerTextField ->
        Column(modifier = Modifier.padding(top = TangemTheme.dimens.spacing6)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = TangemTheme.dimens.spacing6),
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height = TangemTheme.dimens.size1)
                    .background(color = TangemTheme.colors.stroke.primary),
            )
        }
    }
}

@Composable
private fun Hint(value: String) {
    if (value.isBlank()) {
        Row(
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing4),
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
                // FIXME("Incorrect typography. Replace with typography from design system")
                style = TangemTheme.typography.subtitle1.copy(fontSize = 18.sp, fontWeight = FontWeight.Normal),
            )
        }
    }
}

@Preview
@Composable
private fun Preview_AddTokensToolbar_EditAccess() {
    TangemTheme {
        AddTokensToolbar(
            state = Title.EditAccess(
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
    TangemTheme {
        AddTokensToolbar(
            state = Title.ReadAccess(
                titleResId = R.string.search_tokens_title,
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

    TangemTheme {
        AddTokensToolbar(
            state = SearchInputField(
                onBackButtonClick = {},
                onSearchButtonClick = {},
                value = value,
                onValueChange = { value = it },
                onCleanButtonClick = { value = "" },
            ),
        )
    }
}