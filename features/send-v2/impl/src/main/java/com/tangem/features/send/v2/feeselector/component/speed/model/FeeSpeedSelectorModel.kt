package com.tangem.features.send.v2.feeselector.component.speed.model

import androidx.compose.runtime.Stable
import com.tangem.common.TangemBlogUrlBuilder
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.feeselector.component.FeeSelectorComponentParams
import com.tangem.features.send.v2.feeselector.component.speed.FeeSpeedSelectorIntents
import com.tangem.features.send.v2.feeselector.model.FeeSelectorIntents
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class FeeSpeedSelectorModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val urlOpener: UrlOpener,
) : Model(), FeeSpeedSelectorIntents,
    FeeSelectorIntents by paramsContainer.require<FeeSelectorComponentParams>().intents {

    private val params = paramsContainer.require<FeeSelectorComponentParams>()

    val uiState: StateFlow<FeeSelectorUM>
        field = params.state

    override fun onLearnMoreClick() {
        modelScope.launch {
            urlOpener.openUrl(
                TangemBlogUrlBuilder.build(TangemBlogUrlBuilder.Post.WhatIsTransactionFee),
            )
        }
    }
}