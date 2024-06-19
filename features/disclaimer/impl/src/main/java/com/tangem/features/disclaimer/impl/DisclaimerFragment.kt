package com.tangem.features.disclaimer.impl

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels
import com.tangem.common.routing.AppRoute.Disclaimer.Companion.IS_TOS_ACCEPTED_KEY
import com.tangem.common.routing.AppRouter
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.features.disclaimer.impl.presentation.ui.DisclaimerScreen
import com.tangem.features.disclaimer.impl.presentation.viewmodel.DisclaimerViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class DisclaimerFragment : ComposeFragment() {

    @Inject
    override lateinit var uiDependencies: UiDependencies

    @Inject
    lateinit var appRouter: AppRouter

    private val viewModel by viewModels<DisclaimerViewModel>()

    private val isTosAccepted: Boolean
        get() = arguments?.getBoolean(IS_TOS_ACCEPTED_KEY) ?: false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        BackHandler {
            if (isTosAccepted) {
                appRouter.pop()
            } else {
                requireActivity().finish()
            }
        }
        DisclaimerScreen(viewModel.state, appRouter::pop)
    }

    companion object {
        /** Create disclaimer fragment instance */
        fun create(): DisclaimerFragment = DisclaimerFragment()
    }
}