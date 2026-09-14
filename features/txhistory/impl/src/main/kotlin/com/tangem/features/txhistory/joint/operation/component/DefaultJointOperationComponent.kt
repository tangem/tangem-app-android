package com.tangem.features.txhistory.joint.operation.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.joint.operation.JointOperationComponent
import com.tangem.features.txhistory.joint.operation.converter.JointOperationUMConverter
import com.tangem.features.txhistory.joint.operation.model.JointOperationModel
import com.tangem.features.txhistory.joint.operation.ui.JointOperationContentSheet
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

internal class DefaultJointOperationComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: JointOperationComponent.Params,
) : JointOperationComponent, AppComponentContext by appComponentContext {

    private val model: JointOperationModel = getOrCreateModel(params)

    private val converter = JointOperationUMConverter(
        currency = params.currency,
        onSignClick = model::onSignClick,
        onCopyRecipientClick = model::onCopyRecipientClick,
    )

    private val state: StateFlow<TxHistoryDetailsUM.SingleAsset?> = combine(
        flow = model.state,
        flow2 = model.isSigning,
        flow3 = model.hasAnswered,
        flow4 = model.isBalanceHidden,
    ) { state, isSigning, hasAnswered, isBalanceHidden ->
        converter.convert(
            value = JointOperationUMConverter.Input(
                state = state,
                isSigning = isSigning,
                hasAnswered = hasAnswered,
                isBalanceHidden = isBalanceHidden,
            ),
        )
    }
        .stateIn(componentScope, SharingStarted.WhileSubscribed(), initialValue = null)

    init {
        // Executed or cancelled by someone else: nothing left to show or sign
        model.isGone
            .filter { it }
            .onEach { dismiss() }
            .launchIn(componentScope)
    }

    @Composable
    override fun BottomSheet() {
        val uiState by state.collectAsStateWithLifecycle()

        JointOperationContentSheet(state = uiState, onCloseClick = ::dismiss)
    }

    override fun dismiss() = params.onDismiss()

    @AssistedFactory
    interface Factory : JointOperationComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: JointOperationComponent.Params,
        ): DefaultJointOperationComponent
    }
}