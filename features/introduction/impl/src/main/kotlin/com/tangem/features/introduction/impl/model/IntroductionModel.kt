package com.tangem.features.introduction.impl.model

import android.view.SurfaceView
import com.tangem.common.routing.entity.InitScreenLaunchMode
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.card.analytics.IntroductionProcess
import com.tangem.features.introduction.IntroductionComponent
import com.tangem.features.introduction.impl.R
import com.tangem.features.introduction.impl.engine.IntroductionVideoPlayer
import com.tangem.features.introduction.impl.ui.state.IntroductionUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class IntroductionModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    videoPlayerFactory: IntroductionVideoPlayer.Factory,
    private val urlOpener: UrlOpener,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model(), IntroductionVideoPlayer.Listener {

    private val params = paramsContainer.require<IntroductionComponent.Params>()

    private var isSurfaceAttached = false

    private val videoPlayer = videoPlayerFactory.create(
        videoRes = R.raw.introduction_background,
        listener = this,
    )

    val uiState: StateFlow<IntroductionUM>
        field = MutableStateFlow(
            IntroductionUM(
                isVideoReady = false,
                isMotionEnabled = videoPlayer.isMotionEnabled,
                // TODO [REDACTED_TASK_KEY]: open the Create wallet and I have a wallet bottom sheets
                onCreateWalletClick = {},
                onIHaveWalletClick = {},
                onTermsOfServiceClick = ::onTermsOfServiceClick,
                onPrivacyPolicyClick = ::onPrivacyPolicyClick,
            ),
        )

    init {
        analyticsEventHandler.send(IntroductionProcess.ScreenOpened())

        when (params.launchMode) {
            // TODO [REDACTED_TASK_KEY]: start the card scan here and drop the WithCardScan exception in ChildFactory;
            //  it needs the progress and error states the screen has no design for yet.
            InitScreenLaunchMode.WithCardScan,
            InitScreenLaunchMode.Standard,
            -> Unit
        }
    }

    override fun onDestroy() {
        videoPlayer.release()
        super.onDestroy()
    }

    override fun onFirstFrameRendered() {
        if (uiState.value.isVideoReady) return
        uiState.update { it.copy(isVideoReady = true) }
    }

    override fun onError() {
        // Errors arrive asynchronously, so one can land after the surface is gone; lifting the shutter then
        // would uncover a blank surface on the way back.
        if (!isSurfaceAttached) return
        uiState.update { it.copy(isVideoReady = true) }
    }

    fun attachSurface(surfaceView: SurfaceView) {
        isSurfaceAttached = true
        videoPlayer.attachSurface(surfaceView)
    }

    fun detachSurface(surfaceView: SurfaceView) {
        isSurfaceAttached = false
        videoPlayer.detachSurface(surfaceView)
        // A new surface starts out blank, so the shutter has to go back up and wait for its own first frame.
        uiState.update { it.copy(isVideoReady = false) }
    }

    fun setVideoRunning(isRunning: Boolean) {
        videoPlayer.setRunning(isRunning)
    }

    private fun onTermsOfServiceClick() {
        urlOpener.openUrl(TERMS_OF_SERVICE_URL)
    }

    private fun onPrivacyPolicyClick() {
        urlOpener.openUrl(PRIVACY_POLICY_URL)
    }

    private companion object {
        const val TERMS_OF_SERVICE_URL = "https://tangem.com/terms-of-service/"
        const val PRIVACY_POLICY_URL = "https://tangem.com/privacy-policy/"
    }
}