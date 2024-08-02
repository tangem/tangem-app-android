package com.tangem.feature.referral

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels
import com.tangem.common.routing.AppRouter
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.feature.referral.router.ReferralRouter
import com.tangem.feature.referral.ui.ReferralScreen
import com.tangem.feature.referral.viewmodels.ReferralViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReferralFragment : ComposeFragment() {

    @Inject
    override lateinit var uiDependencies: UiDependencies

    @Inject
    internal lateinit var appRouter: AppRouter

    private val viewModel by viewModels<ReferralViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.onScreenOpened()
        viewModel.setRouter(ReferralRouter(appRouter))
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        ReferralScreen(stateHolder = viewModel.uiState)
    }
}