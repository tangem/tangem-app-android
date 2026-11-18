package com.tangem.features.earn.impl.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnResume
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.api.EarnEntrySource
import com.tangem.features.earn.api.ui.EarnRowInfoModal
import com.tangem.features.earn.impl.getfunds.EarnGetTokenSheet
import com.tangem.features.earn.impl.getfunds.EarnGetTokenSheetComponent
import com.tangem.features.earn.impl.incentive.EarnIncentiveSheet
import com.tangem.features.earn.impl.incentive.EarnIncentiveSheetComponent
import com.tangem.features.earn.impl.picker.model.EarnPickerModel
import com.tangem.features.earn.impl.picker.ui.EarnPickerContent
import com.tangem.features.earn.impl.widgets.calculator.EarnCalculatorComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

internal class EarnYieldPickerComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: Params,
    calculatorFactory: EarnCalculatorComponent.Factory,
    incentiveSheetFactory: EarnIncentiveSheetComponent.Factory,
    getTokenSheetFactory: EarnGetTokenSheetComponent.Factory,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: EarnPickerModel = getOrCreateModel(params)

    private val calculator = calculatorFactory.create(
        context = child(key = EarnCalculatorComponent.KEY),
        params = EarnCalculatorComponent.Params(
            projection = model.projection,
            incentiveRate = model.incentiveRate,
            callback = model,
        ),
    )

    private val incentiveSheet = EarnIncentiveSheet(context = context, sheetFactory = incentiveSheetFactory)

    private val getTokenSheet = EarnGetTokenSheet(context = context, sheetFactory = getTokenSheetFactory)

    init {
        componentScope.launch { model.incentiveSheetRequests.collect(incentiveSheet::show) }
        componentScope.launch { model.getTokenSheetRequests.collect(getTokenSheet::show) }
        lifecycle.doOnResume { model.setScreenResumed(isResumed = true) }
        lifecycle.doOnPause { model.setScreenResumed(isResumed = false) }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val heroVideo by model.heroVideoState.collectAsStateWithLifecycle()

        EarnPickerContent(
            state = state,
            heroVideo = heroVideo.surface,
            onHeroVisibilityChange = model::setHeroVisible,
            modifier = modifier,
            calculator = { calculator.Content(modifier = Modifier) },
        )

        incentiveSheet.BottomSheet()
        getTokenSheet.BottomSheet()

        val rowInfo by model.rowInfo.collectAsStateWithLifecycle()
        rowInfo?.let { um -> EarnRowInfoModal(state = um, onDismiss = model::onRowInfoDismiss) }
    }

    /**
     * @property preselected the product the user came from, `null` selects the first one
     * @property shouldRedirectToActive `Auto`: a currency with an active product opens its Info instead of the picker
     */
    data class Params(
        val userWalletId: UserWalletId,
        val cryptoCurrency: CryptoCurrency,
        val source: EarnEntrySource,
        val preselected: EarnProductId?,
        val shouldRedirectToActive: Boolean,
    )

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): EarnYieldPickerComponent
    }
}