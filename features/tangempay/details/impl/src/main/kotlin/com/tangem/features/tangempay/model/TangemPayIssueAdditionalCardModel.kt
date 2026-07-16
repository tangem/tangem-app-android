package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.pay.usecase.IssueAdditionalCardUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.components.TangemPayIssueAdditionalCardComponent
import com.tangem.features.tangempay.entity.TangemPayIssueAdditionalCardUM
import com.tangem.features.tangempay.utils.TangemPayMessagesFactory
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayIssueAdditionalCardModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val issueAdditionalCard: IssueAdditionalCardUseCase,
    private val uiMessageSender: UiMessageSender,
    private val analytics: AnalyticsEventHandler,
) : Model() {

    private val params: TangemPayIssueAdditionalCardComponent.Params = paramsContainer.require()

    val uiState: StateFlow<TangemPayIssueAdditionalCardUM>
        field = MutableStateFlow(buildInitialState())

    private fun buildInitialState(): TangemPayIssueAdditionalCardUM {
        return TangemPayIssueAdditionalCardUM(
            isBalanceInsufficient = params.fiatBalance < params.feeAmount,
            feeText = formatFee(),
            isLoading = false,
            onIssueClick = ::onIssueClick,
            onAddFundsClick = { params.listener.onAddFundsForCardIssue() },
            onDismiss = ::onDismiss,
        )
    }

    fun onDismiss() {
        params.listener.onIssueAdditionalCardDismissed()
    }

    private fun onIssueClick() {
        analytics.send(TangemPayAnalyticsEvents.IssueAdditionalCardConfirmed())
        uiState.update { it.copy(isLoading = true) }
        modelScope.launch {
            issueAdditionalCard(userWalletId = params.userWalletId).fold(
                ifLeft = ::handleIssueError,
                ifRight = {
                    uiState.update { uiState -> uiState.copy(isLoading = false) }
                    params.listener.onIssueAdditionalCardSucceeded()
                },
            )
        }
    }

    private fun handleIssueError(error: VisaApiError) {
        when (error) {
            VisaApiError.CardIssueInsufficientBalance -> uiState.update { it.copy(isBalanceInsufficient = true) }
            else -> {
                uiMessageSender.send(message = TangemPayMessagesFactory.createGenericError())
                onDismiss()
            }
        }
    }

    private fun formatFee(): String {
        return params.feeAmount.format {
            fiat(
                fiatCurrencyCode = params.feeCurrency.currencyCode,
                fiatCurrencySymbol = params.feeCurrency.symbol,
            )
        }
    }
}