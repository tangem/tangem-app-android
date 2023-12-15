package com.tangem.managetokens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import com.tangem.core.ui.components.SystemBarsEffect
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.core.ui.theme.AppThemeModeHolder
import com.tangem.features.managetokens.navigation.ManageTokensRouter
import com.tangem.managetokens.presentation.managetokens.ui.ManageTokensScreen
import com.tangem.managetokens.presentation.managetokens.viewmodels.ManageTokensViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class ManageTokensFragment : ComposeFragment() {

    @Inject
    override lateinit var appThemeModeHolder: AppThemeModeHolder

    @Inject
    lateinit var manageTokensRouter: ManageTokensRouter

    // private val internalManageTokensRouter: InnerManageTokensRouter
    //     get() = requireNotNull(manageTokensRouter as? InnerManageTokensRouter) {
    //         "internalManageTokensRouter should be instance of InnerManageTokensRouter"
    //     }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val viewModel = hiltViewModel<ManageTokensViewModel>()
        // viewModel.router = [REDACTED_EMAIL]
        //
        LocalLifecycleOwner.current.lifecycle.addObserver(viewModel)

        val systemBarsColor = TangemTheme.colors.background.secondary
        SystemBarsEffect {
            setSystemBarsColor(systemBarsColor)
        }

        ManageTokensScreen(state = viewModel.uiState)
    }
}