package com.tangem.feature.tokendetails.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import com.tangem.core.ui.components.SystemBarsEffect
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.core.ui.theme.AppThemeModeHolder
import com.tangem.feature.tokendetails.presentation.router.InnerTokenDetailsRouter
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.TokenDetailsScreen
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsViewModel
import com.tangem.features.tokendetails.navigation.TokenDetailsRouter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class TokenDetailsFragment : ComposeFragment() {

    @Inject
    override lateinit var appThemeModeHolder: AppThemeModeHolder

    @Inject
    lateinit var tokenDetailsRouter: TokenDetailsRouter

    private val internalTokenDetailsRouter: InnerTokenDetailsRouter
        get() = requireNotNull(tokenDetailsRouter as? InnerTokenDetailsRouter) {
            "internalTokenDetailsRouter should be instance of InnerTokenDetailsRouter"
        }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val viewModel = hiltViewModel<TokenDetailsViewModel>()
        viewModel.router = this@TokenDetailsFragment.internalTokenDetailsRouter

        LocalLifecycleOwner.current.lifecycle.addObserver(viewModel)

        val systemBarsColor = TangemTheme.colors.background.secondary
        SystemBarsEffect {
            setSystemBarsColor(systemBarsColor)
        }

        TokenDetailsScreen(state = viewModel.uiState)
    }
}