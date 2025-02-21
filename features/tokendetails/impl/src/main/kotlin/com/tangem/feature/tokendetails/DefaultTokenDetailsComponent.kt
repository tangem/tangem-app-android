package com.tangem.feature.tokendetails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.essenty.lifecycle.subscribe
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.deeplink.DeepLinksRegistry
import com.tangem.core.deeplink.global.BuyCurrencyDeepLink
import com.tangem.core.deeplink.utils.registerDeepLinks
import com.tangem.core.ui.components.NavigationBar3ButtonsScrim
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsModel
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.TokenDetailsScreen
import com.tangem.features.markets.token.block.TokenMarketBlockComponent
import com.tangem.features.tokendetails.TokenDetailsComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTokenDetailsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: TokenDetailsComponent.Params,
    tokenMarketBlockComponentFactory: TokenMarketBlockComponent.Factory,
    deepLinksRegistry: DeepLinksRegistry,
) : TokenDetailsComponent, AppComponentContext by appComponentContext {

    private val model: TokenDetailsModel = getOrCreateModel(params)

    init {
        lifecycle.subscribe(
            onPause = model::onPause,
            onResume = model::onResume,
        )

        registerDeepLinks(
            registry = deepLinksRegistry,
            BuyCurrencyDeepLink(
                onReceive = model::onBuyCurrencyDeepLink,
            ),
        )
    }

    private val tokenMarketBlockComponent = params.currency.toTokenMarketParam()?.let { tokenMarketParams ->
        tokenMarketBlockComponentFactory.create(
            appComponentContext = child("tokenMarketBlockComponent"),
            params = tokenMarketParams,
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        NavigationBar3ButtonsScrim()
        TokenDetailsScreen(
            state = state,
            tokenMarketBlockComponent = tokenMarketBlockComponent,
        )
    }

    private fun CryptoCurrency.toTokenMarketParam(): TokenMarketBlockComponent.Params? {
        id.rawCurrencyId ?: return null // token price is not available

        return TokenMarketBlockComponent.Params(cryptoCurrency = this)
    }

    @AssistedFactory
    interface Factory : TokenDetailsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TokenDetailsComponent.Params,
        ): DefaultTokenDetailsComponent
    }
}