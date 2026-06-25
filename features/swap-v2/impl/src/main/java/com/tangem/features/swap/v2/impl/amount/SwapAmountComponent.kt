package com.tangem.features.swap.v2.impl.amount

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.essenty.lifecycle.subscribe
import com.tangem.common.ui.navigationButtons.NavigationModelCallback
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.AppBarWithBackButtonAndIcon
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.features.swap.v2.impl.R
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.model.SwapAmountModel
import com.tangem.features.swap.v2.impl.amount.ui.SwapAmountContent
import com.tangem.features.swap.v2.impl.sendviaswap.rateinfo.SwapRateInfoComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

internal class SwapAmountComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: SwapAmountComponentParams.AmountParams,
) : ComposableModularContentComponent, AppComponentContext by appComponentContext {

    private val model: SwapAmountModel = getOrCreateModel(params = params)

    private val rateInfoSlot = childSlot(
        source = model.rateInfoNavigation,
        key = "rateInfoSlot",
        serializer = null,
        handleBackButton = true,
        childFactory = ::rateInfoChild,
    )

    init {
        lifecycle.subscribe(
            onStart = model::onStart,
            onStop = model::onStop,
        )
    }

    fun updateState(amountUM: SwapAmountUM) = model.updateState(amountUM)

    @Composable
    override fun Title() {
        AppBarWithBackButtonAndIcon(
            text = stringResourceSafe(R.string.send_amount_label),
            onBackClick = {
                params.callback.onBackClick(params.route)
            },
            backIconRes = if (params.route.isEditMode) {
                R.drawable.ic_back_24
            } else {
                R.drawable.ic_close_24
            },
            backgroundColor = TangemTheme.colors.background.tertiary,
            modifier = Modifier.height(TangemTheme.dimens.size56),
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val amountUM by model.uiState.collectAsStateWithLifecycle()
        val rateInfo by rateInfoSlot.subscribeAsState()

        SwapAmountContent(
            amountUM = amountUM,
            modifier = Modifier.background(TangemTheme.colors.background.tertiary),
            clickIntents = model,
        )

        rateInfo.child?.instance?.BottomSheet()
    }

    @Composable
    override fun Footer() {
        val state by model.uiState.collectAsStateWithLifecycle()
        PrimaryButton(
            text = if (params.route.isEditMode) {
                stringResourceSafe(R.string.common_continue)
            } else {
                stringResourceSafe(R.string.common_next)
            },
            enabled = state.isPrimaryButtonEnabled,
            onClick = {
                model.onAmountNext()
                params.callback.onNextClick(params.route)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
        )
    }

    private fun rateInfoChild(
        config: ExpressRateType,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent {
        return SwapRateInfoComponent(
            appComponentContext = childByContext(componentContext),
            expressRateType = config,
            onDismiss = { model.rateInfoNavigation.dismiss() },
        )
    }

    interface ModelCallback : NavigationModelCallback {
        fun onAmountResult(amountUM: SwapAmountUM)
        fun onSeparatorClick(lastAmount: String, isEnterInFiatSelected: Boolean)
        fun resetSendWithSwapNavigation(resetNavigation: Boolean)
    }
}