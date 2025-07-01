package com.tangem.features.swap.v2.impl.amount

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.swap.v2.impl.amount.SwapAmountComponentParams.AmountBlockParams
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.model.SwapAmountModel
import com.tangem.features.swap.v2.impl.amount.ui.SwapAmountBlockContent
import com.tangem.features.swap.v2.impl.chooseprovider.SwapChooseProviderComponent
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class SwapAmountBlockComponent(
    private val appComponentContext: AppComponentContext,
    private val params: AmountBlockParams,
    private val onResult: (SwapAmountUM) -> Unit,
    val onClick: () -> Unit,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: SwapAmountModel = getOrCreateModel(params = params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = true,
        childFactory = ::bottomSheetChild,
    )

    init {
        model.uiState.onEach {
            onResult(it)
        }.launchIn(componentScope)
    }

    fun updateState(amountUM: SwapAmountUM) = model.updateState(amountUM)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        val isClickEnabled by params.blockClickEnableFlow.collectAsStateWithLifecycle()

        SwapAmountBlockContent(
            amountUM = state,
            onInfoClick = model::onInfoClick,
            isClickEnabled = isClickEnabled,
            onClick = onClick,
            onProviderSelectClick = {
                val amountUM = model.uiState.value as? SwapAmountUM.Content ?: return@SwapAmountBlockContent
                val selectedProvider = amountUM.selectedQuote.provider ?: return@SwapAmountBlockContent
                val cryptoCurrency = params.secondaryCryptoCurrency ?: return@SwapAmountBlockContent

                model.bottomSheetNavigation.activate(
                    SwapChooseProviderConfig(
                        providers = amountUM.swapQuotes,
                        cryptoCurrency = cryptoCurrency,
                        selectedProvider = selectedProvider,
                    ),
                )
            },
        )

        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: SwapChooseProviderConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent {
        return SwapChooseProviderComponent(
            context = childByContext(componentContext),
            params = SwapChooseProviderComponent.Params(
                providers = config.providers,
                cryptoCurrency = config.cryptoCurrency,
                selectedProvider = config.selectedProvider,
                callback = model,
                onDismiss = { model.bottomSheetNavigation.dismiss() },
            ),
        )
    }

    data class SwapChooseProviderConfig(
        val providers: ImmutableList<SwapQuoteUM>,
        val cryptoCurrency: CryptoCurrency,
        val selectedProvider: ExpressProvider,
    )
}