package com.tangem.tap.features.welcome.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.tangem.core.analytics.Analytics
import com.tangem.core.ui.components.SystemBarsEffect
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.core.ui.theme.AppThemeModeHolder
import com.tangem.tap.common.analytics.events.SignIn
import com.tangem.tap.common.extensions.eraseContext
import com.tangem.tap.features.details.ui.cardsettings.resolveReference
import com.tangem.tap.features.welcome.ui.components.WarningDialog
import com.tangem.tap.features.welcome.ui.components.WelcomeScreenContent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class WelcomeFragment : ComposeFragment() {

    @Inject
    override lateinit var appThemeModeHolder: AppThemeModeHolder

    private val viewModel by viewModels<WelcomeViewModel>()

    override fun onStart() {
        super.onStart()
        Analytics.eraseContext()
        Analytics.send(SignIn.ScreenOpened())
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        val errorMessage by rememberUpdatedState(newValue = state.error?.resolveReference())
        val warning by rememberUpdatedState(newValue = state.warning)

        val backgroundColor = TangemTheme.colors.background.primary
        SystemBarsEffect {
            setSystemBarsColor(backgroundColor)
        }

        BackHandler {
            requireActivity().finish()
        }

        Box(
            modifier = modifier
                .systemBarsPadding()
                .background(backgroundColor),
        ) {
            WelcomeScreenContent(
                showUnlockProgress = state.showUnlockWithBiometricsProgress,
                showScanCardProgress = state.showUnlockWithCardProgress,
                onUnlockClick = viewModel::unlockWallets,
                onScanCardClick = { viewModel.scanCard(lifecycleCoroutineScope = lifecycleScope) },
            )

            SnackbarHost(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(vertical = TangemTheme.dimens.spacing16)
                    .fillMaxWidth(),
                hostState = snackbarHostState,
            )
        }

        WarningDialog(warning)

        LaunchedEffect(key1 = errorMessage) {
            errorMessage?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.closeError()
            }
        }
    }
}
