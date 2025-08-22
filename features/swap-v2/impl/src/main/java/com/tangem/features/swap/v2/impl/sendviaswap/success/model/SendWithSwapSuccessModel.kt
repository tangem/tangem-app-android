package com.tangem.features.swap.v2.impl.sendviaswap.success.model

import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.common.ui.navigationButtons.NavigationUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.swap.v2.impl.R
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
    private val appRouter: AppRouter,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: SendWithSwapSuccessComponent.Params = paramsContainer.require()

    val uiState: StateFlow<SendWithSwapUM> = params.sendWithSwapUMFlow

    val confirmUM = uiState.value.confirmUM as? ConfirmUM.Success

    init {
        configConfirmSuccessNavigation()
    }

    private fun configConfirmSuccessNavigation() {
        params.callback.onNavigationResult(
            NavigationUM.Content(
                title = TextReference.EMPTY,
                subtitle = null,
                backIconRes = R.drawable.ic_close_24,
                backIconClick = appRouter::pop,
                primaryButton = NavigationButton(
                    textReference = resourceReference(R.string.common_close),
                    iconRes = null,
                    isEnabled = true,
                    isHapticClick = false,
                    onClick = appRouter::pop,
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
        )
    }

    private fun onExploreClick() {
        if (confirmUM == null) return

        // analyticsEventHandler.send(CommonSendAnalyticEvents.ExploreButtonClicked(params.analyticsCategoryName))
        urlOpener.openUrl(confirmUM.txUrl)
    }

    private fun onShareClick() {
        if (confirmUM == null) return

        // analyticsEventHandler.send(CommonSendAnalyticEvents.ShareButtonClicked(params.analyticsCategoryName))
        shareManager.shareText(confirmUM.txUrl)
    }
}