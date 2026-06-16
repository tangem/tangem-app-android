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
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.tokendetails.impl.R
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class TokenDetailsStateController @Inject constructor() {

    val uiState: StateFlow<TokenDetailsUM>
        field = MutableStateFlow(value = getInitialState())

    val value: TokenDetailsUM get() = uiState.value

    val isBalanceHidden: Flow<Boolean> = uiState
        .map { it.isBalanceHidden }
        .distinctUntilChanged()

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
                addFundsButton = TangemButtonUM(
                    text = resourceReference(R.string.tangempay_card_details_add_funds),
                    tangemIconUM = TangemIconUM.Icon(iconRes = R.drawable.ic_arrow_down_24),
                    onClick = { },
                    isEnabled = true,
                    type = TangemButtonType.Secondary,
                ),
                swapButton = TangemButtonUM(
                    text = resourceReference(R.string.common_swap),
                    tangemIconUM = TangemIconUM.Icon(
                        iconRes = R.drawable.ic_exchange_default_24,
                        tintReference = { TangemTheme.colors2.graphic.neutral.quaternary },
                    ),
                    onClick = { },
                    isEnabled = false,
                    type = TangemButtonType.Secondary,
                ),
                transferButton = TangemButtonUM(
                    text = resourceReference(R.string.common_transfer),
                    tangemIconUM = TangemIconUM.Icon(iconRes = R.drawable.ic_arrow_up_24),
                    onClick = { },
                    isEnabled = true,
                    type = TangemButtonType.Secondary,
                ),
                tokenBalanceTypeUM = TokenBalanceTypeUM.Single,
                currencyIconState = CurrencyIconState.Loading,
            ),
            notifications = persistentListOf(),
            earnBlockState = null,
            marketPriceBlockState = MarketPriceBlockState.Loading(currencySymbol = ""),
            pullToRefreshConfig = PullToRefreshConfig(
                isRefreshing = false,
                onRefresh = {},
            ),
            isBalanceHidden = false,
            isMarketPriceAvailable = false,
            addFundsUM = AddFundsUM.Loading,
            transferUM = TransferUM.Loading,
            zeroBalanceActionsUM = ZeroBalanceActionsUM.Loading,
        )
    }
}