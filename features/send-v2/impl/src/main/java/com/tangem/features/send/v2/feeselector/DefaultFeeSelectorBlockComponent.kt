package com.tangem.features.send.v2.feeselector

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.extensions.conditional
import com.tangem.features.send.v2.api.FeeSelectorBlockComponent
import com.tangem.features.send.v2.api.FeeSelectorComponent
import com.tangem.features.send.v2.api.SendFeatureToggles
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import com.tangem.features.send.v2.feeselector.model.FeeSelectorBlockModel
import com.tangem.features.send.v2.feeselector.ui.FeeSelectorBlockContent
import com.tangem.utils.extensions.isSingleItem
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.builtins.serializer

internal class DefaultFeeSelectorBlockComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: FeeSelectorParams.FeeSelectorBlockParams,
    @Assisted onResult: (feeSelectorUM: FeeSelectorUM) -> Unit,
    private val feeSelectorComponentFactory: FeeSelectorComponent.Factory,
    private val sendFeatureToggles: SendFeatureToggles,
) : FeeSelectorBlockComponent, AppComponentContext by appComponentContext {

    private val model: FeeSelectorBlockModel = getOrCreateModel(params = params)

    private val bottomSheetSlot = childSlot(
        source = model.feeSelectorBottomSheet,
        serializer = Unit.serializer(),
        handleBackButton = false,
        childFactory = { _, componentContext ->
            feeSelectorComponentFactory.create(
                context = childByContext(componentContext),
                params = FeeSelectorParams.FeeSelectorDetailsParams(
                    state = model.uiState.value,
                    onLoadFee = params.onLoadFee,
                    onLoadFeeExtended = params.onLoadFeeExtended,
                    feeCryptoCurrencyStatus = params.feeCryptoCurrencyStatus,
                    cryptoCurrencyStatus = params.cryptoCurrencyStatus,
                    callback = model,
                    feeStateConfiguration = params.feeStateConfiguration,
                    feeDisplaySource = FeeSelectorParams.FeeDisplaySource.Screen,
                    analyticsCategoryName = params.analyticsCategoryName,
                    analyticsSendSource = params.analyticsSendSource,
                    userWalletId = params.userWalletId,
                ),
                onDismiss = {
                    model.feeSelectorBottomSheet.dismiss()
                },
            )
        },
    )

    init {
        bottomSheetSlot.subscribe {
            params.bottomSheetShown(it.child != null)
        }

        model.uiState
            .onEach { onResult(it) }
            .launchIn(componentScope)
    }

    override fun updateState(feeSelectorUM: FeeSelectorUM) {
        model.updateState(feeSelectorUM)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        val isScreenSource = params.feeDisplaySource == FeeSelectorParams.FeeDisplaySource.Screen
        val isNotSingleFee = (state as? FeeSelectorUM.Content)?.feeItems?.isSingleItem() == false
        val isGaslessAvailable = state is FeeSelectorUM.Content &&
            (state as FeeSelectorUM.Content).feeExtraInfo.transactionFeeExtended != null
        FeeSelectorBlockContent(
            state = state,
            onReadMoreClick = model::onReadMoreClicked,
            isGaslessFeatureEnabled = sendFeatureToggles.isGaslessTransactionsEnabled,
            modifier = modifier
                .conditional(isScreenSource && (isNotSingleFee || isGaslessAvailable)) {
                    Modifier.clickable {
                        model.showFeeSelector()
                    }
                },
        )
        bottomSheet.child?.instance?.BottomSheet()
    }

    @AssistedFactory
    interface Factory : FeeSelectorBlockComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: FeeSelectorParams.FeeSelectorBlockParams,
            onResult: (feeSelectorUM: FeeSelectorUM) -> Unit,
        ): DefaultFeeSelectorBlockComponent
    }
}