package com.tangem.features.onramp.tokenlist.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.fields.SearchBar
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.tokenlist.TokenListItem
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onramp.tokenlist.entity.TokenListUM
import com.tangem.features.onramp.tokenlist.ui.preview.PreviewTokenListUMProvider
import kotlinx.collections.immutable.ImmutableList

/**
 * Token list
 *
 * @param state    state
 * @param modifier modifier
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun TokenList(state: TokenListUM, modifier: Modifier = Modifier) {
    Column(modifier) {
        if (state.warning == null) {
            SearchBar(searchBarUM = state.searchBarUM)
        } else {
            AnimatedContent(targetState = state.warning, label = "") { warning ->
                when (warning) {
                    is NotificationUM.Warning.OnrampErrorNotification -> {
                        Notification(
                            config = warning.config,
                            containerColor = TangemTheme.colors.background.primary,
                        )
                    }
                    else -> {
                        Notification(config = warning.config, containerColor = TangemTheme.colors.button.disabled)
                    }
                }
            }
        }

        if (state.availableItems.isNotEmpty()) {
            SpacerH12()
            ItemsBlock(items = state.availableItems, isBalanceHidden = state.isBalanceHidden)
        }

        if (state.unavailableItems.isNotEmpty()) {
            SpacerH12()
            ItemsBlock(items = state.unavailableItems, isBalanceHidden = state.isBalanceHidden)
        }
    }
}

@Composable
private fun SearchBar(searchBarUM: SearchBarUM) {
    SearchBar(
        state = searchBarUM,
        colors = TextFieldDefaults.colors().copy(
            focusedContainerColor = TangemTheme.colors.field.focused,
            unfocusedContainerColor = TangemTheme.colors.field.focused,
            focusedTextColor = TangemTheme.colors.text.primary1,
            unfocusedTextColor = TangemTheme.colors.text.primary1,
            cursorColor = TangemTheme.colors.icon.primary1,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun ItemsBlock(items: ImmutableList<TokensListItemUM>, isBalanceHidden: Boolean) {
    items.fastForEachIndexed { index, item ->
        key(item.id) {
            TokenListItem(
                state = item,
                isBalanceHidden = isBalanceHidden,
                modifier = Modifier
                    .roundedShapeItemDecoration(
                        currentIndex = index,
                        lastIndex = items.lastIndex,
                        addDefaultPadding = false,
                        backgroundColor = TangemTheme.colors.background.primary,
                    ),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TokenList(@PreviewParameter(PreviewTokenListUMProvider::class) state: TokenListUM) {
    TangemThemePreview {
        TokenList(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .background(color = TangemTheme.colors.background.secondary)
                .padding(16.dp),
        )
    }
}