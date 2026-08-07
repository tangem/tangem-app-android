package com.tangem.tap.routing.startup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.appupdate.model.AppUpdateState
import com.tangem.domain.appupdate.usecase.GetAppUpdateStateUseCase
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.features.forceupdate.ForceUpdateComponent
import com.tangem.features.forceupdate.ForceUpdateContinuation
import com.tangem.security.DeviceSecurityInfoProvider
import com.tangem.security.isSecurityExposed
import com.tangem.tap.features.root.RootDetectedWarningComponent
import com.tangem.tap.features.root.RootWarningContinuation
import com.tangem.tap.routing.configurator.AppRouterConfig
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

/**
 * Owns the pre-start gates shown before the regular startup navigation — the force-update screen and the
 * root-detected security warning — as interchangeable full-screen overlays in a single [childSlot].
 * [await] runs them in order and returns when the app may proceed, so the routing component stays agnostic.
 */
@Suppress("LongParameterList")
internal class AppStartupGateComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    private val getAppUpdateStateUseCase: GetAppUpdateStateUseCase,
    private val forceUpdateContinuation: ForceUpdateContinuation,
    private val forceUpdateComponentFactory: ForceUpdateComponent.Factory,
    private val rootDetectedWarningComponentFactory: RootDetectedWarningComponent.Factory,
    private val rootWarningContinuation: RootWarningContinuation,
    private val settingsRepository: SettingsRepository,
    private val securityInfoProvider: DeviceSecurityInfoProvider,
    private val appRouterConfig: AppRouterConfig,
) : AppComponentContext by context, ComposableContentComponent {

    private val slotNavigation = SlotNavigation<GateConfig>()

    private val slot: Value<ChildSlot<GateConfig, ComposableContentComponent>> = childSlot(
        source = slotNavigation,
        serializer = null,
        handleBackButton = false,
        childFactory = { config, childContext ->
            when (config) {
                is GateConfig.ForceUpdate -> forceUpdateComponentFactory.create(
                    context = childByContext(childContext),
                    params = ForceUpdateComponent.Params(mode = config.mode),
                )
                GateConfig.RootWarning -> rootDetectedWarningComponentFactory.create(
                    context = childByContext(childContext),
                    params = Unit,
                )
            }
        },
    )

    /** Runs the pre-start gates in order; returns when the app may proceed to normal startup. */
    suspend fun await() {
        awaitForceUpdate()
        awaitRootWarning()
    }

    private suspend fun awaitForceUpdate() {
        val mode = runSuspendCatching { resolveForceUpdateMode() }
            .onFailure { error -> TangemLogger.e("App update check failed, proceeding with normal startup", error) }
            .getOrNull()
            ?: return

        showGate(GateConfig.ForceUpdate(mode))
        forceUpdateContinuation.awaitDismiss()
        slotNavigation.dismiss()
    }

    private suspend fun awaitRootWarning() {
        if (settingsRepository.isRootDetectedWarningShown() || !securityInfoProvider.isSecurityExposed()) return

        showGate(GateConfig.RootWarning)
        rootWarningContinuation.awaitDismiss()
        settingsRepository.setRootDetectedWarningShown(true)
        slotNavigation.dismiss()
    }

    private fun showGate(config: GateConfig) {
        // The gate overlay is drawn on top of the splash, so mark navigation initialized to dismiss the splash.
        appRouterConfig.initializedState.value = true
        slotNavigation.activate(config)
    }

    private suspend fun resolveForceUpdateMode(): ForceUpdateComponent.Mode? {
        val mode = getAppUpdateStateUseCase.getCached().toForceUpdateModeOrNull()

        // The force-update screen re-checks on open, so a one-shot refresh is only needed when no screen is shown.
        if (mode == null) {
            componentScope.launch { getAppUpdateStateUseCase.refresh() }
        }

        return mode
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val child by slot.subscribeAsState()
        child.child?.instance?.Content(modifier)
    }

    private fun AppUpdateState.toForceUpdateModeOrNull(): ForceUpdateComponent.Mode? = when (this) {
        AppUpdateState.ForceUpdate -> ForceUpdateComponent.Mode.Force
        AppUpdateState.Brick -> ForceUpdateComponent.Mode.Brick
        AppUpdateState.OsTooOld -> ForceUpdateComponent.Mode.OsTooOld
        AppUpdateState.OptionalUpdate,
        AppUpdateState.NoUpdate,
        -> null
    }

    private sealed interface GateConfig {
        data class ForceUpdate(val mode: ForceUpdateComponent.Mode) : GateConfig
        data object RootWarning : GateConfig
    }

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext): AppStartupGateComponent
    }
}