package com.tangem.features.swap.v2.impl.sendviaswap.success.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.features.swap.v2.impl.common.entity.ConfirmUM
import com.tangem.features.swap.v2.impl.sendviaswap.entity.SendWithSwapUM
import com.tangem.features.swap.v2.impl.sendviaswap.success.SendWithSwapSuccessComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class SendWithSwapSuccessModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val urlOpener: UrlOpener,
    private val shareManager: ShareManager,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: SendWithSwapSuccessComponent.Params = paramsContainer.require()

    val uiState: StateFlow<SendWithSwapUM> = params.sendWithSwapUMFlow

    val confirmUM = uiState.value.confirmUM as? ConfirmUM.Success

    fun onExploreClick() {
        if (confirmUM == null) return

        // analyticsEventHandler.send(CommonSendAnalyticEvents.ExploreButtonClicked(params.analyticsCategoryName))
        urlOpener.openUrl(confirmUM.txUrl)
    }

    fun onShareClick() {
        if (confirmUM == null) return

        // analyticsEventHandler.send(CommonSendAnalyticEvents.ShareButtonClicked(params.analyticsCategoryName))
        shareManager.shareText(confirmUM.txUrl)
    }
}