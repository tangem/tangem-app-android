package com.tangem.features.send.sendnft.success.model

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents.SendScreenSource
import com.tangem.features.send.send.ui.state.SendUM
import com.tangem.features.send.sendnft.success.NFTSendSuccessComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

@Stable
@ModelScoped
internal class NFTSendSuccessModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val urlOpener: UrlOpener,
    private val shareManager: ShareManager,
) : Model() {
    private val params: NFTSendSuccessComponent.Params = paramsContainer.require()

    val uiState = params.nftSendUMFlow

    fun onBackClick() {
        analyticsEventHandler.send(
            CommonSendAnalyticEvents.CloseButtonClicked(
                categoryName = params.analyticsCategoryName,
                source = SendScreenSource.Confirm,
                isFromSummary = true,
                isValid = true,
            ),
        )
    }

    fun onExploreClick() {
        analyticsEventHandler.send(CommonSendAnalyticEvents.ExploreButtonClicked(params.analyticsCategoryName))
        urlOpener.openUrl(params.txUrl)
    }

    fun onShareClick() {
        analyticsEventHandler.send(CommonSendAnalyticEvents.ShareButtonClicked(params.analyticsCategoryName))
        shareManager.shareText(params.txUrl)
    }

    interface ModelCallback {
        fun onResult(sendUM: SendUM)
    }
}