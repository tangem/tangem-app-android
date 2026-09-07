package com.tangem.features.tangempay.orderCard.impl.model

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.GetCustomerOffersUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.orderCard.impl.TangemPayReissuePlasticCardComponent
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayReissuePlasticCardUM
import com.tangem.utils.CountryNames
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.extensions.orZero
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayReissuePlasticCardModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getCustomerOffers: GetCustomerOffersUseCase,
    private val onboardingRepository: OnboardingRepository,
    private val analytics: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<TangemPayReissuePlasticCardComponent.Params>()
    private val loadDataJobHolder = JobHolder()

    val state: StateFlow<TangemPayReissuePlasticCardUM>
        field = MutableStateFlow<TangemPayReissuePlasticCardUM>(
            TangemPayReissuePlasticCardUM.Loading(onDismissRequest = ::onDismiss),
        )

    init {
        analytics.send(TangemPayAnalyticsEvents.ReplaceCardConfirmationPopupOpened())
        loadData()
    }

    fun onDismiss() {
        loadDataJobHolder.cancel()
        params.onDismiss()
    }

    private fun loadData() {
        state.value = TangemPayReissuePlasticCardUM.Loading(onDismissRequest = ::onDismiss)

        modelScope.launch {
            val (offer, customerInfo) = coroutineScope {
                val offerDeferred = async {
                    getCustomerOffers.plasticReissueOffer(
                        userWalletId = params.userWalletId,
                        productInstanceId = params.sourceProductInstanceId,
                    ).getOrNull()
                }
                val customerInfoDeferred = async {
                    onboardingRepository.getCustomerInfo(params.userWalletId).getOrNull()
                }
                offerDeferred.await() to customerInfoDeferred.await()
            }

            state.value = buildState(offer = offer, customerInfo = customerInfo)
        }.saveIn(loadDataJobHolder)
    }

    private fun buildState(offer: Offer?, customerInfo: CustomerInfo?): TangemPayReissuePlasticCardUM {
        if (offer == null || customerInfo == null) return errorState()

        val deliveryEta = offer.data.deliveryEta ?: return errorState()
        val country = CountryNames.getDisplayName(customerInfo.country)
        if (country.isBlank()) return errorState()

        val fee = offer.fee
        val availableBalance = customerInfo.fiatBalance?.availableBalance.orZero()
        return TangemPayReissuePlasticCardUM.Content(
            onDismissRequest = ::onDismiss,
            country = country,
            deliveryFee = fee.formatted(),
            deliveryEtaMaxBusinessDays = deliveryEta.maxBusinessDays,
            isInsufficientFunds = availableBalance < fee.amount,
            onReplaceClick = { onReplaceClick(deliveryEta.maxBusinessDays) },
        )
    }

    private fun errorState() = TangemPayReissuePlasticCardUM.Error(
        onDismissRequest = ::onDismiss,
        onRetry = ::loadData,
    )

    private fun onReplaceClick(deliveryEtaMaxBusinessDays: Int) {
        analytics.send(TangemPayAnalyticsEvents.ReplaceCardConfirmed())
        params.onReplaceConfirmed(deliveryEtaMaxBusinessDays)
    }

    private fun Offer.Fee.formatted(): String = amount.format {
        fiat(fiatCurrencyCode = currency.currencyCode, fiatCurrencySymbol = currency.symbol)
    }
}