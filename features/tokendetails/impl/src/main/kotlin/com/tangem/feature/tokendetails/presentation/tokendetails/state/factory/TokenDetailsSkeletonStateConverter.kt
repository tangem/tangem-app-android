package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.networkIconResId
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.feature.tokendetails.presentation.tokendetails.state.*
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsActionButton
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsPullToRefreshConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.features.tokendetails.featuretoggles.TokenDetailsFeatureToggles
import com.tangem.features.tokendetails.impl.R
import com.tangem.lib.crypto.BlockchainUtils.isBitcoin
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow

internal class TokenDetailsSkeletonStateConverter(
    private val clickIntents: TokenDetailsClickIntents,
    private val featureToggles: TokenDetailsFeatureToggles,
) : Converter<CryptoCurrency, TokenDetailsState> {

    private val iconStateConverter by lazy { TokenDetailsIconStateConverter() }

    override fun convert(value: CryptoCurrency): TokenDetailsState {
        return TokenDetailsState(
            topAppBarConfig = TokenDetailsTopAppBarConfig(
                onBackClick = clickIntents::onBackClick,
                tokenDetailsAppBarMenuConfig = createMenu(value),
            ),
            tokenInfoBlockState = TokenInfoBlockState(
                name = value.name,
                iconState = iconStateConverter.convert(value),
                currency = when (value) {
                    is CryptoCurrency.Coin -> TokenInfoBlockState.Currency.Native
                    is CryptoCurrency.Token -> TokenInfoBlockState.Currency.Token(
                        standardName = value.network.standardType.name,
                        networkName = value.network.name,
                        networkIcon = value.networkIconResId,
                    )
                },
            ),
            tokenBalanceBlockState = TokenDetailsBalanceBlockState.Loading(actionButtons = createButtons()),
            marketPriceBlockState = MarketPriceBlockState.Loading(value.symbol),
            notifications = persistentListOf(),
            pendingTxs = persistentListOf(),
            swapTxs = persistentListOf(),
            txHistoryState = TxHistoryState.Content(
                contentItems = MutableStateFlow(
                    value = TxHistoryState.getDefaultLoadingTransactions(clickIntents::onExploreClick),
                ),
            ),
            dialogConfig = null,
            pullToRefreshConfig = createPullToRefresh(),
            bottomSheetConfig = null,
            isBalanceHidden = true,
            isMarketPriceAvailable = value.id.rawCurrencyId != null,
            event = consumedEvent(),
        )
    }

    private fun createMenu(cryptoCurrency: CryptoCurrency): TokenDetailsAppBarMenuConfig = TokenDetailsAppBarMenuConfig(
        items = buildList {
            if (featureToggles.isGenerateXPubEnabled() && isBitcoin(cryptoCurrency.network.id.value)) {
                TokenDetailsAppBarMenuConfig.MenuItem(
                    title = resourceReference(R.string.token_details_generate_xpub),
                    textColorProvider = { TangemTheme.colors.text.primary1 },
                    onClick = clickIntents::onGenerateExtendedKey,
                ).let(::add)
            }
            TokenDetailsAppBarMenuConfig.MenuItem(
                title = TextReference.Res(id = R.string.token_details_hide_token),
                textColorProvider = { TangemTheme.colors.text.warning },
                onClick = clickIntents::onHideClick,
            ).let(::add)
        }.toImmutableList(),
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
}
