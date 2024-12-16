package com.tangem.features.onramp.redirect

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.onramp.redirect.model.OnrampRedirectModel
import com.tangem.features.onramp.redirect.ui.OnrampRedirectContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultOnrampRedirectComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: OnrampRedirectComponent.Params,
) : OnrampRedirectComponent, AppComponentContext by appComponentContext {

    private val model: OnrampRedirectModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        BackHandler(onBack = params.onBack)
        OnrampRedirectContent(modifier = modifier, state = model.state)
    }

    @AssistedFactory
    interface Factory : OnrampRedirectComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: OnrampRedirectComponent.Params,
        ): DefaultOnrampRedirectComponent
    }
}