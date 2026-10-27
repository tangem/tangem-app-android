package com.tangem.features.forceupdate.impl.model

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.analytics.models.event.MainScreenAnalyticsEvent
import com.tangem.core.analytics.models.event.MainScreenAnalyticsEvent.ButtonUpdateApp
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.settings.SettingsManager
import com.tangem.core.navigation.url.AppStoreOpener
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.appupdate.model.AppUpdateState
import com.tangem.domain.appupdate.usecase.GetAppUpdateStateUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.features.forceupdate.ForceUpdateComponent
import com.tangem.features.forceupdate.ForceUpdateContinuation
import com.tangem.features.forceupdate.impl.R
import com.tangem.features.forceupdate.impl.ui.state.ForceUpdateUM
import com.tangem.features.forceupdate.impl.ui.state.ForceUpdateUM.Accent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class ForceUpdateModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val appStoreOpener: AppStoreOpener,
    private val settingsManager: SettingsManager,
    private val forceUpdateContinuation: ForceUpdateContinuation,
    private val getAppUpdateStateUseCase: GetAppUpdateStateUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: ForceUpdateComponent.Params = paramsContainer.require()

    val state: StateFlow<ForceUpdateUM>
        field = MutableStateFlow(createState(params.mode))

    init {
        analyticsEventHandler.send(params.mode.toNoticeEvent())
        checkAppUpdateState()
    }

    private fun createState(mode: ForceUpdateComponent.Mode): ForceUpdateUM = when (mode) {
        ForceUpdateComponent.Mode.Force -> ForceUpdateUM(
            mode = mode,
            accent = Accent.Red,
            title = resourceReference(R.string.force_update_warning_title),
            description = resourceReference(R.string.force_update_warning_message),
            onUpdateClick = ::onUpdateClick,
            onLaterClick = null,
            onOpenSettingsClick = null,
            onSupportClick = { onSupportClick(source = AnalyticsParam.ScreensSources.CritUpdate) },
        )
        ForceUpdateComponent.Mode.Brick -> ForceUpdateUM(
            mode = mode,
            accent = Accent.Red,
            title = resourceReference(R.string.force_update_brick_title),
            description = resourceReference(R.string.force_update_brick_description),
            onUpdateClick = null,
            onLaterClick = null,
            onOpenSettingsClick = null,
            onSupportClick = { onSupportClick(source = AnalyticsParam.ScreensSources.UnsupportedDevice) },
        )
        ForceUpdateComponent.Mode.OsTooOld -> ForceUpdateUM(
            mode = mode,
            accent = Accent.Yellow,
            title = resourceReference(R.string.force_update_os_title),
            description = resourceReference(R.string.force_update_os_description),
            onUpdateClick = null,
            onLaterClick = ::onLaterClick,
            onOpenSettingsClick = ::onOpenSettingsClick,
            onSupportClick = null,
        )
    }

    private fun onUpdateClick() {
        analyticsEventHandler.send(ButtonUpdateApp(source = ButtonUpdateApp.UpdateSource.Critical))
        appStoreOpener.openStorePage()
    }

    private fun onLaterClick() {
        analyticsEventHandler.send(MainScreenAnalyticsEvent.ButtonLater())
        forceUpdateContinuation.dismiss()
    }

    private fun onOpenSettingsClick() {
        analyticsEventHandler.send(MainScreenAnalyticsEvent.ButtonOpenSettings())
        settingsManager.openSystemSettings()
    }

    private fun onSupportClick(source: AnalyticsParam.ScreensSources) {
        analyticsEventHandler.send(Basic.ButtonSupport(source = source))
        modelScope.launch {
            sendFeedbackEmailUseCase(type = FeedbackEmailType.AppUpdateProblem)
        }
    }

    /**
     * Re-checks the update state once when the screen opens. A concrete result refines the displayed mode
     * (e.g. escalates an optional update to a blocking one). When the update is no longer required — or the
     * state can't be resolved — the screen is dismissed so the regular startup can proceed. Network errors
     * fall back to the cached thresholds, so a transient failure keeps the current screen.
     */
    private fun checkAppUpdateState() {
        modelScope.launch {
            val mode = getAppUpdateStateUseCase.refresh().toModeOrNull()
            if (mode == null) {
                forceUpdateContinuation.dismiss()
            } else if (state.value.mode != mode) {
                state.value = createState(mode)
                analyticsEventHandler.send(mode.toNoticeEvent())
            }
        }
    }

    private fun ForceUpdateComponent.Mode.toNoticeEvent(): AnalyticsEvent = when (this) {
        ForceUpdateComponent.Mode.Force -> MainScreenAnalyticsEvent.NoticeCriticalUpdate()
        ForceUpdateComponent.Mode.Brick -> MainScreenAnalyticsEvent.NoticeUnsupportedDevice()
        ForceUpdateComponent.Mode.OsTooOld -> MainScreenAnalyticsEvent.NoticeOsUpdate()
    }

    private fun AppUpdateState.toModeOrNull(): ForceUpdateComponent.Mode? = when (this) {
        AppUpdateState.ForceUpdate -> ForceUpdateComponent.Mode.Force
        AppUpdateState.Brick -> ForceUpdateComponent.Mode.Brick
        AppUpdateState.OsTooOld -> ForceUpdateComponent.Mode.OsTooOld
        AppUpdateState.OptionalUpdate,
        AppUpdateState.NoUpdate,
        -> null
    }
}