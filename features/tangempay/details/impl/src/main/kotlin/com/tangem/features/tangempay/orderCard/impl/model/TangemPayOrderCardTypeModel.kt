package com.tangem.features.tangempay.orderCard.impl.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.model.plasticOffer
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.GetCustomerOffersUseCase
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.common.cardMainImageUrl
import com.tangem.features.tangempay.orderCard.impl.TangemPayOrderCardTypeComponent
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardTypeUM
import com.tangem.features.tangempay.orderCard.impl.ui.state.availableTypesOf
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.extensions.orZero
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
    private val onboardingRepository: OnboardingRepository,
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
                availableTypes = availableTypesOf(isPlasticEnabled = featureToggles.isPlasticCardOrderEnabled),
                cardImageUrl = null,
                virtual = TangemPayOrderCardTypeUM.Virtual(issueFee = ""),
                plastic = TangemPayOrderCardTypeUM.Plastic.Unavailable(country = ""),
                onBackClick = ::onBackClick,
                onRetry = ::loadData,
                onSelectVirtual = params.onSelectVirtual,
                onSelectPlastic = ::onSelectPlastic,
            ),
        )

    init {
        loadData()
        observeCardImage()
    }

    fun onBackClick() {
        router.pop()
    }

    private fun onSelectPlastic() {
        val plastic = state.value.plastic as? TangemPayOrderCardTypeUM.Plastic.Available ?: return
        params.onSelectPlastic(plastic.deliveryEta.maxBusinessDays)
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

            val plasticContent = if (featureToggles.isPlasticCardOrderEnabled) {
                val customerInfo = onboardingRepository.getCustomerInfo(params.userWalletId).getOrNull()
                if (customerInfo == null) {
                    state.update { it.copy(isLoading = false, isError = true) }
                    return@launch
                }
                offers.plasticOffer()?.toPlasticContent(customerInfo)
                    ?: TangemPayOrderCardTypeUM.Plastic.Unavailable(country = customerInfo.country.orEmpty())
            } else {
                null
            }

            val virtualOffer = offers.firstOrNull { it.type == Offer.Type.CARD_ISSUE_VIRTUAL_RAIN }

            state.update { current ->
                current.copy(
                    isLoading = false,
                    isError = false,
                    virtual = TangemPayOrderCardTypeUM.Virtual(
                        issueFee = virtualOffer?.fee?.let { fee -> fee.amount.formatFiat(fee.currency) }.orEmpty(),
                    ),
                    plastic = plasticContent ?: current.plastic,
                )
            }
        }.saveIn(loadDataJobHolder)
    }

    private fun Offer.toPlasticContent(customerInfo: CustomerInfo): TangemPayOrderCardTypeUM.Plastic.Available? {
        val deliveryEta = data.deliveryEta ?: return null
        val availableBalance = customerInfo.fiatBalance?.availableBalance.orZero()
        val isFeeZero = fee.amount.signum() == 0
        val feeState = when {
            isFeeZero -> TangemPayOrderCardTypeUM.FeeState.FreeDelivery
            availableBalance < fee.amount -> TangemPayOrderCardTypeUM.FeeState.InsufficientFunds
            else -> TangemPayOrderCardTypeUM.FeeState.Default
        }
        return TangemPayOrderCardTypeUM.Plastic.Available(
            country = customerInfo.country.orEmpty(),
            deliveryFee = fee.amount.formatFiat(fee.currency).takeUnless { isFeeZero },
            deliveryEta = TangemPayOrderCardTypeUM.DeliveryEta(
                minBusinessDays = deliveryEta.minBusinessDays,
                maxBusinessDays = deliveryEta.maxBusinessDays,
            ),
            feeState = feeState,
        )
    }

    private fun BigDecimal.formatFiat(currency: Currency): String = format {
        fiat(fiatCurrencyCode = currency.currencyCode, fiatCurrencySymbol = currency.symbol).optionalDecimals()
    }
}