package com.tangem.features.txhistory.joint.operation.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.childStack
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.joint.operation.JointOperationComponent
import com.tangem.features.txhistory.joint.operation.converter.JointOperationUMConverter
import com.tangem.features.txhistory.joint.operation.model.JointOperationModel
import com.tangem.features.txhistory.joint.operation.route.JointOperationRoutes
import com.tangem.features.txhistory.joint.operation.signers.component.JointOperationSignersScreenComponent
import com.tangem.features.txhistory.joint.operation.signers.converter.JointOperationSignersUMConverter
import com.tangem.features.txhistory.joint.operation.signers.ui.state.JointOperationSignersUM
import com.tangem.features.txhistory.joint.operation.ui.JointOperationContentSheet
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

internal class DefaultJointOperationComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: JointOperationComponent.Params,
) : JointOperationComponent, AppComponentContext by appComponentContext {

    private val model: JointOperationModel = getOrCreateModel(params)

    private val innerRouter = InnerRouter<JointOperationRoutes>(
        stackNavigation = model.stackNavigation,
        popCallback = { onChildBack() },
    )

    private val converter = JointOperationUMConverter(
        currency = params.currency,
        onSignClick = model::onSignClick,
        onSignersClick = model::onSignersClick,
        onCopyRecipientClick = model::onCopyRecipientClick,
    )

    private val operationState: StateFlow<TxHistoryDetailsUM.SingleAsset?> = combine(
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

    private val signersState: StateFlow<JointOperationSignersUM> = model.state
        .map { state ->
            state ?: return@map JointOperationSignersUM.Loading

            JointOperationSignersUMConverter().convert(
                value = JointOperationSignersUMConverter.Input(
                    membersCount = state.membersCount,
                    signers = state.signers,
                ),
            )
        }
        .stateIn(componentScope, SharingStarted.WhileSubscribed(), initialValue = JointOperationSignersUM.Loading)

    private val contentStack = childStack(
        key = "JointOperationStack",
        source = model.stackNavigation,
        serializer = JointOperationRoutes.serializer(),
        initialConfiguration = JointOperationRoutes.Operation,
        handleBackButton = false,
        childFactory = ::screenChild,
    )

    init {
        // Executed or cancelled by someone else: nothing left to show or sign on either screen
        model.isGone
            .filter { it }
            .onEach { dismiss() }
            .launchIn(componentScope)
    }

    @Composable
    override fun BottomSheet() {
        val stack by contentStack.subscribeAsState()
        val operation by operationState.collectAsStateWithLifecycle()
        val signers by signersState.collectAsStateWithLifecycle()

        JointOperationContentSheet(
            header = operation?.header,
            membersCount = (signers as? JointOperationSignersUM.Content)?.membersCount,
            contentStack = stack,
            onBackClick = ::onChildBack,
            onCloseClick = ::dismiss,
        )
    }

    override fun dismiss() = params.onDismiss()

    private fun onChildBack() {
        when (contentStack.value.active.configuration) {
            JointOperationRoutes.Signers -> model.onBackClick()
            JointOperationRoutes.Operation -> dismiss()
        }
    }

    private fun screenChild(
        config: JointOperationRoutes,
        componentContext: ComponentContext,
    ): ComposableContentComponent {
        val childContext = childByContext(componentContext = componentContext, router = innerRouter)

        return when (config) {
            JointOperationRoutes.Operation -> JointOperationDetailComponent(
                appComponentContext = childContext,
                state = operationState,
            )
            JointOperationRoutes.Signers -> JointOperationSignersScreenComponent(
                appComponentContext = childContext,
                state = signersState,
            )
        }
    }

    @AssistedFactory
    interface Factory : JointOperationComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: JointOperationComponent.Params,
        ): DefaultJointOperationComponent
    }
}