package com.tangem.features.onramp.success

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.onramp.component.OnrampSuccessComponent
import com.tangem.features.onramp.success.model.OnrampSuccessComponentModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultOnrampSuccessComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: OnrampSuccessComponent.Params,
) : OnrampSuccessComponent, AppComponentContext by appComponentContext {

    private val model: OnrampSuccessComponentModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsState()

        // todo onramp https://tangem.atlassian.net/browse/AND-9100
    }

    @AssistedFactory
    interface Factory : OnrampSuccessComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: OnrampSuccessComponent.Params,
        ): DefaultOnrampSuccessComponent
    }
}
