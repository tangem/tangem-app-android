package com.tangem.features.onramp.selecttoken

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import arrow.core.getOrElse
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.tokens.legacy.TradeCryptoAction
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.selecttoken.ui.OnrampSelectToken
import com.tangem.features.onramp.tokenlist.OnrampTokenListComponent
import com.tangem.features.onramp.tokenlist.entity.OnrampOperation
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

internal class DefaultOnrampOperationComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    onrampTokenListComponentFactory: OnrampTokenListComponent.Factory,
    @Assisted private val params: OnrampOperationComponent.Params,
    private val reduxStateHolder: ReduxStateHolder,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
) : AppComponentContext by appComponentContext, OnrampOperationComponent {

    private val onrampTokenListComponent: OnrampTokenListComponent = onrampTokenListComponentFactory.create(
        context = child(key = "token_list"),
        params = OnrampTokenListComponent.Params(
            filterOperation = when (params) {
                is OnrampOperationComponent.Params.Buy -> OnrampOperation.BUY
                is OnrampOperationComponent.Params.Sell -> OnrampOperation.SELL
            },
            hasSearchBar = true,
            userWalletId = params.userWalletId,
            onTokenClick = { _, status -> onTokenClick(status) },
        ),
    )

    @Composable
    override fun Content(modifier: Modifier) {
        OnrampSelectToken(
            titleResId = when (params) {
                is OnrampOperationComponent.Params.Buy -> R.string.common_buy
                is OnrampOperationComponent.Params.Sell -> R.string.common_sell
            },
            onBackClick = router::pop,
            onrampTokenListComponent = onrampTokenListComponent,
            modifier = modifier,
        )
    }

    private fun onTokenClick(status: CryptoCurrencyStatus) {
        componentScope.launch {
            val appCurrencyCode = getSelectedAppCurrencyUseCase.invokeSync().getOrElse { AppCurrency.Default }.code

            reduxStateHolder.dispatch(
                action = when (params) {
                    is OnrampOperationComponent.Params.Buy -> getBuyAction(status, appCurrencyCode)
                    is OnrampOperationComponent.Params.Sell -> TradeCryptoAction.Sell(status, appCurrencyCode)
                },
            )
        }
    }

    private fun getBuyAction(status: CryptoCurrencyStatus, appCurrencyCode: String): TradeCryptoAction {
        return TradeCryptoAction.Buy(
            userWallet = getWalletsUseCase.invokeSync().first { it.walletId == params.userWalletId },
            cryptoCurrencyStatus = status,
            appCurrencyCode = appCurrencyCode,
        )
    }

    @AssistedFactory
    interface Factory : OnrampOperationComponent.Factory {

        override fun create(
            context: AppComponentContext,
            params: OnrampOperationComponent.Params,
        ): DefaultOnrampOperationComponent
    }
}
