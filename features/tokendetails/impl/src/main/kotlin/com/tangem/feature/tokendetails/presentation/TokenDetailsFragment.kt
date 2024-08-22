package com.tangem.feature.tokendetails.presentation

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.defaultComponentContext
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.bundle.unbundle
import com.tangem.core.decompose.context.DefaultAppComponentContext
import com.tangem.core.decompose.di.DecomposeComponent
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.components.NavigationBar3ButtonsScrim
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.feature.tokendetails.presentation.router.InnerTokenDetailsRouter
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.TokenDetailsScreen
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsViewModel
import com.tangem.features.markets.MarketsFeatureToggles
import com.tangem.features.markets.token.block.TokenMarketBlockComponent
import com.tangem.features.tokendetails.navigation.TokenDetailsRouter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class TokenDetailsFragment : ComposeFragment() {

    @Inject
    override lateinit var uiDependencies: UiDependencies

    @Inject
    lateinit var tokenDetailsRouter: TokenDetailsRouter

    @Inject
    internal lateinit var marketsFeatureToggles: MarketsFeatureToggles

    @Inject
    internal lateinit var coroutineDispatcherProvider: CoroutineDispatcherProvider

    @Inject
    internal lateinit var componentBuilder: DecomposeComponent.Builder

    @Inject
    internal lateinit var tokenMarketBlockComponentFactory: TokenMarketBlockComponent.Factory

    private var tokenMarketBlockComponent: TokenMarketBlockComponent? = null

    private val internalTokenDetailsRouter: InnerTokenDetailsRouter
        get() = requireNotNull(tokenDetailsRouter as? InnerTokenDetailsRouter) {
            "internalTokenDetailsRouter should be instance of InnerTokenDetailsRouter"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (marketsFeatureToggles.isFeatureEnabled) {
            val appContext = DefaultAppComponentContext(
                componentContext = defaultComponentContext(requireActivity().onBackPressedDispatcher),
                messageHandler = uiDependencies.eventMessageHandler,
                dispatchers = coroutineDispatcherProvider,
                hiltComponentBuilder = componentBuilder,
            )

            val cryptoCurrency: CryptoCurrency = arguments
                ?.getBundle(AppRoute.CurrencyDetails.CRYPTO_CURRENCY_KEY)
                ?.unbundle(CryptoCurrency.serializer())
                ?: error("This screen can't open without `CryptoCurrency`")

            tokenMarketBlockComponent = tokenMarketBlockComponentFactory.create(
                appComponentContext = appContext,
                params = TokenMarketBlockComponent.Params(cryptoCurrency = cryptoCurrency),
            )
        }
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val viewModel = hiltViewModel<TokenDetailsViewModel>()
        viewModel.router = this@TokenDetailsFragment.internalTokenDetailsRouter
        LocalLifecycleOwner.current.lifecycle.addObserver(viewModel)
        NavigationBar3ButtonsScrim()
        TokenDetailsScreen(
            state = viewModel.uiState.collectAsStateWithLifecycle().value,
            tokenMarketBlockComponent = tokenMarketBlockComponent,
        )
    }
}