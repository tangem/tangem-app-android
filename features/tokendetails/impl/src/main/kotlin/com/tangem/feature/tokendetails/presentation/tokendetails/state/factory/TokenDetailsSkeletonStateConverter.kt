package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.networkIconResId
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.feature.tokendetails.presentation.tokendetails.state.*
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsActionButton
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsPullToRefreshConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.TokenDetailsSkeletonStateConverter.SkeletonModel
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.features.tokendetails.impl.R
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow

internal class TokenDetailsSkeletonStateConverter(
    private val clickIntents: TokenDetailsClickIntents,
) : Converter<SkeletonModel, TokenDetailsState> {

    override fun convert(value: SkeletonModel): TokenDetailsState {
        return TokenDetailsState(
            topAppBarConfig = TokenDetailsTopAppBarConfig(
                onBackClick = clickIntents::onBackClick,
                tokenDetailsAppBarMenuConfig = createMenu(),
            ),
            tokenInfoBlockState = TokenInfoBlockState(
                name = value.cryptoCurrency.name,
                iconUrl = requireNotNull(value.cryptoCurrency.iconUrl),
                currency = when (val currency = value.cryptoCurrency) {
                    is CryptoCurrency.Coin -> TokenInfoBlockState.Currency.Native
                    is CryptoCurrency.Token -> TokenInfoBlockState.Currency.Token(
                        networkName = currency.network.standardType.name,
                        blockchainName = currency.network.name,
                        networkIcon = currency.networkIconResId,
                    )
                },
            ),
            tokenBalanceBlockState = TokenDetailsBalanceBlockState.Loading(
                actionButtons = createButtons(),
            ),
            marketPriceBlockState = MarketPriceBlockState.Loading(value.cryptoCurrency.name),
            pendingTxs = persistentListOf(),
            txHistoryState = TxHistoryState.Content(
                contentItems = MutableStateFlow(
                    value = TxHistoryState.getDefaultLoadingTransactions(clickIntents::onExploreClick),
                ),
            ),
            dialogConfig = null,
            pullToRefreshConfig = createPullToRefresh(),
        )
    }

    private fun createMenu(): TokenDetailsAppBarMenuConfig = TokenDetailsAppBarMenuConfig(
        items = persistentListOf(
            TokenDetailsAppBarMenuConfig.MenuItem(
                title = TextReference.Res(id = R.string.token_details_hide_token),
                textColorProvider = { TangemTheme.colors.text.warning },
                onClick = clickIntents::onHideClick,
            ),
        ),
    )

    private fun createButtons(): ImmutableList<TokenDetailsActionButton> {
        return persistentListOf(
            TokenDetailsActionButton.Buy(enabled = false, onClick = {}),
            TokenDetailsActionButton.Send(enabled = false, onClick = {}),
            TokenDetailsActionButton.Receive(onClick = {}),
            TokenDetailsActionButton.Sell(enabled = false, onClick = {}),
            TokenDetailsActionButton.Swap(enabled = false, onClick = {}),
        )
    }

    private fun createPullToRefresh(): TokenDetailsPullToRefreshConfig = TokenDetailsPullToRefreshConfig(
        isRefreshing = false,
        onRefresh = clickIntents::onRefreshSwipe,
    )

    data class SkeletonModel(val cryptoCurrency: CryptoCurrency)
}