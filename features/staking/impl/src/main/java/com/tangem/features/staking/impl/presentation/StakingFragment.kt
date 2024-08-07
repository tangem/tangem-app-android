package com.tangem.features.staking.impl.presentation

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.features.staking.api.navigation.StakingRouter
import com.tangem.features.staking.impl.navigation.InnerStakingRouter
import com.tangem.features.staking.impl.presentation.state.StakingStateController
import com.tangem.features.staking.impl.presentation.state.StakingStateRouter
import com.tangem.features.staking.impl.presentation.ui.StakingScreen
import com.tangem.features.staking.impl.presentation.viewmodel.StakingViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Staking fragment
 */
@AndroidEntryPoint
internal class StakingFragment : ComposeFragment() {

    @Inject
    override lateinit var uiDependencies: UiDependencies

    @Inject
    lateinit var stakingRouter: StakingRouter

    @Inject
    lateinit var appRouter: AppRouter

    @Inject
    lateinit var stateController: StakingStateController

    @Inject
    lateinit var analyticsEventsHandler: AnalyticsEventHandler

    private val viewModel by viewModels<StakingViewModel>()
    private val innerStakingRouter: InnerStakingRouter
        get() = requireNotNull(stakingRouter as? InnerStakingRouter) {
            "innerStakingRouter should be instance of InnerStakingRouter"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)

        viewModel.setRouter(
            innerStakingRouter,
            StakingStateRouter(
                appRouter = appRouter,
                stateController = stateController,
            ),
        )
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val currentState = viewModel.uiState.collectAsStateWithLifecycle()
        StakingScreen(currentState.value)
    }

    override fun onDestroy() {
        lifecycle.removeObserver(viewModel)
        super.onDestroy()
    }

    companion object {
        /** Create staking fragment instance */
        fun create(): StakingFragment = StakingFragment()
    }
}