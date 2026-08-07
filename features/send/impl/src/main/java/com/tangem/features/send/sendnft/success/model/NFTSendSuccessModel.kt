package com.tangem.features.send.sendnft.success.model

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.features.nft.entity.NFTSendSuccessTrigger
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents.SendScreenSource
import com.tangem.features.send.sendnft.success.NFTSendSuccessComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class NFTSendSuccessModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val urlOpener: UrlOpener,
    private val shareManager: ShareManager,
    private val nftSendSuccessTrigger: NFTSendSuccessTrigger,
) : Model() {
    private val params: NFTSendSuccessComponent.Params = paramsContainer.require()

    val uiState = params.nftSendUMFlow

    fun onBackClick() {
        modelScope.launch {
            nftSendSuccessTrigger.triggerSuccessNFTSend()
            router.pop()
            analyticsEventHandler.send(
                CommonSendAnalyticEvents.CloseButtonClicked(
                    categoryName = params.analyticsCategoryName,
                    source = SendScreenSource.Confirm,
                    isFromSummary = true,
                    isValid = true,
                ),
            )
        }
    }

    fun onExploreClick() {
        analyticsEventHandler.send(CommonSendAnalyticEvents.ExploreButtonClicked(params.analyticsCategoryName))
        urlOpener.openUrl(params.txUrl)
    }

    fun onShareClick() {
        analyticsEventHandler.send(CommonSendAnalyticEvents.ShareButtonClicked(params.analyticsCategoryName))
        shareManager.shareText(params.txUrl)
    }
}