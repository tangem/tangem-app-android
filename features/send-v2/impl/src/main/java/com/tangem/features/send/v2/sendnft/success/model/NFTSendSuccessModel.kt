package com.tangem.features.send.v2.sendnft.success.model

import androidx.compose.runtime.Stable
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.common.ui.navigationButtons.NavigationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents.SendScreenSource
import com.tangem.features.send.v2.common.CommonSendRoute
import com.tangem.features.send.v2.impl.R
import com.tangem.features.send.v2.send.ui.state.SendUM
import com.tangem.features.send.v2.sendnft.success.NFTSendSuccessComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@Stable
@ModelScoped
internal class NFTSendSuccessModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val appRouter: AppRouter,
    private val urlOpener: UrlOpener,
    private val shareManager: ShareManager,
) : Model() {
    private val params: NFTSendSuccessComponent.Params = paramsContainer.require()

    val uiState = params.nftSendUMFlow

    init {
        configConfirmSuccessNavigation()
    }

    private fun configConfirmSuccessNavigation() {
        combine(
            flow = uiState,
            flow2 = params.currentRoute,
            transform = { state, route -> state to route },
        ).filter { it.second is CommonSendRoute.ConfirmSuccess }.onEach { (state, _) ->
            params.callback.onResult(
                state.copy(
                    navigationUM = NavigationUM.Content(
                        title = stringReference(""),
                        subtitle = null,
                        backIconRes = R.drawable.ic_close_24,
                        backIconClick = {
                            analyticsEventHandler.send(
                                CommonSendAnalyticEvents.CloseButtonClicked(
                                    categoryName = params.analyticsCategoryName,
                                    source = SendScreenSource.Confirm,
                                    isFromSummary = true,
                                    isValid = true,
                                ),
                            )
                            appRouter.pop()
                        },
                        primaryButton = NavigationButton(
                            textReference = resourceReference(R.string.common_close),
                            iconRes = null,
                            isEnabled = true,
                            isHapticClick = false,
                            onClick = {
                                appRouter.pop()
                            },
                        ),
                        prevButton = null,
                        secondaryPairButtonsUM = NavigationButton(
                            textReference = resourceReference(R.string.common_explore),
                            iconRes = R.drawable.ic_web_24,
                            onClick = ::onExploreClick,
                        ) to NavigationButton(
                            textReference = resourceReference(R.string.common_share),
                            iconRes = R.drawable.ic_share_24,
                            onClick = ::onShareClick,
                        ),
                    ),
                ),
            )
        }.launchIn(modelScope)
    }

    private fun onExploreClick() {
        analyticsEventHandler.send(CommonSendAnalyticEvents.ExploreButtonClicked(params.analyticsCategoryName))
        urlOpener.openUrl(params.txUrl)
    }

    private fun onShareClick() {
        analyticsEventHandler.send(CommonSendAnalyticEvents.ShareButtonClicked(params.analyticsCategoryName))
        shareManager.shareText(params.txUrl)
    }

    interface ModelCallback {
        fun onResult(sendUM: SendUM)
    }
}