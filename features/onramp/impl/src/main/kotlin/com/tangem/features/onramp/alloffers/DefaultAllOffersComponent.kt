package com.tangem.features.onramp.alloffers

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.onramp.alloffers.model.AllOffersModel
import com.tangem.features.onramp.alloffers.ui.AllOffersContentSheet
import com.tangem.features.onramp.featuretoggles.LocalOnrampDesignSystemEnabled
import com.tangem.features.onramp.featuretoggles.OnrampFeatureToggles
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultAllOffersComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: AllOffersComponent.Params,
    private val onrampFeatureToggles: OnrampFeatureToggles,
) : AllOffersComponent, AppComponentContext by context {

    private val model: AllOffersModel = getOrCreateModel(params)

    override fun dismiss() {
        model.dismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.state.collectAsState()
        CompositionLocalProvider(
            LocalOnrampDesignSystemEnabled provides onrampFeatureToggles.isNewDesignSystemEnabled,
        ) {
            AllOffersContentSheet(
                state = state,
                marketingBannerComponent = params.marketingBannerComponent,
                linkedMarketingBannerComponent = params.linkedMarketingBannerComponent,
                onCloseClick = { dismiss() },
            )
        }
    }

    @AssistedFactory
    interface Factory : AllOffersComponent.Factory {
        override fun create(context: AppComponentContext, params: AllOffersComponent.Params): DefaultAllOffersComponent
    }
}