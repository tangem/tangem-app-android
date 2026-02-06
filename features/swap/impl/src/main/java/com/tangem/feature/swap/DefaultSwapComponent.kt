package com.tangem.feature.swap

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.essenty.lifecycle.subscribe
import com.tangem.common.ui.swapStoriesScreen.SwapStoriesScreen
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.feature.swap.component.SwapFeeSelectorBlockComponent
import com.tangem.feature.swap.model.SwapModel
import com.tangem.feature.swap.models.AddToPortfolioRoute
import com.tangem.feature.swap.router.SwapNavScreen
import com.tangem.feature.swap.ui.SwapScreen
import com.tangem.feature.swap.ui.SwapSelectTokenScreen
import com.tangem.feature.swap.ui.SwapSuccessScreen
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioComponent
import com.tangem.features.send.v2.api.SendFeatureToggles
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.swap.SwapComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("UnusedPrivateMember")
internal class DefaultSwapComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: SwapComponent.Params,
    private val swapFeeSelectorBlockComponentFactory: SwapFeeSelectorBlockComponent.Factory,
    private val sendFeatureToggles: SendFeatureToggles,
    private val addToPortfolioComponentFactory: AddToPortfolioComponent.Factory,
) : SwapComponent, AppComponentContext by appComponentContext {

    private val model: SwapModel = getOrCreateModel(params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = AddToPortfolioRoute.serializer(),
        key = BOTTOM_SHEET_SLOT_KEY,
        handleBackButton = false,
        childFactory = { configuration, context -> bottomSheetChild(context) },
    )

    init {
        lifecycle.subscribe(
            onStart = model::onStart,
            onStop = model::onStop,
        )
    }

    val slotNavigation = SlotNavigation<FeeSelectorConfig>()
    val childSlot = childSlot(
        source = slotNavigation,
        serializer = null,
        key = FEE_SELECTOR_SLOT_KEY,
        childFactory = { config, context ->
            createSwapFeeSelectorBlockComponent(
                context = childByContext(context),
                config = config,
            )
        },
    )

    private fun createSwapFeeSelectorBlockComponent(
        context: AppComponentContext,
        config: FeeSelectorConfig,
    ): SwapFeeSelectorBlockComponent {
        return swapFeeSelectorBlockComponentFactory.create(
            context = context,
            params = SwapFeeSelectorBlockComponent.Params(
                repository = model.feeSelectorRepository,
                userWalletId = params.userWalletId,
                sendingCryptoCurrencyStatus = config.sendingCurrencyStatus,
                feeCryptoCurrencyStatus = config.feeCurrencyStatus,
                analyticsParams = SwapFeeSelectorBlockComponent.AnalyticsParams(
                    analyticsCategoryName = CommonSendAnalyticEvents.SWAP_CATEGORY,
                    analyticsSendSource = CommonSendAnalyticEvents.CommonSendSource.Swap,
                ),
            ),
        )
    }

    data class FeeSelectorConfig(
        val sendingCurrencyStatus: CryptoCurrencyStatus,
        val feeCurrencyStatus: CryptoCurrencyStatus,
    )

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    @Composable
    override fun Content(modifier: Modifier) {
        if (sendFeatureToggles.isGaslessTransactionsEnabled) {
            val dataState by model.dataStateStateFlow.collectAsStateWithLifecycle()
            val fromCryptoCurrency by remember { derivedStateOf { dataState.fromCryptoCurrency } }
            val feePaidCryptoCurrency by remember { derivedStateOf { dataState.feePaidCryptoCurrency } }
            val amount by remember { derivedStateOf { dataState.amount } }

            LaunchedEffect(fromCryptoCurrency, feePaidCryptoCurrency, amount.isNullOrBlank()) {
                if (amount.isNullOrBlank()) {
                    slotNavigation.dismiss()
                    return@LaunchedEffect
                }

                val sendingCryptoCurrencyStatus = fromCryptoCurrency ?: run {
                    slotNavigation.dismiss()
                    return@LaunchedEffect
                }

                val feeCurrencyStatus = feePaidCryptoCurrency ?: run {
                    slotNavigation.dismiss()
                    return@LaunchedEffect
                }

                slotNavigation.activate(
                    FeeSelectorConfig(
                        sendingCurrencyStatus = sendingCryptoCurrencyStatus,
                        feeCurrencyStatus = feeCurrencyStatus,
                    ),
                )
            }
        }

        val feeSelectorChildStackState by childSlot.subscribeAsState()
        val feeSelectorBlockComponent = feeSelectorChildStackState.child?.instance
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        Crossfade(
            modifier = Modifier.background(TangemTheme.colors.background.secondary),
            targetState = model.currentScreen,
            label = "",
        ) { screen ->
            when (screen) {
                SwapNavScreen.PromoStories -> {
                    val storiesConfig = model.uiState.storiesConfig
                    if (storiesConfig != null) {
                        SwapStoriesScreen(config = storiesConfig)
                    } else {
                        SwapScreen(
                            stateHolder = model.uiState,
                            feeSelectorBlockComponent = feeSelectorBlockComponent,
                        )
                    }
                }
                SwapNavScreen.Main -> SwapScreen(
                    stateHolder = model.uiState,
                    feeSelectorBlockComponent = feeSelectorBlockComponent,
                )
                SwapNavScreen.Success -> {
                    val successState = model.uiState.successState
                    val feeSelectorState by model.feeSelectorRepository.state.collectAsStateWithLifecycle()
                    if (successState != null) {
                        SwapSuccessScreen(
                            state = successState,
                            feeSelectorUM = feeSelectorState,
                            onBack = model.uiState.onBackClicked,
                        )
                    } else {
                        SwapScreen(
                            stateHolder = model.uiState,
                            feeSelectorBlockComponent = feeSelectorBlockComponent,
                        )
                    }
                }
                SwapNavScreen.SelectToken -> {
                    val tokenState = model.uiState.selectTokenState
                    if (tokenState != null) {
                        SwapSelectTokenScreen(state = tokenState, onBack = model.uiState.onBackClicked)
                    } else {
                        SwapScreen(
                            stateHolder = model.uiState,
                            feeSelectorBlockComponent = feeSelectorBlockComponent,
                        )
                    }
                }
            }
        }

        bottomSheet.child?.instance?.BottomSheet()
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun bottomSheetChild(componentContext: ComponentContext): ComposableBottomSheetComponent {
        return addToPortfolioComponentFactory.create(
            context = childByContext(componentContext),
            params = AddToPortfolioComponent.Params(
                addToPortfolioManager = model.addToPortfolioManager!!,
                callback = model.addToPortfolioCallback,
                shouldSkipTokenActionsScreen = true,
            ),
        )
    }

    @AssistedFactory
    interface Factory : SwapComponent.Factory {
        override fun create(context: AppComponentContext, params: SwapComponent.Params): DefaultSwapComponent
    }

    private companion object {
        const val BOTTOM_SHEET_SLOT_KEY = "bottomSheetSlot"
        const val FEE_SELECTOR_SLOT_KEY = "feeSelectorSlot"
    }
}