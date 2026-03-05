package com.tangem.features.staking.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.approval.api.GiveApprovalComponent
import com.tangem.features.staking.api.StakingComponent
import com.tangem.features.staking.impl.presentation.model.StakingModel
import com.tangem.features.staking.impl.presentation.ui.StakingScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultStakingComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: StakingComponent.Params,
    private val giveApprovalComponentFactory: GiveApprovalComponent.Factory,
) : StakingComponent, AppComponentContext by appComponentContext {

    private val model: StakingModel = getOrCreateModel(params)

    private val approvalSlot = childSlot(
        key = "stakingApprovalSlot",
        source = model.approvalSlotNavigation,
        serializer = null,
        handleBackButton = true,
        childFactory = { _, factoryContext ->
            val approvalParams = model.getApprovalParams()
                ?: error("Approval params are not available")
            giveApprovalComponentFactory.create(
                context = childByContext(factoryContext),
                params = approvalParams,
            )
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val currentState by model.uiState.collectAsStateWithLifecycle()
        val approvalSlotState by approvalSlot.subscribeAsState()

        StakingScreen(currentState)

        approvalSlotState.child?.instance?.BottomSheet()
    }

    @AssistedFactory
    interface Factory : StakingComponent.Factory {
        override fun create(context: AppComponentContext, params: StakingComponent.Params): DefaultStakingComponent
    }
}