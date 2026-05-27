package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.pay.repository.TangemPayReissueCardRepository
import com.tangem.domain.pay.usecase.ReissueTangemPayCardUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.components.TangemPayReissueCardComponent
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayReissueCardError
import com.tangem.features.tangempay.entity.TangemPayReissueCardUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class TangemPayReissueCardModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val reissueCardRepository: TangemPayReissueCardRepository,
    private val reissueTangemPayCardUseCase: ReissueTangemPayCardUseCase,
    private val uiMessageSender: UiMessageSender,
    private val analytics: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<TangemPayReissueCardComponent.Params>()
    private val reissueJobHolder = JobHolder()
    private val loadDataJobHolder = JobHolder()

    val state: StateFlow<TangemPayReissueCardUM>
        field = MutableStateFlow(
            TangemPayReissueCardUM(
                feeAmount = "",
                isFeeLoading = true,
                error = null,
                isReissuingInProgress = false,
                onConfirmClick = ::onConfirm,
                onRetryFee = ::loadData,
                onAddFundsClick = { params.listener.onClickAddFunds() },
                onDismissRequest = ::onDismiss,
            ),
        )

    init {
        analytics.send(TangemPayAnalyticsEvents.ReplaceCardConfirmationPopupOpened())
        loadData()
    }

    fun onDismiss() {
        reissueJobHolder.cancel()
        params.listener.onDismissReissueCard()
    }

    private fun onConfirm() {
        analytics.send(TangemPayAnalyticsEvents.ReplaceCardConfirmed())
        state.update { it.copy(isReissuingInProgress = true) }
        modelScope.launch {
            reissueTangemPayCardUseCase(
                userWalletId = params.userWalletId,
                cardId = params.cardId,
            ).onLeft {
                uiMessageSender.send(SnackbarMessage(resourceReference(R.string.common_something_went_wrong)))
            }
            onDismiss()
        }.saveIn(reissueJobHolder)
    }

    private fun loadData() {
        state.update { it.copy(isFeeLoading = true, error = null) }

        modelScope.launch {
            val (cardBalance, fee) = coroutineScope {
                val balanceDeferred = async { cardDetailsRepository.getCardBalance(params.userWalletId).getOrNull() }
                val feeDeferred = async { reissueCardRepository.getReissueCardFee(params.userWalletId).getOrNull() }
                balanceDeferred.await() to feeDeferred.await()
            }

            val error = if (fee == null || cardBalance == null) {
                TangemPayReissueCardError.InitialDataLoading
            } else if (cardBalance.fiatBalance < fee.amount) {
                TangemPayReissueCardError.InsufficientFunds
            } else {
                null
            }

            state.update { state ->
                state.copy(
                    feeAmount = fee?.let {
                        fee.amount.format {
                            val symbol = getJavaCurrencyByCode(fee.currencyCode).symbol
                            fiat(fee.currencyCode, symbol)
                        }
                    }.orEmpty(),
                    isFeeLoading = false,
                    error = error,
                )
            }
        }.saveIn(loadDataJobHolder)
    }
}