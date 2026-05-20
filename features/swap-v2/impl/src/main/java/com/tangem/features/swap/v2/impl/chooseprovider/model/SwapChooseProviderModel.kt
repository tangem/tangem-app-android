package com.tangem.features.swap.v2.impl.chooseprovider.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.express.models.ProviderFilterType
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.settings.usercountry.models.needApplyFCARestrictions
import com.tangem.features.swap.v2.api.SwapFeatureToggles
import com.tangem.features.swap.v2.impl.chooseprovider.SwapChooseProviderComponent
import com.tangem.features.swap.v2.impl.chooseprovider.entity.SwapChooseProviderBottomSheetContent
import com.tangem.features.swap.v2.impl.chooseprovider.model.converter.SwapProviderListItemConverter
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.features.swap.v2.impl.common.isRestrictedByFCA
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.isSingleItem
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class SwapChooseProviderModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val swapFeatureToggles: SwapFeatureToggles,
) : Model() {

    private val params: SwapChooseProviderComponent.Params = paramsContainer.require()

    private val isNeedApplyFCARestrictions = params.userCountry.needApplyFCARestrictions()

    private val swapProviderListItemConverter by lazy(LazyThreadSafetyMode.NONE) {
        SwapProviderListItemConverter(
            fromCryptoCurrency = params.fromCryptoCurrency,
            toCryptoCurrency = params.toCryptoCurrency,
            amountType = params.amountType,
            selectedProvider = params.selectedProvider,
            isNeedApplyFCARestrictions = isNeedApplyFCARestrictions,
            needBestRateBadge = params.providers.filterIsInstance<SwapQuoteUM.Content>().isSingleItem().not(),
        )
    }

    val uiState: StateFlow<SwapChooseProviderBottomSheetContent>
        field: MutableStateFlow<SwapChooseProviderBottomSheetContent> = MutableStateFlow(getInitialState())

    fun onProviderClick(quoteUM: SwapQuoteUM) {
        params.callback.onProviderResult(quoteUM)
        params.onDismiss()
    }

    fun onFilterSelect(filterType: ProviderFilterType) {
        val filteredProviders = getDisplayableProviders(params.providers)
            .filter { matchesTypeFilter(it, filterType) }
        uiState.value = uiState.value.copy(
            providerList = swapProviderListItemConverter.convertList(filteredProviders)
                .filterNotNull()
                .toPersistentList(),
            selectedFilter = filterType,
        )
    }

    private fun getInitialState(): SwapChooseProviderBottomSheetContent {
        val displayableProviders = getDisplayableProviders(params.providers)
        val hasCex = displayableProviders.any { it.provider?.type == ExpressProviderType.CEX }
        val hasDex = displayableProviders.any {
            it.provider?.type == ExpressProviderType.DEX || it.provider?.type == ExpressProviderType.DEX_BRIDGE
        }
        val availableFilters = if (swapFeatureToggles.isSwapProviderFilterEnabled && hasCex && hasDex) {
            persistentListOf(ProviderFilterType.ALL, ProviderFilterType.CEX, ProviderFilterType.DEX)
        } else {
            persistentListOf()
        }
        return SwapChooseProviderBottomSheetContent(
            isApplyFCARestrictions = isNeedApplyFCARestrictions && params.selectedProvider.isRestrictedByFCA(),
            providerList = swapProviderListItemConverter.convertList(displayableProviders)
                .filterNotNull()
                .toPersistentList(),
            selectedProvider = params.selectedProvider,
            selectedFilter = ProviderFilterType.ALL,
            availableFilters = availableFilters,
        )
    }

    private fun getDisplayableProviders(allProviders: List<SwapQuoteUM>): List<SwapQuoteUM> {
        return allProviders.filter { swapQuoteUM ->
            swapQuoteUM is SwapQuoteUM.Content ||
                swapQuoteUM is SwapQuoteUM.Allowance ||
                (swapQuoteUM as? SwapQuoteUM.Error)?.expressError is ExpressError.AmountError
        }
    }

    private fun matchesTypeFilter(quote: SwapQuoteUM, filterType: ProviderFilterType): Boolean {
        val type = quote.provider?.type ?: return filterType == ProviderFilterType.ALL
        return when (filterType) {
            ProviderFilterType.ALL -> true
            ProviderFilterType.CEX -> type == ExpressProviderType.CEX
            ProviderFilterType.DEX -> type == ExpressProviderType.DEX || type == ExpressProviderType.DEX_BRIDGE
        }
    }
}