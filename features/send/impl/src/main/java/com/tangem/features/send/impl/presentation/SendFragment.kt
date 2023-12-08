package com.tangem.features.send.impl.presentation

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels
import com.tangem.core.ui.components.SystemBarsEffect
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.core.ui.theme.AppThemeModeHolder
import com.tangem.features.send.api.navigation.SendRouter
import com.tangem.features.send.impl.navigation.InnerSendRouter
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.features.send.impl.presentation.ui.SendScreen
import com.tangem.features.send.impl.presentation.viewmodel.SendViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Send fragment
 */
@AndroidEntryPoint
internal class SendFragment : ComposeFragment() {

    @Inject
    override lateinit var appThemeModeHolder: AppThemeModeHolder

    @Inject
    lateinit var router: SendRouter

    private val viewModel by viewModels<SendViewModel>()
    private val innerSendRouter: InnerSendRouter
        get() = requireNotNull(router as? InnerSendRouter) {
            "innerSendRouter should be instance of InnerSendRouter"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
        viewModel.setRouter(
            innerSendRouter,
            StateRouter(
                fragmentManager = WeakReference(parentFragmentManager),
            ),
        )
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val systemBarsColor = TangemTheme.colors.background.tertiary
        SystemBarsEffect {
            setSystemBarsColor(systemBarsColor)
        }
        SendScreen(viewModel.uiState)
    }

    override fun onDestroy() {
        lifecycle.removeObserver(viewModel)
        super.onDestroy()
    }

    companion object {

        /** Create send fragment instance */
        fun create(): SendFragment = SendFragment()
    }
}
