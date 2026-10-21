package com.tangem.features.jointaccount.main.component

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
import com.tangem.features.jointaccount.main.model.JointAccountMembersListParams
import com.tangem.features.jointaccount.main.model.JointAccountMembersModel
import com.tangem.features.jointaccount.main.ui.JointAccountMembersScreen

internal class JointAccountMembersListComponent(
    appComponentContext: AppComponentContext,
    params: JointAccountMembersListParams,
    childFactory: JointAccountMembersChildFactory,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: JointAccountMembersModel = getOrCreateModel(params = params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
        key = "bottomSheetSlot",
        childFactory = { config, componentContext ->
            childFactory.createChild(
                config = config,
                appComponentContext = childByContext(componentContext),
                model = model,
            )
        },
    )

    init {
        lifecycle.doOnResume { model.onResume() }
        lifecycle.doOnPause { model.onPause() }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        JointAccountMembersScreen(state = state, modifier = modifier)

        bottomSheet.child?.instance?.BottomSheet()
    }
}