package com.tangem.features.forceupdate.impl.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class ForceUpdateModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val appStoreOpener: AppStoreOpener,
    private val forceUpdateContinuation: ForceUpdateContinuation,
    private val getAppUpdateStateUseCase: GetAppUpdateStateUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: ForceUpdateComponent.Params = paramsContainer.require()

    val state: StateFlow<ForceUpdateUM>
        field = MutableStateFlow(createState(params.mode))

    init {
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
            onSupportClick = ::onSupportClick,
        )
        ForceUpdateComponent.Mode.Brick -> ForceUpdateUM(
            mode = mode,
            accent = Accent.Red,
            title = resourceReference(R.string.force_update_brick_title),
            description = resourceReference(R.string.force_update_brick_description),
            onUpdateClick = null,
            onLaterClick = null,
            onSupportClick = ::onSupportClick,
        )
        ForceUpdateComponent.Mode.OsTooOld -> ForceUpdateUM(
            mode = mode,
            accent = Accent.Yellow,
            title = resourceReference(R.string.force_update_os_title),
            description = resourceReference(R.string.force_update_os_description),
            onUpdateClick = null,
            onLaterClick = ::onLaterClick,
            onSupportClick = null,
        )
    }

    private fun onUpdateClick() {
        appStoreOpener.openStorePage()
    }

    private fun onLaterClick() {
        forceUpdateContinuation.dismiss()
    }

    private fun onSupportClick() {
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
            } else {
                state.update { current -> if (current.mode == mode) current else createState(mode) }
            }
        }
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