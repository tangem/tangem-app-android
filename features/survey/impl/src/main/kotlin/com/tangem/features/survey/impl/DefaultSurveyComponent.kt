package com.tangem.features.survey.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.features.survey.SurveyComponent
import com.tangem.features.survey.SurveyLaunchData
import com.tangem.features.survey.SurveySparrowLauncher
import com.tangem.features.survey.impl.service.SurveyCustomParamsBuilder
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
internal class DefaultSurveyComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: SurveyComponent.Params,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val customParamsBuilder: SurveyCustomParamsBuilder,
    private val surveySparrowLauncher: SurveySparrowLauncher,
    @Suppress("UnusedPrivateProperty") // TODO([REDACTED_TASK_KEY]): emit [Survey] analytics events
    private val analyticsEventHandler: AnalyticsEventHandler,
) : SurveyComponent, AppComponentContext by appComponentContext {

    init {
        // componentScope runs on mainImmediate, so presenting the SDK is already on the main thread.
        componentScope.launch {
            val launchData = buildLaunchData()
            if (launchData != null) {
                surveySparrowLauncher.present(activity, launchData)
                // TODO([REDACTED_TASK_KEY]): analyticsEventHandler.send(SurveyAnalyticsEvent.Shown(...))
            }
            router.pop()
        }
    }

    private suspend fun buildLaunchData(): SurveyLaunchData? {
        return getSelectedWalletSyncUseCase().fold(
            ifLeft = { error ->
                TangemLogger.e("$TAG: survey skipped, no available wallet ($error)")
                null
            },
            ifRight = { userWallet ->
                SurveyLaunchData(
                    domain = SURVEY_DOMAIN,
                    token = params.token,
                    customParams = customParamsBuilder.build(
                        userWallet = userWallet,
                        token = params.token,
                        displayId = params.displayId,
                    ),
                )
            },
        )
    }

    @Composable
    override fun Content(modifier: Modifier) = Unit

    @AssistedFactory
    interface Factory : SurveyComponent.Factory {
        override fun create(context: AppComponentContext, params: SurveyComponent.Params): DefaultSurveyComponent
    }

    private companion object {
        const val TAG = "SurveyComponent"
        const val SURVEY_DOMAIN = "tangem.surveysparrow.com"
    }
}