package com.tangem.features.send.impl.presentation

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.features.send.api.navigation.SendRouter
import com.tangem.features.send.impl.navigation.InnerSendRouter
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.features.send.impl.presentation.ui.SendScreen
import com.tangem.features.send.impl.presentation.viewmodel.SendViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Send fragment
 */
@AndroidEntryPoint
internal class SendFragment : ComposeFragment() {

    @Inject
    override lateinit var uiDependencies: UiDependencies

    @Inject
    lateinit var router: SendRouter

    @Inject
    lateinit var appRouter: AppRouter

    @Inject
    lateinit var analyticsEventsHandler: AnalyticsEventHandler

    private val viewModel by viewModels<SendViewModel>()
    private val innerSendRouter: InnerSendRouter
        get() = requireNotNull(router as? InnerSendRouter) {
            "innerSendRouter should be instance of InnerSendRouter"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)

        val isEditingDisabled = arguments?.getString(AppRoute.Send.TRANSACTION_ID_KEY) != null
        viewModel.setRouter(
            innerSendRouter,
            StateRouter(
                appRouter = appRouter,
                isEditingDisabled = isEditingDisabled,
                analyticsEventsHandler = analyticsEventsHandler,
            ),
        )
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val currentState = viewModel.stateRouter.currentState.collectAsStateWithLifecycle()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        SendScreen(uiState, currentState.value)
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