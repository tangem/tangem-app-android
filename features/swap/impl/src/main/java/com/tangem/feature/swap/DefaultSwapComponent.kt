package com.tangem.feature.swap

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.feature.swap.component.SwapFeeSelectorBlockComponent
import com.tangem.feature.swap.model.SwapModel
import com.tangem.feature.swap.models.SwapCardState.SwapCardData
import com.tangem.feature.swap.router.SwapNavScreen
import com.tangem.feature.swap.ui.SwapScreen
import com.tangem.feature.swap.ui.SwapSelectTokenScreen
import com.tangem.feature.swap.ui.SwapSuccessScreen
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
) : SwapComponent, AppComponentContext by appComponentContext {

    private val model: SwapModel = getOrCreateModel(params)

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
                    analyticsCategoryName = CommonSendAnalyticEvents.SEND_CATEGORY,
                    analyticsSendSource = CommonSendAnalyticEvents.CommonSendSource.Swap,
                ),
            ),
        )
    }

    data class FeeSelectorConfig(
        val sendingCurrencyStatus: CryptoCurrencyStatus,
        val feeCurrencyStatus: CryptoCurrencyStatus,
    )

    @Suppress("LongMethod")
    @Composable
    override fun Content(modifier: Modifier) {
        if (sendFeatureToggles.isGaslessTransactionsEnabled) {
            val sendCardData = model.uiState.sendCardData as? SwapCardData
            val feePaidCryptoCurrency = model.dataState.feePaidCryptoCurrency
            LaunchedEffect(sendCardData?.token?.currency, feePaidCryptoCurrency?.currency) {
                val sendingCryptoCurrencyStatus = sendCardData?.token ?: run {
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
    }

    @AssistedFactory
    interface Factory : SwapComponent.Factory {
        override fun create(context: AppComponentContext, params: SwapComponent.Params): DefaultSwapComponent
    }
}