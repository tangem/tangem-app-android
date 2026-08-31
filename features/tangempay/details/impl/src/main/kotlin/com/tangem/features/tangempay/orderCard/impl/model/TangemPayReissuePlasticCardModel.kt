package com.tangem.features.tangempay.orderCard.impl.model

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.domain.models.pay.TangemPayReissueCardFee
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.model.plasticOffer
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.repository.TangemPayReissueCardRepository
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

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class TangemPayReissuePlasticCardModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getCustomerOffers: GetCustomerOffersUseCase,
    private val onboardingRepository: OnboardingRepository,
    private val reissueCardRepository: TangemPayReissueCardRepository,
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
            val (offer, fee, customerInfo) = coroutineScope {
                val offerDeferred = async { getCustomerOffers(params.userWalletId).getOrNull()?.plasticOffer() }
                val feeDeferred = async {
                    reissueCardRepository.getPlasticReissueCardFee(params.userWalletId).getOrNull()
                }
                val customerInfoDeferred = async {
                    onboardingRepository.getCustomerInfo(params.userWalletId).getOrNull()
                }
                Triple(offerDeferred.await(), feeDeferred.await(), customerInfoDeferred.await())
            }

            state.value = buildState(offer = offer, fee = fee, customerInfo = customerInfo)
        }.saveIn(loadDataJobHolder)
    }

    private fun buildState(
        offer: Offer?,
        fee: TangemPayReissueCardFee?,
        customerInfo: CustomerInfo?,
    ): TangemPayReissuePlasticCardUM {
        val deliveryEta = offer?.data?.deliveryEta
        val country = CountryNames.getDisplayName(customerInfo?.country)
        if (deliveryEta == null || fee == null || country.isBlank()) {
            return TangemPayReissuePlasticCardUM.Error(onDismissRequest = ::onDismiss, onRetry = ::loadData)
        }

        val availableBalance = customerInfo?.fiatBalance?.availableBalance.orZero()
        return TangemPayReissuePlasticCardUM.Content(
            onDismissRequest = ::onDismiss,
            country = country,
            deliveryFee = fee.formatted(),
            deliveryEtaMaxBusinessDays = deliveryEta.maxBusinessDays,
            isInsufficientFunds = availableBalance < fee.amount,
            onReplaceClick = { onReplaceClick(deliveryEta.maxBusinessDays) },
        )
    }

    private fun onReplaceClick(deliveryEtaMaxBusinessDays: Int) {
        analytics.send(TangemPayAnalyticsEvents.ReplaceCardConfirmed())
        params.onReplaceConfirmed(deliveryEtaMaxBusinessDays)
    }

    private fun TangemPayReissueCardFee.formatted(): String = amount.format {
        fiat(fiatCurrencyCode = currencyCode, fiatCurrencySymbol = getJavaCurrencyByCode(currencyCode).symbol)
    }
}