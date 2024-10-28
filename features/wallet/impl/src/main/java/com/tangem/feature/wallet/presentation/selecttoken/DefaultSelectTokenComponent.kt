package com.tangem.feature.wallet.presentation.selecttoken

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.feature.wallet.presentation.selecttoken.ui.SelectToken
import com.tangem.feature.wallet.presentation.tokenlist.TokenListComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultSelectTokenComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    tokenListComponentFactory: TokenListComponent.Factory,
    @Assisted private val params: SelectTokenComponent.Params,
) : AppComponentContext by appComponentContext, SelectTokenComponent {

    private val tokenListComponent: TokenListComponent = tokenListComponentFactory.create(
        context = child(key = "token_list"),
        params = TokenListComponent.Params(
            hasSearchBar = params.hasSearchBar,
            userWalletId = params.userWalletId,
            onTokenClick = params.onTokenClick,
        ),
    )

    @Composable
    override fun Content(modifier: Modifier) {
        SelectToken(
            titleResId = params.titleResId,
            onBackClick = router::pop,
            tokenListComponent = tokenListComponent,
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory : SelectTokenComponent.Factory {

        override fun create(
            context: AppComponentContext,
            params: SelectTokenComponent.Params,
        ): DefaultSelectTokenComponent
    }
}