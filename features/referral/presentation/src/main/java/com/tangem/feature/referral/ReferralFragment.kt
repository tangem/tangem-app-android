package com.tangem.feature.referral

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tangem.feature.referral.ui.ReferralScreen
import com.tangem.feature.referral.viewmodels.ReferralViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReferralFragment : Fragment() {
    private val viewModel by viewModels<ReferralViewModel>().apply {
        // TODO("[REDACTED_JIRA]") this.value.setOnBackClicked()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(inflater.context).apply {
            setContent {
                ReferralScreen(stateHolder = viewModel.uiState)
            }
        }
    }
}