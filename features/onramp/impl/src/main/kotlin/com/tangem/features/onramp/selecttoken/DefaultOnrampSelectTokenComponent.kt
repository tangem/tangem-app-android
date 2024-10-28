package com.tangem.features.onramp.selecttoken

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.features.onramp.selecttoken.ui.OnrampSelectToken
import com.tangem.features.onramp.tokenlist.OnrampTokenListComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultOnrampSelectTokenComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    onrampTokenListComponentFactory: OnrampTokenListComponent.Factory,
    @Assisted private val params: OnrampSelectTokenComponent.Params,
) : AppComponentContext by appComponentContext, OnrampSelectTokenComponent {

    private val onrampTokenListComponent: OnrampTokenListComponent = onrampTokenListComponentFactory.create(
        context = child(key = "token_list"),
        params = OnrampTokenListComponent.Params(
            hasSearchBar = params.hasSearchBar,
            userWalletId = params.userWalletId,
            onTokenClick = params.onTokenClick,
        ),
    )

    @Composable
    override fun Content(modifier: Modifier) {
        OnrampSelectToken(
            titleResId = params.titleResId,
            onBackClick = router::pop,
            onrampTokenListComponent = onrampTokenListComponent,
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory : OnrampSelectTokenComponent.Factory {

        override fun create(
            context: AppComponentContext,
            params: OnrampSelectTokenComponent.Params,
        ): DefaultOnrampSelectTokenComponent
    }
}