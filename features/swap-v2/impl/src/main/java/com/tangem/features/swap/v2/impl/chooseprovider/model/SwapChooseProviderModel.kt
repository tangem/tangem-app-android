package com.tangem.features.swap.v2.impl.chooseprovider.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.settings.usercountry.models.needApplyFCARestrictions
import com.tangem.features.swap.v2.impl.chooseprovider.SwapChooseProviderComponent
import com.tangem.features.swap.v2.impl.chooseprovider.entity.SwapChooseProviderBottomSheetContent
import com.tangem.features.swap.v2.impl.chooseprovider.model.converter.SwapProviderListItemConverter
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.features.swap.v2.impl.common.isRestrictedByFCA
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

    private val needApplyFCARestrictions = params.userCountry.needApplyFCARestrictions()

    private val swapProviderListItemConverter by lazy(LazyThreadSafetyMode.NONE) {
        SwapProviderListItemConverter(
            cryptoCurrency = params.cryptoCurrency,
            selectedProvider = params.selectedProvider,
            needApplyFCARestrictions = needApplyFCARestrictions,
        )
    }

    val uiState: StateFlow<SwapChooseProviderBottomSheetContent>
    field: MutableStateFlow<SwapChooseProviderBottomSheetContent> = MutableStateFlow(getInitialState())

    fun onProviderClick(quoteUM: SwapQuoteUM) {
        params.callback.onProviderResult(quoteUM)
        params.onDismiss()
    }

    private fun getInitialState(): SwapChooseProviderBottomSheetContent {
        return SwapChooseProviderBottomSheetContent(
            isApplyFCARestrictions = needApplyFCARestrictions && params.selectedProvider.isRestrictedByFCA(),
            providerList = swapProviderListItemConverter.convertList(params.providers)
                .filterNotNull()
                .toPersistentList(),
            selectedProvider = params.selectedProvider,
        )
    }
}