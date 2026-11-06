package com.tangem.features.onramp.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.essenty.lifecycle.subscribe
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.marketing.api.MarketingBannerComponent
import com.tangem.features.marketing.api.concierge.ConciergeDemandSurveyComponent
import com.tangem.features.onramp.alloffers.AllOffersComponent
import com.tangem.features.onramp.confirmresidency.ConfirmResidencyComponent
import com.tangem.features.onramp.main.entity.OnrampMainBottomSheetConfig
import com.tangem.features.onramp.main.model.OnrampMainComponentModel
import com.tangem.features.onramp.main.ui.OnrampMainScreen
import com.tangem.features.onramp.selectcurrency.SelectCurrencyComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("LongParameterList")
internal class DefaultOnrampMainComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: OnrampMainComponent.Params,
    private val confirmResidencyComponentFactory: ConfirmResidencyComponent.Factory,
    private val selectCurrencyComponentFactory: SelectCurrencyComponent.Factory,
    private val allOffersComponentFactory: AllOffersComponent.Factory,
    private val marketingBannerComponentFactory: MarketingBannerComponent.Factory,
    private val conciergeDemandSurveyComponentFactory: ConciergeDemandSurveyComponent.Factory,
) : OnrampMainComponent, AppComponentContext by appComponentContext {

    private val model: OnrampMainComponentModel = getOrCreateModel(params)

    private val marketingBannerComponent: MarketingBannerComponent = marketingBannerComponentFactory.create(
        context = child("marketing_banner"),
        params = MarketingBannerComponent.Params.Standalone(
            requestFlow = model.marketingRequest,
            onDeeplinkClick = model::onMarketingBannerDeeplink,
        ),
    )

    private val linkedMarketingBannerComponent: MarketingBannerComponent = marketingBannerComponentFactory.create(
        context = child("marketing_banner_linked"),
        params = MarketingBannerComponent.Params.Linked(requestFlow = model.linkedMarketingRequest),
    )

    init {
        lifecycle.subscribe(onStart = model::onStart, onStop = model::onStop)
    }

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    private val demandSurveyRouter: Router = object : Router by router {
        override fun pop(onComplete: (isSuccess: Boolean) -> Unit) {
            model.demandSurveyNavigation.dismiss(onComplete)
        }
    }

    private val demandSurveySlot = childSlot(
        source = model.demandSurveyNavigation,
        key = "demandSurveySlot",
        serializer = null,
        handleBackButton = false,
        childFactory = { surveyParams, componentContext ->
            conciergeDemandSurveyComponentFactory.create(
                context = childByContext(componentContext = componentContext, router = demandSurveyRouter),
                params = surveyParams,
            )
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsState()
        val bottomSheet by bottomSheetSlot.subscribeAsState()
        val demandSurvey by demandSurveySlot.subscribeAsState()

        OnrampMainScreen(
            modifier = modifier,
            state = state,
            marketingBannerComponent = marketingBannerComponent,
            linkedMarketingBannerComponent = linkedMarketingBannerComponent,
        )
        bottomSheet.child?.instance?.BottomSheet()
        demandSurvey.child?.instance?.Content(modifier = Modifier)
    }

    private fun bottomSheetChild(
        config: OnrampMainBottomSheetConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = when (config) {
        is OnrampMainBottomSheetConfig.ConfirmResidency -> confirmResidencyComponentFactory.create(
            context = childByContext(componentContext),
            params = ConfirmResidencyComponent.Params(
                userWalletId = params.userWalletId,
                cryptoCurrency = params.cryptoCurrency,
                country = config.country,
                onDismiss = {
                    model.bottomSheetNavigation.dismiss()
                    model.handleOnrampAvailable()
                },
            ),
        )
        is OnrampMainBottomSheetConfig.CurrenciesList -> selectCurrencyComponentFactory.create(
            context = childByContext(componentContext),
            params = SelectCurrencyComponent.Params(
                userWallet = model.userWallet,
                cryptoCurrency = params.cryptoCurrency,
                onDismiss = model.bottomSheetNavigation::dismiss,
            ),
        )
        is OnrampMainBottomSheetConfig.AllOffers -> allOffersComponentFactory.create(
            context = childByContext(componentContext),
            params = AllOffersComponent.Params(
                userWallet = model.userWallet,
                cryptoCurrency = params.cryptoCurrency,
                onDismiss = model.bottomSheetNavigation::dismiss,
                openRedirectPage = params.openRedirectPage,
                amountCurrencyCode = config.amountCurrencyCode,
                marketingBannerComponent = marketingBannerComponent,
                linkedMarketingBannerComponent = linkedMarketingBannerComponent,
            ),
        )
    }

    @AssistedFactory
    interface Factory : OnrampMainComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: OnrampMainComponent.Params,
        ): DefaultOnrampMainComponent
    }
}