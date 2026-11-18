package com.tangem.features.earn.impl.info

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.api.EarnEntrySource
import com.tangem.features.earn.api.ui.EarnHowItWorksModal
import com.tangem.features.earn.api.ui.EarnRowInfoModal
import com.tangem.features.earn.impl.info.model.EarnInfoModel
import com.tangem.features.earn.impl.info.ui.EarnInfoContent
import com.tangem.features.earn.impl.widgets.calculator.EarnCalculatorComponent
import com.tangem.features.earn.impl.widgets.calculator.EarnCalculatorSheetComponent
import com.tangem.features.earn.impl.widgets.chart.EarnApyChartComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class EarnInfoComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: Params,
    private val chartFactory: EarnApyChartComponent.Factory,
    calculatorFactory: EarnCalculatorComponent.Factory,
    private val calculatorSheetFactory: EarnCalculatorSheetComponent.Factory,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: EarnInfoModel = getOrCreateModel(params)

    private val calculator = calculatorFactory.create(
        context = child(key = EarnCalculatorComponent.KEY),
        params = EarnCalculatorComponent.Params(
            projection = model.projection,
            incentiveRate = model.incentiveRate,
            callback = model,
        ),
    )

    private val chartSlot = childSlot(
        key = EarnApyChartComponent.KEY,
        source = model.chartNavigation,
        serializer = null,
        initialConfiguration = { EarnInfoChartConfig.takeIf { model.isChartActivated } },
        handleBackButton = false,
        childFactory = { _, factoryContext ->
            chartFactory.create(
                context = childByContext(factoryContext),
                params = model.chartParams(),
            )
        },
    )

    private val calculatorSheetSlot = childSlot(
        key = EarnCalculatorSheetComponent.KEY,
        source = model.calculatorSheetNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = { _, factoryContext ->
            calculatorSheetFactory.create(
                context = childByContext(factoryContext),
                params = EarnCalculatorSheetComponent.Params(
                    calculator = calculator,
                    callback = model,
                ),
            )
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val pullToRefresh by model.pullToRefresh.collectAsStateWithLifecycle()
        val chart = chartSlot.subscribeAsState().value.child?.instance
        val calculatorSheet = calculatorSheetSlot.subscribeAsState().value.child?.instance

        Box(modifier = modifier.fillMaxSize()) {
            EarnInfoContent(
                state = state,
                chart = chart?.let { component -> { component.Content(modifier = Modifier) } },
                pullToRefresh = pullToRefresh,
                modifier = Modifier.fillMaxSize(),
            )

            calculatorSheet?.BottomSheet()
        }

        val howItWorks by model.howItWorks.collectAsStateWithLifecycle()
        howItWorks?.let { um -> EarnHowItWorksModal(state = um, onDismiss = model::onHowItWorksDismiss) }

        val rowInfo by model.rowInfo.collectAsStateWithLifecycle()
        rowInfo?.let { um -> EarnRowInfoModal(state = um, onDismiss = model::onRowInfoDismiss) }
    }

    data class Params(
        val userWalletId: UserWalletId,
        val cryptoCurrency: CryptoCurrency,
        val productId: EarnProductId,
        val source: EarnEntrySource,
    )

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): EarnInfoComponent
    }
}

internal data object EarnInfoChartConfig