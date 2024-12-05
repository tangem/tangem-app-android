package com.tangem.features.onramp.providers.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.onramp.GetOnrampPaymentMethodsUseCase
import com.tangem.domain.onramp.GetOnrampProviderWithQuoteUseCase
import com.tangem.domain.onramp.GetOnrampSelectedPaymentMethodUseCase
import com.tangem.domain.onramp.OnrampSaveSelectedPaymentMethod
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.OnrampProviderWithQuote
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.paymentmethod.entity.PaymentMethodUM
import com.tangem.features.onramp.providers.SelectProviderComponent
import com.tangem.features.onramp.providers.entity.ProviderListBottomSheetConfig
import com.tangem.features.onramp.providers.entity.ProviderListItemUM
import com.tangem.features.onramp.providers.entity.ProviderListPaymentMethodUM
import com.tangem.features.onramp.providers.entity.ProviderListUM
import com.tangem.utils.StringsSigns.MINUS
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LongParameterList")
@ComponentScoped
internal class SelectProviderModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val getSelectedPaymentMethodsUseCase: GetOnrampPaymentMethodsUseCase,
    private val getOnrampSelectedPaymentMethodUseCase: GetOnrampSelectedPaymentMethodUseCase,
    private val getOnrampProviderWithQuoteUseCase: GetOnrampProviderWithQuoteUseCase,
    private val saveSelectedPaymentMethod: OnrampSaveSelectedPaymentMethod,
    paramsContainer: ParamsContainer,
) : Model() {

    val state: StateFlow<ProviderListUM> get() = _state.asStateFlow()
    val bottomSheetNavigation: SlotNavigation<ProviderListBottomSheetConfig> = SlotNavigation()
    private val params: SelectProviderComponent.Params = paramsContainer.require()
    private val _state = MutableStateFlow(getInitialState())

    init {
        analyticsEventHandler.send(OnrampAnalyticsEvent.ProvidersScreenOpened)
        getProviders(params.selectedPaymentMethod)
        subscribeToPaymentMethodUpdates()
    }

    private fun subscribeToPaymentMethodUpdates() {
        getOnrampSelectedPaymentMethodUseCase.invoke()
            .onEach { it.getOrNull()?.let(::getProviders) }
            .launchIn(modelScope)
    }

    private fun getProviders(paymentMethod: OnrampPaymentMethod) {
        modelScope.launch {
            getOnrampProviderWithQuoteUseCase.invoke(paymentMethod)
                .onRight { quotes ->
                    _state.update { state -> state.copy(providers = quotes.toProvidersListItems()) }
                }
                .onLeft { Timber.e(it) }
        }
    }

    private fun getInitialState(): ProviderListUM {
        return ProviderListUM(
            paymentMethod = ProviderListPaymentMethodUM(
                id = params.selectedPaymentMethod.id,
                name = params.selectedPaymentMethod.name,
                imageUrl = params.selectedPaymentMethod.imageUrl,
                onClick = ::openPaymentMethods,
            ),
            providers = emptyList<ProviderListItemUM>().toImmutableList(),
        )
    }

    private fun openPaymentMethods() {
        analyticsEventHandler.send(OnrampAnalyticsEvent.PaymentMethodsScreenOpened)
        modelScope.launch {
            val methods = getSelectedPaymentMethodsUseCase.invoke().getOrNull().orEmpty()
            bottomSheetNavigation.activate(
                ProviderListBottomSheetConfig.PaymentMethods(
                    selectedMethodId = _state.value.paymentMethod.id,
                    paymentMethodsUM = methods.toPaymentUMList(),
                ),
            )
        }
    }

    private fun List<OnrampPaymentMethod>.toPaymentUMList(): List<PaymentMethodUM> = map { method ->
        PaymentMethodUM(
            id = method.id,
            imageUrl = method.imageUrl,
            name = method.name,
            onSelect = { onPaymentMethodSelected(method) },
        )
    }

    private fun onPaymentMethodSelected(paymentMethod: OnrampPaymentMethod) {
        analyticsEventHandler.send(OnrampAnalyticsEvent.OnPaymentMethodChosen(paymentMethod = paymentMethod.name))
        modelScope.launch {
            saveSelectedPaymentMethod.invoke(paymentMethod)
            _state.update { state ->
                val paymentMethodUM = state.paymentMethod.copy(
                    id = paymentMethod.id,
                    name = paymentMethod.name,
                    imageUrl = paymentMethod.imageUrl,
                )
                state.copy(paymentMethod = paymentMethodUM)
            }
            bottomSheetNavigation.dismiss()
        }
    }

    private fun List<OnrampProviderWithQuote>.toProvidersListItems(): ImmutableList<ProviderListItemUM> {
        val sorted = sortByRate()

        val bestProvider = sorted.firstOrNull() as? OnrampProviderWithQuote.Data
        return sorted.mapIndexed { index, quote ->
            when (quote) {
                is OnrampProviderWithQuote.Data -> {
                    val rate = quote.toAmount.value.format {
                        crypto(symbol = quote.toAmount.symbol, decimals = quote.toAmount.decimals)
                    }
                    val rateDiff = bestProvider?.toAmount?.value?.let { bestRate ->
                        BigDecimal.ONE - quote.toAmount.value / bestRate
                    }
                    val isSelectedProvider = quote.provider.id == params.selectedProviderId
                    val isSelectedPayment = quote.paymentMethod.id == params.selectedPaymentMethod.id
                    ProviderListItemUM.Available(
                        providerId = quote.provider.id,
                        imageUrl = quote.provider.info.imageLarge,
                        name = quote.provider.info.name,
                        rate = rate,
                        isSelected = isSelectedProvider && isSelectedPayment,
                        isBestRate = quote == bestProvider,
                        diffRate = stringReference("$MINUS${rateDiff.format { percent() }}"),
                        onClick = {
                            analyticsEventHandler.send(
                                OnrampAnalyticsEvent.OnProviderChosen(
                                    providerName = quote.provider.info.name,
                                    tokenSymbol = params.cryptoCurrency.symbol,
                                ),
                            )
                            params.onProviderClick(quote, bestProvider == quote)
                            params.onDismiss()
                        },
                    )
                }
                is OnrampProviderWithQuote.Unavailable.AvailableFrom -> {
                    val amount = quote.requiredAmount.value.format {
                        crypto(symbol = quote.requiredAmount.symbol, decimals = quote.requiredAmount.decimals)
                    }
                    ProviderListItemUM.Unavailable(
                        providerId = quote.provider.id,
                        imageUrl = quote.provider.info.imageLarge,
                        name = quote.provider.info.name,
                        subtitle = resourceReference(R.string.express_provider_min_amount, wrappedList(amount)),
                    )
                }
                is OnrampProviderWithQuote.Unavailable.AvailableUpTo -> {
                    val amount = quote.requiredAmount.value.format {
                        crypto(symbol = quote.requiredAmount.symbol, decimals = quote.requiredAmount.decimals)
                    }
                    ProviderListItemUM.Unavailable(
                        providerId = quote.provider.id,
                        imageUrl = quote.provider.info.imageLarge,
                        name = quote.provider.info.name,
                        subtitle = resourceReference(R.string.express_provider_max_amount, wrappedList(amount)),
                    )
                }
                is OnrampProviderWithQuote.Unavailable.NotSupportedPaymentMethod -> {
                    ProviderListItemUM.Unavailable(
                        providerId = quote.provider.id,
                        imageUrl = quote.provider.info.imageLarge,
                        name = quote.provider.info.name,
                        subtitle = stringReference(
                            "Available with ${quote.availablePaymentMethods.joinToString { it.name }}",
                        ),
                    )
                }
            }
        }.toImmutableList()
    }

    /**
     * Sorting providers by rule:
     *
     * 1. Highest rate
     * 2. Smallest difference between entered amount and required min/max amount
     */
    private fun List<OnrampProviderWithQuote>.sortByRate() = sortedByDescending {
        when (it) {
            is OnrampProviderWithQuote.Data -> it.toAmount.value

            // negative difference to sort both when data and unavailable is present
            is OnrampProviderWithQuote.Unavailable.AvailableFrom -> it.fromAmount.value - it.requiredAmount.value
            is OnrampProviderWithQuote.Unavailable.AvailableUpTo -> it.requiredAmount.value - it.fromAmount.value
            is OnrampProviderWithQuote.Unavailable.NotSupportedPaymentMethod -> null
        }
    }
}