package com.tangem.feature.referral

import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tangem.feature.referral.presentation.R
import com.tangem.feature.referral.router.ReferralRouter
import com.tangem.feature.referral.ui.ReferralScreen
import com.tangem.feature.referral.viewmodels.ReferralViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference

@AndroidEntryPoint
class ReferralFragment : Fragment() {
    private val viewModel by viewModels<ReferralViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.fade)
        exitTransition = inflater.inflateTransition(R.transition.fade)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        activity?.window?.let { WindowCompat.setDecorFitsSystemWindows(it, true) }
        viewModel.setRouter(ReferralRouter(fragmentManager = WeakReference(parentFragmentManager)))
        return ComposeView(inflater.context).apply {
            setContent {
                ReferralScreen(stateHolder = viewModel.uiState)
            }
        }
    }
}
