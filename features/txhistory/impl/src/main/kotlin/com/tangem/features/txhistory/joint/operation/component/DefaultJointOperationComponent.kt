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
import com.tangem.features.txhistory.joint.operation.JointOperationComponent
import com.tangem.features.txhistory.joint.operation.model.JointOperationModel
import com.tangem.features.txhistory.joint.operation.route.JointOperationRoutes
import com.tangem.features.txhistory.joint.operation.signers.component.JointOperationSignersScreenComponent
import com.tangem.features.txhistory.joint.operation.ui.JointOperationContentSheet
import com.tangem.features.txhistory.joint.operation.ui.state.JointOperationUM
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class DefaultJointOperationComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: JointOperationComponent.Params,
) : JointOperationComponent, AppComponentContext by appComponentContext {

    private val model: JointOperationModel = getOrCreateModel(params)

    private val innerRouter = InnerRouter<JointOperationRoutes>(
        stackNavigation = model.stackNavigation,
        popCallback = { onChildBack() },
    )

    private val contentStack = childStack(
        key = "JointOperationStack",
        source = model.stackNavigation,
        serializer = JointOperationRoutes.serializer(),
        initialConfiguration = JointOperationRoutes.Operation,
        handleBackButton = false,
        childFactory = ::screenChild,
    )

    init {
        model.uiState
            .filterIsInstance<JointOperationUM.Gone>()
            .onEach { dismiss() }
            .launchIn(componentScope)
    }

    @Composable
    override fun BottomSheet() {
        val stack by contentStack.subscribeAsState()
        val uiState by model.uiState.collectAsStateWithLifecycle()

        JointOperationContentSheet(
            state = uiState as? JointOperationUM.Content,
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
                state = model.uiState,
            )
            JointOperationRoutes.Signers -> JointOperationSignersScreenComponent(
                appComponentContext = childContext,
                state = model.uiState,
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