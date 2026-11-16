package com.tangem.features.jointaccount.main.component

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnResume
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.utils.findActivity
import com.tangem.features.jointaccount.main.model.JointAccountMembersListParams
import com.tangem.features.jointaccount.main.model.JointAccountMembersModel
import com.tangem.features.jointaccount.main.model.PushPermissionResult
import com.tangem.features.jointaccount.main.ui.JointAccountMembersScreen
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION

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
        val hapticManager = LocalHapticManager.current
        LaunchedEffect(model) {
            model.memberStatusChanges.collect { hapticManager.perform(TangemHapticEffect.OneTime.Click) }
        }
        val requestPushPermission = rememberPushPermissionRequester(onResult = model::onPushPermissionResult)
        LaunchedEffect(model) {
            model.pushPermissionRequests.collect { requestPushPermission() }
        }

        JointAccountMembersScreen(state = state, modifier = modifier)

        bottomSheet.child?.instance?.BottomSheet()
    }
}

@Composable
private fun rememberPushPermissionRequester(onResult: (PushPermissionResult) -> Unit): () -> Unit {
    val activity = LocalContext.current.findActivity()
    val currentOnResult by rememberUpdatedState(onResult)
    var wasRationaleShowable by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        val isRationaleShowable = activity.shouldShowRequestPermissionRationale(PUSH_PERMISSION)
        currentOnResult(
            when {
                isGranted -> PushPermissionResult.Granted
                !wasRationaleShowable && !isRationaleShowable -> PushPermissionResult.Blocked
                else -> PushPermissionResult.Denied
            },
        )
    }
    return {
        wasRationaleShowable = activity.shouldShowRequestPermissionRationale(PUSH_PERMISSION)
        launcher.launch(PUSH_PERMISSION)
    }
}