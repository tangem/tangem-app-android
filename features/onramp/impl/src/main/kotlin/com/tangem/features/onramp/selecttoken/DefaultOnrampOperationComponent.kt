package com.tangem.features.onramp.selecttoken

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.onramp.selecttoken.model.OnrampOperationModel
import com.tangem.features.onramp.selecttoken.ui.OnrampSelectToken
import com.tangem.features.onramp.tokenlist.OnrampTokenListComponent
import com.tangem.features.onramp.tokenlist.entity.OnrampOperation
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultOnrampOperationComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    onrampTokenListComponentFactory: OnrampTokenListComponent.Factory,
    @Assisted private val params: OnrampOperationComponent.Params,
) : AppComponentContext by appComponentContext, OnrampOperationComponent {

    private val model: OnrampOperationModel = getOrCreateModel(params)

    private val onrampTokenListComponent: OnrampTokenListComponent = onrampTokenListComponentFactory.create(
        context = child(key = "token_list"),
        params = OnrampTokenListComponent.Params(
            filterOperation = when (params) {
                is OnrampOperationComponent.Params.Buy -> OnrampOperation.BUY
                is OnrampOperationComponent.Params.Sell -> OnrampOperation.SELL
            },
            userWalletId = params.userWalletId,
            onTokenClick = { _, status -> model.onTokenClick(status) },
        ),
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state = model.state.collectAsStateWithLifecycle()

        OnrampSelectToken(
            state = state.value,
            onrampTokenListComponent = onrampTokenListComponent,
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory : OnrampOperationComponent.Factory {

        override fun create(
            context: AppComponentContext,
            params: OnrampOperationComponent.Params,
        ): DefaultOnrampOperationComponent
    }
}