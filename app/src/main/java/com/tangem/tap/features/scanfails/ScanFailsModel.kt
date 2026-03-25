package com.tangem.tap.features.scanfails

import com.tangem.common.TangemBlogUrlBuilder
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.card.ScanFailsRequester
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.tap.common.analytics.events.ScanFailsDialogAnalytics
import com.tangem.tap.features.scanfails.ui.ScanFailsUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class ScanFailsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val urlOpener: UrlOpener,
) : Model() {

    private val result = MutableStateFlow<ScanFailsRequester.Result?>(null)

    val uiState: StateFlow<ScanFailsUM>
        field = MutableStateFlow(ScanFailsUM(onDismiss = ::dismiss))

    fun show(source: ScanFailsRequester.Source) {
        val analyticsSource = source.toAnalyticsSource()
        result.value = null
        uiState.update {
            ScanFailsUM(
                isShown = true,
                onHowToScan = { onHowToScan(analyticsSource) },
                onRequestSupport = { onRequestSupport(analyticsSource) },
                onDismiss = ::dismiss,
            )
        }
    }

    suspend fun waitResult(): ScanFailsRequester.Result {
        return result.filterNotNull().first().also { result.value = null }
    }

    fun dismiss() {
        result.value = ScanFailsRequester.Result.Dismissed
        uiState.update { it.copy(isShown = false) }
    }

    private fun onHowToScan(source: AnalyticsParam.ScreensSources) {
        analyticsEventHandler.send(
            ScanFailsDialogAnalytics(
                button = ScanFailsDialogAnalytics.Buttons.HOW_TO_SCAN,
                source = source,
            ),
        )
        modelScope.launch {
            urlOpener.openUrl(TangemBlogUrlBuilder.build(TangemBlogUrlBuilder.Post.HowToScan))
        }
    }

    private fun onRequestSupport(source: AnalyticsParam.ScreensSources) {
        analyticsEventHandler.send(Basic.ButtonSupport(source))
        modelScope.launch {
            sendFeedbackEmailUseCase(type = FeedbackEmailType.ScanningProblem)
        }
    }

    private fun ScanFailsRequester.Source.toAnalyticsSource(): AnalyticsParam.ScreensSources = when (this) {
        ScanFailsRequester.Source.MAIN -> AnalyticsParam.ScreensSources.Main
        ScanFailsRequester.Source.SIGN_IN -> AnalyticsParam.ScreensSources.SignIn
        ScanFailsRequester.Source.SETTINGS -> AnalyticsParam.ScreensSources.Settings
        ScanFailsRequester.Source.INTRO -> AnalyticsParam.ScreensSources.Intro
    }
}