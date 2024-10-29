package com.tangem.features.onramp.buy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import arrow.core.getOrElse
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.tokens.legacy.TradeCryptoAction
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.onramp.component.BuyCryptoComponent
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.selecttoken.OnrampSelectTokenComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

@Stable
internal class DefaultBuyCryptoComponent @AssistedInject constructor(
    onrampSelectTokenComponentFactory: OnrampSelectTokenComponent.Factory,
    @Assisted private val appComponentContext: AppComponentContext,
    @Assisted private val params: BuyCryptoComponent.Params,
    private val reduxStateHolder: ReduxStateHolder,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
) : BuyCryptoComponent {

    private val selectTokenComponent: OnrampSelectTokenComponent = onrampSelectTokenComponentFactory.create(
        context = appComponentContext,
        params = OnrampSelectTokenComponent.Params(
            hasSearchBar = true,
            userWalletId = params.userWalletId,
            titleResId = R.string.common_buy,
            onTokenClick = ::onTokenClick,
        ),
    )

    @Composable
    override fun Content(modifier: Modifier) {
        selectTokenComponent.Content(modifier = modifier)
    }

    private fun onTokenClick(status: CryptoCurrencyStatus) {
        appComponentContext.componentScope.launch {
            reduxStateHolder.dispatch(
                TradeCryptoAction.Buy(
                    userWallet = getWalletsUseCase.invokeSync().first { it.walletId == params.userWalletId },
                    cryptoCurrencyStatus = status,
                    appCurrencyCode = getSelectedAppCurrencyUseCase.invokeSync()
                        .getOrElse { AppCurrency.Default }.code,
                ),
            )
        }
    }

    @AssistedFactory
    interface Factory : BuyCryptoComponent.Factory {

        override fun create(context: AppComponentContext, params: BuyCryptoComponent.Params): DefaultBuyCryptoComponent
    }
}