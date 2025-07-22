package com.tangem.features.swap.v2.impl.chooseprovider.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.swap.v2.impl.chooseprovider.SwapChooseProviderComponent
import com.tangem.features.swap.v2.impl.chooseprovider.entity.SwapChooseProviderBottomSheetContent
import com.tangem.features.swap.v2.impl.chooseprovider.model.converter.SwapProviderListItemConverter
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class SwapChooseProviderModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params: SwapChooseProviderComponent.Params = paramsContainer.require()

    private val swapProviderListItemConverter by lazy(LazyThreadSafetyMode.NONE) {
        SwapProviderListItemConverter(
            cryptoCurrency = params.cryptoCurrency,
            selectedProvider = params.selectedProvider,
        )
    }

    val uiState: StateFlow<SwapChooseProviderBottomSheetContent>
    field: MutableStateFlow<SwapChooseProviderBottomSheetContent> = MutableStateFlow(getInitialState())

    fun onProviderClick(quoteUM: SwapQuoteUM) {
        params.callback.onProviderResult(quoteUM)
    }

    private fun getInitialState(): SwapChooseProviderBottomSheetContent {
        return SwapChooseProviderBottomSheetContent(
            providerList = swapProviderListItemConverter.convertList(params.providers)
                .filterNotNull()
                .toPersistentList(),
            selectedProvider = params.selectedProvider,
        )
    }
}