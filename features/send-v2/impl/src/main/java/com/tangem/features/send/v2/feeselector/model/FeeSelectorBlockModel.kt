package com.tangem.features.send.v2.feeselector.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.TangemBlogUrlBuilder
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.settings.NeverShowTapHelpUseCase
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents.SendScreenSource
import com.tangem.features.send.v2.api.callbacks.FeeSelectorModelCallback
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class FeeSelectorBlockModel @Inject constructor(
    paramsContainer: ParamsContainer,
    feeSelectorLogicFactory: FeeSelectorLogic.Factory,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val neverShowTapHelpUseCase: NeverShowTapHelpUseCase,
    private val urlOpener: UrlOpener,
) : Model(), FeeSelectorModelCallback {

    private val params = paramsContainer.require<FeeSelectorParams.FeeSelectorBlockParams>()
    private val feeSelectorLogic = feeSelectorLogicFactory.create(
        params = params,
        modelScope = modelScope,
    )

    val shouldShowOnlySpeedOption: Boolean
        get() = feeSelectorLogic.shouldShowOnlySpeedOption.value
    val feeSelectorBottomSheet = SlotNavigation<Unit>()
    val uiState: StateFlow<FeeSelectorUM>
        field = feeSelectorLogic.uiState

    override fun onFeeResult(feeSelectorUM: FeeSelectorUM) {
        uiState.value = feeSelectorUM
        feeSelectorBottomSheet.dismiss()
    }

    fun updateState(feeSelectorUM: FeeSelectorUM) {
        uiState.value = feeSelectorUM
    }

    fun onReadMoreClicked() {
        modelScope.launch {
            urlOpener.openUrl(TangemBlogUrlBuilder.build(TangemBlogUrlBuilder.Post.WhatIsTransactionFee))
        }
    }

    fun showFeeSelector() {
        analyticsEventHandler.send(
            CommonSendAnalyticEvents.ScreenReopened(
                categoryName = params.analyticsCategoryName,
                source = SendScreenSource.Fee,
            ),
        )
        modelScope.launch {
            neverShowTapHelpUseCase()
            feeSelectorBottomSheet.activate(Unit)
        }
    }
}