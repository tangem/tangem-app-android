package com.tangem.features.jointaccount.main.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.jointaccount.main.JointAccountMembersComponent
import com.tangem.features.jointaccount.main.entity.MemberCardConfig
import com.tangem.features.jointaccount.main.model.JointAccountMembersModel
import com.tangem.features.jointaccount.main.ui.JointAccountMembersScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultJointAccountMembersComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: JointAccountMembersComponent.Params,
) : JointAccountMembersComponent, AppComponentContext by appComponentContext {

    private val model: JointAccountMembersModel = getOrCreateModel(params = params)

    private val memberCardSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        key = "memberCardSlot",
        childFactory = ::memberCardChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val memberCard by memberCardSlot.subscribeAsState()

        JointAccountMembersScreen(state = state, modifier = modifier)

        memberCard.child?.instance?.BottomSheet()
    }

    private fun memberCardChild(
        config: MemberCardConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = MemberCardComponent(
        appComponentContext = childByContext(componentContext),
        params = MemberCardComponent.Params(
            avatar = config.avatar,
            name = config.name,
            address = stringReference(config.address),
            onCopyClick = { model.onCopyAddressClick(config.address) },
            onDismiss = model.bottomSheetNavigation::dismiss,
        ),
    )

    @AssistedFactory
    interface Factory : JointAccountMembersComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: JointAccountMembersComponent.Params,
        ): DefaultJointAccountMembersComponent
    }
}