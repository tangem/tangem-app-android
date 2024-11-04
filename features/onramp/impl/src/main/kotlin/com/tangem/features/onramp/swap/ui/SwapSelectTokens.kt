package com.tangem.features.onramp.swap.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.swap.entity.ExchangeCardUM
import com.tangem.features.onramp.swap.entity.SwapSelectTokensUM
import com.tangem.features.onramp.tokenlist.OnrampTokenListComponent

/**
 * Swap select tokens
 *
 * @param state                        state
 * @param selectFromTokenListComponent select "from" token list component
 * @param selectToTokenListComponent   select "to" token list component
 * @param modifier                     modifier
 *
* [REDACTED_AUTHOR]
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SwapSelectTokens(
    state: SwapSelectTokensUM,
    selectFromTokenListComponent: OnrampTokenListComponent,
    selectToTokenListComponent: OnrampTokenListComponent,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = state.onBackClick)

    LazyColumn(
        modifier = modifier
            .background(TangemTheme.colors.background.secondary)
            .imePadding()
            .systemBarsPadding(),
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        stickyHeader(key = "header") {
            AppBarWithBackButton(
                onBackClick = state.onBackClick,
                text = stringResource(id = R.string.common_swap),
                iconRes = R.drawable.ic_close_24,
                containerColor = TangemTheme.colors.background.secondary,
            )
        }

        item(key = "exchange_from", contentType = "exchange_from") {
            ExchangeCard(
                state = state.exchangeFrom,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 12.dp)
                    .animateItem(),
            )
        }

        if (state.exchangeFrom is ExchangeCardUM.Empty) {
            item(key = "select_from", contentType = "select_from") {
                selectFromTokenListComponent.Content(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .animateItem(),
                )
            }
        }

        if (state.exchangeFrom is ExchangeCardUM.Filled) {
            item(key = "exchange_to", contentType = "exchange_to") {
                ExchangeCard(
                    state = state.exchangeTo,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp)
                        .animateItem(),
                )
            }

            if (state.exchangeTo is ExchangeCardUM.Empty) {
                item(key = "select_to", contentType = "select_to") {
                    selectToTokenListComponent.Content(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .animateItem(),
                    )
                }
            }
        }
    }
}
