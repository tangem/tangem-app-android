package com.tangem.feature.tokendetails.presentation.tokendetails.state

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.tokendetails.impl.R
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class TokenDetailsStateController @Inject constructor() {

    val uiState: StateFlow<TokenDetailsUM>
        field = MutableStateFlow(value = getInitialState())

    val value: TokenDetailsUM get() = uiState.value

    fun update(function: (TokenDetailsUM) -> TokenDetailsUM) {
        uiState.update(function = function)
    }

    fun update(transformer: Transformer<TokenDetailsUM>) {
        uiState.update(function = transformer::transform)
    }

    private fun getInitialState(): TokenDetailsUM {
        return TokenDetailsUM(
            topAppBarUM = TokenDetailsTopAppBarUM(
                titleState = TokenDetailsTopAppBarUM.TitleState.Simple(tokenName = ""),
                subtitle = TextReference.EMPTY,
                onBackClick = {},
                menuItems = persistentListOf(),
            ),
            balanceBlockUM = TokenDetailsBalanceBlockUM.Loading(
                actionButtons = persistentListOf(
                    TangemButtonUM(
                        text = resourceReference(R.string.tangempay_card_details_add_funds),
                        tangemIconUM = TangemIconUM.Icon(iconRes = R.drawable.ic_arrow_down_24),
                        onClick = { },
                        isEnabled = true,
                        type = TangemButtonType.Secondary,
                    ),
                    TangemButtonUM(
                        text = resourceReference(R.string.common_transfer),
                        tangemIconUM = TangemIconUM.Icon(iconRes = R.drawable.ic_arrow_up_24),
                        onClick = { },
                        isEnabled = true,
                        type = TangemButtonType.Secondary,
                    ),
                ),
                tokenBalanceTypeUM = TokenBalanceTypeUM.Single,
                currencyIconState = CurrencyIconState.Loading,
            ),
            marketPriceBlockState = MarketPriceBlockState.Loading(currencySymbol = ""),
            stakingBlocksState = null,
            pullToRefreshConfig = PullToRefreshConfig(
                isRefreshing = false,
                onRefresh = {},
            ),
            isBalanceHidden = false,
            isMarketPriceAvailable = false,
        )
    }
}