package com.tangem.feature.referral

import android.os.Bundle
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels
import com.tangem.core.ui.components.SystemBarsEffect
import com.tangem.core.ui.haptic.HapticManager
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.core.ui.theme.AppThemeModeHolder
import com.tangem.feature.referral.router.ReferralRouter
import com.tangem.feature.referral.ui.ReferralScreen
import com.tangem.feature.referral.viewmodels.ReferralViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import javax.inject.Inject

@AndroidEntryPoint
class ReferralFragment : ComposeFragment() {

    @Inject
    override lateinit var appThemeModeHolder: AppThemeModeHolder

    @Inject
    override lateinit var hapticManager: HapticManager

    private val viewModel by viewModels<ReferralViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.onScreenOpened()
        viewModel.setRouter(ReferralRouter(fragmentManager = WeakReference(parentFragmentManager)))
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val backgroundColor = TangemTheme.colors.background.secondary
        SystemBarsEffect { setSystemBarsColor(backgroundColor) }

        ReferralScreen(
            modifier = Modifier.systemBarsPadding(),
            stateHolder = viewModel.uiState,
        )
    }
}