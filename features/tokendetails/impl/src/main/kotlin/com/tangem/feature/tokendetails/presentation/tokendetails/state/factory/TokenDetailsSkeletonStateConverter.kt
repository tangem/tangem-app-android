package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.extensions.iconResId
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenInfoBlockState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsActionButton
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.TokenDetailsSkeletonStateConverter.SkeletonModel
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
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
                onMoreClick = clickIntents::onMoreClick,
            ),
            tokenInfoBlockState = TokenInfoBlockState(
                name = value.cryptoCurrency.name,
                iconUrl = requireNotNull(value.cryptoCurrency.iconUrl),
                currency = when (val currency = value.cryptoCurrency) {
                    is CryptoCurrency.Coin -> TokenInfoBlockState.Currency.Native
                    is CryptoCurrency.Token -> TokenInfoBlockState.Currency.Token(
                        networkName = currency.network.standardType.name,
                        blockchainName = currency.network.name,
                        networkIcon = currency.iconResId,
                    )
                },
            ),
            tokenBalanceBlockState = TokenDetailsBalanceBlockState.Loading(
                actionButtons = createButtons(),
            ),
            marketPriceBlockState = MarketPriceBlockState.Loading(value.cryptoCurrency.name),
            txHistoryState = TxHistoryState.Content(
                contentItems = MutableStateFlow(
                    value = TxHistoryState.getDefaultLoadingTransactions(clickIntents::onExploreClick),
                ),
            ),
        )
    }

    private fun createButtons(): ImmutableList<TokenDetailsActionButton> {
        return persistentListOf(
            TokenDetailsActionButton.Buy(enabled = false, onClick = {}),
            TokenDetailsActionButton.Send(enabled = false, onClick = {}),
            TokenDetailsActionButton.Receive(onClick = {}),
            TokenDetailsActionButton.Sell(enabled = false, onClick = {}),
            TokenDetailsActionButton.Swap(enabled = false, onClick = {}),
        )
    }

    data class SkeletonModel(val cryptoCurrency: CryptoCurrency)
}