package com.tangem.features.polymarket.impl.placeprediction.order

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnResume
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.polymarket.impl.main.addfunds.polymarketFundingSheetSlot
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionComponent
import com.tangem.features.polymarket.impl.placeprediction.SubmittingStep
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionOrderModel
import com.tangem.features.polymarket.impl.placeprediction.summary.PredictionFeeSheetComponent
import com.tangem.features.polymarket.impl.placeprediction.ui.PlacePredictionOrderContent
import com.tangem.features.tokenreceive.TokenReceiveComponent

internal class PlacePredictionOrderComponent(
    appComponentContext: AppComponentContext,
    params: PlacePredictionComponent.Params,
    tokenReceiveComponentFactory: TokenReceiveComponent.Factory,
) : ComposableContentComponent, SubmittingStep, AppComponentContext by appComponentContext {

    private val model: PlacePredictionOrderModel = getOrCreateModel(params = params)

    private val feeSheetSlot = childSlot(
        source = model.feeSheet.navigation,
        serializer = null,
        key = "feeSheet",
        handleBackButton = false,
        childFactory = { _, componentContext ->
            PredictionFeeSheetComponent(
                appComponentContext = childByContext(componentContext),
                feeSheet = model.feeSheet,
            )
        },
    )

    private val fundingSheetSlot = polymarketFundingSheetSlot(
        navigation = model.fundingSheetNavigation,
        callback = model,
        tokenReceiveComponentFactory = tokenReceiveComponentFactory,
    )

    init {
        lifecycle.doOnResume { model.onResume() }
        lifecycle.doOnPause { model.onPause() }
    }

    override fun isSubmitting(): Boolean = model.isSubmitting()

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val feeSheet by feeSheetSlot.subscribeAsState()
        val fundingSheet by fundingSheetSlot.subscribeAsState()

        PlacePredictionOrderContent(
            state = state,
            presets = model.amountPresets,
            intents = model,
            modifier = modifier,
        )

        feeSheet.child?.instance?.BottomSheet()
        fundingSheet.child?.instance?.BottomSheet()
    }
}