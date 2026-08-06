package com.tangem.features.tangempay.orderCard.impl.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.model.CardDeliveryContext
import com.tangem.domain.pay.model.CardDeliveryQuote
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.model.plasticOffer
import com.tangem.domain.pay.repository.CardDeliveryQuoteRepository
import com.tangem.domain.pay.usecase.GetCustomerOffersUseCase
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.common.cardMainImageUrl
import com.tangem.features.tangempay.orderCard.impl.TangemPayOrderCardTypeComponent
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardTypeUM
import com.tangem.features.tangempay.orderCard.impl.ui.state.availableTypesOf
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Currency
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class TangemPayOrderCardTypeModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val getCustomerOffers: GetCustomerOffersUseCase,
    private val cardDeliveryQuoteRepository: CardDeliveryQuoteRepository,
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
    private val featureToggles: TangemPayFeatureToggles,
) : Model() {

    private val params = paramsContainer.require<TangemPayOrderCardTypeComponent.Params>()
    private val loadDataJobHolder = JobHolder()

    val state: StateFlow<TangemPayOrderCardTypeUM>
        field = MutableStateFlow(
            TangemPayOrderCardTypeUM(
                isLoading = true,
                isError = false,
                availableTypes = availableTypesOf(isPlasticAvailable = false),
                cardImageUrl = null,
                virtual = TangemPayOrderCardTypeUM.Virtual(issueFee = ""),
                plastic = null,
                onBackClick = ::onBackClick,
                onRetry = ::loadData,
                onSelectVirtual = params.onSelectVirtual,
                onSelectPlastic = params.onSelectPlastic,
            ),
        )

    init {
        loadData()
        observeCardImage()
    }

    fun onBackClick() {
        router.pop()
    }

    private fun observeCardImage() {
        modelScope.launch {
            paymentAccountStatusSupplier(params.userWalletId).collect { status ->
                val url = status.takeIf { it.value is PaymentAccountStatusValue.Loaded }?.cardMainImageUrl
                if (url != null) {
                    state.update { it.copy(cardImageUrl = url) }
                }
            }
        }
    }

    private fun loadData() {
        state.update { it.copy(isLoading = true, isError = false) }

        modelScope.launch {
            val offers = getCustomerOffers(params.userWalletId).getOrNull()
            if (offers == null) {
                state.update { it.copy(isLoading = false, isError = true) }
                return@launch
            }

            val plasticOffer = if (featureToggles.isPlasticCardOrderEnabled) offers.plasticOffer() else null
            val plasticContent = if (plasticOffer != null) {
                val quote = cardDeliveryQuoteRepository
                    .getCardDeliveryQuote(params.userWalletId, CardDeliveryContext.ISSUE)
                    .getOrNull()
                if (quote == null) {
                    state.update { it.copy(isLoading = false, isError = true) }
                    return@launch
                }
                quote.toPlasticContent()
            } else {
                null
            }

            val virtualOffer = offers.firstOrNull { it.type == Offer.Type.CARD_ISSUE_VIRTUAL_RAIN }

            state.update { current ->
                current.copy(
                    isLoading = false,
                    isError = false,
                    availableTypes = availableTypesOf(isPlasticAvailable = plasticContent != null),
                    virtual = TangemPayOrderCardTypeUM.Virtual(
                        issueFee = virtualOffer?.fee?.let { fee -> fee.amount.formatFiat(fee.currency) }.orEmpty(),
                    ),
                    plastic = plasticContent,
                )
            }
        }.saveIn(loadDataJobHolder)
    }

    private fun CardDeliveryQuote.toPlasticContent(): TangemPayOrderCardTypeUM.Plastic {
        val feeState = when {
            isDeliveryFeeWaived -> TangemPayOrderCardTypeUM.FeeState.FreeDelivery
            deliveryFee.amount.signum() > 0 && !hasSufficientBalance ->
                TangemPayOrderCardTypeUM.FeeState.InsufficientFunds
            else -> TangemPayOrderCardTypeUM.FeeState.Default
        }
        return TangemPayOrderCardTypeUM.Plastic(
            country = country,
            deliveryFee = deliveryFee.amount.formatFiat(deliveryFee.currency),
            deliveryEtaMaxBusinessDays = deliveryEta.maxBusinessDays,
            feeState = feeState,
        )
    }

    private fun BigDecimal.formatFiat(currency: Currency): String = format {
        fiat(fiatCurrencyCode = currency.currencyCode, fiatCurrencySymbol = currency.symbol)
    }
}