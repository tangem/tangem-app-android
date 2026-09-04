package com.tangem.features.tangempay.withdraw.hub.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.pay.repository.TangemPayWithdrawRepository
import com.tangem.features.tangempay.common.TangemPayDetailsErrorType
import com.tangem.features.tangempay.common.TangemPayMessagesFactory
import com.tangem.features.tangempay.withdraw.buildWithdrawSwapRoute
import com.tangem.features.tangempay.withdraw.hub.TangemPayWithdrawHubComponent
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUMFactory
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class TangemPayWithdrawHubModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    tangemPayCurrencyFactory: TangemPayCurrencyFactory,
    private val tangemPayWithdrawRepository: TangemPayWithdrawRepository,
    private val uiMessageSender: UiMessageSender,
) : Model() {

    private val params = paramsContainer.require<TangemPayWithdrawHubComponent.Params>()

    private val cryptoCurrency: CryptoCurrency.Token = tangemPayCurrencyFactory.create(params.userWalletId)

    val uiState: StateFlow<TangemPayWithdrawHubUM>
        field = MutableStateFlow(
            TangemPayWithdrawHubUMFactory(
                onBackClick = ::onBackClick,
                onWithinPortfolioClick = ::onWithinPortfolioClick,
            ).create(),
        )

    private fun onBackClick() {
        router.pop()
    }

    private fun onWithinPortfolioClick() {
        modelScope.launch {
            val hasActiveWithdrawal = tangemPayWithdrawRepository.hasWithdrawOrder(params.userWalletId)
            if (hasActiveWithdrawal) {
                uiMessageSender.send(
                    TangemPayMessagesFactory.createErrorMessage(
                        errorType = TangemPayDetailsErrorType.WithdrawInProgress,
                    ),
                )
            } else {
                router.pop()
                router.push(buildWithdrawSwapRoute(userWalletId = params.userWalletId, cryptoCurrency = cryptoCurrency))
            }
        }
    }
}