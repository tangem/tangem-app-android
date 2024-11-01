package com.tangem.features.onboarding.v2.entry.impl.model

import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.features.onboarding.v2.TitleProvider
import com.tangem.features.onboarding.v2.entry.OnboardingEntryComponent
import com.tangem.features.onboarding.v2.entry.impl.model.state.OnboardingState
import com.tangem.features.onboarding.v2.entry.impl.routing.OnboardingRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@ComponentScoped
internal class OnboardingEntryModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<OnboardingEntryComponent.Params>()

    val titleProvider = object : TitleProvider {
        override val currentTitle = MutableStateFlow(stringReference(""))
        override fun changeTitle(text: TextReference) {
            currentTitle.value = text
        }
    }

    val state = MutableStateFlow(
        OnboardingState(
            currentRoute = routeByProductType(params.scanResponse),
        ),
    )

    private fun routeByProductType(scanResponse: ScanResponse): OnboardingRoute {
        return when (scanResponse.productType) {
            ProductType.Note -> TODO()
            ProductType.Twins -> TODO()
            ProductType.Wallet -> OnboardingRoute.Wallet12(
                scanResponse = scanResponse,
                withSeedPhraseFlow = false,
                titleProvider = titleProvider,
            )
            ProductType.Wallet2 -> OnboardingRoute.Wallet12(
                scanResponse = scanResponse,
                withSeedPhraseFlow = true,
                titleProvider = titleProvider,
            )
            ProductType.Start2Coin -> TODO()
            ProductType.Ring -> TODO()
            ProductType.Visa -> TODO()
        }
    }
}