package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.utils.transformer.Transformer

internal class InitializeWithCryptoCurrencyTransformer(
    private val cryptoCurrency: CryptoCurrency,
    private val onBackClick: () -> Unit,
    private val onRefreshSwipe: (Boolean) -> Unit,
) : Transformer<TokenDetailsUM> {

    override fun transform(prevState: TokenDetailsUM): TokenDetailsUM {
        val iconState = CryptoCurrencyToIconStateConverter().convert(cryptoCurrency)
        return prevState.copy(
            topAppBarUM = prevState.topAppBarUM.copy(
                titleState = TokenDetailsTopAppBarUM.TitleState.Simple(tokenName = cryptoCurrency.name),
                subtitle = stringReference(cryptoCurrency.symbol),
                onBackClick = onBackClick,
            ),
            balanceBlockUM = prevState.balanceBlockUM.copyCurrencyIconState(iconState),
            marketPriceBlockState = MarketPriceBlockState.Loading(currencySymbol = cryptoCurrency.symbol),
            pullToRefreshConfig = prevState.pullToRefreshConfig.copy(
                onRefresh = { onRefreshSwipe(it.value) },
            ),
        )
    }
}