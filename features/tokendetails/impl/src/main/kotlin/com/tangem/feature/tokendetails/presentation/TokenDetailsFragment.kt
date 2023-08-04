package com.tangem.feature.tokendetails.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.compose.hiltViewModel
import com.tangem.core.ui.components.SystemBarsEffect
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tokendetails.presentation.router.InnerTokenDetailsRouter
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.TokenDetailsScreen
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsViewModel
import com.tangem.features.tokendetails.navigation.TokenDetailsRouter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class TokenDetailsFragment : Fragment() {

    @Inject
    lateinit var tokenDetailsRouter: TokenDetailsRouter

    private val internalTokenDetailsRouter: InnerTokenDetailsRouter
        get() = requireNotNull(tokenDetailsRouter as? InnerTokenDetailsRouter) {
            "internalTokenDetailsRouter should be instance of InnerTokenDetailsRouter"
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(inflater.context).apply {
            setContent {
                TangemTheme {
                    val systemBarsColor = TangemTheme.colors.background.secondary
                    SystemBarsEffect {
                        setSystemBarsColor(systemBarsColor)
                    }

                    val viewModel = hiltViewModel<TokenDetailsViewModel>()
                    viewModel.router = this@TokenDetailsFragment.internalTokenDetailsRouter
                    TokenDetailsScreen(state = viewModel.uiState)
                }
            }
        }
    }
}