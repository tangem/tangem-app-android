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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.analytics.Analytics
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.screen.ComposeFragment
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
    override lateinit var uiDependencies: UiDependencies

    override fun onStart() {
        super.onStart()
        Analytics.eraseContext()
        Analytics.send(SignIn.ScreenOpened())
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val viewModel = hiltViewModel<WelcomeViewModel>()
        LocalLifecycleOwner.current.lifecycle.addObserver(viewModel)

        val state by viewModel.state.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        val errorMessage by rememberUpdatedState(newValue = state.error?.resolveReference())
        val warning by rememberUpdatedState(newValue = state.warning)

        BackHandler {
            requireActivity().finish()
        }

        Box(
            modifier = modifier
                .background(TangemTheme.colors.background.primary)
                .systemBarsPadding(),
        ) {
            WelcomeScreenContent(
                showUnlockProgress = state.showUnlockWithBiometricsProgress,
                showScanCardProgress = state.showUnlockWithCardProgress,
                onUnlockClick = viewModel::unlockWallets,
                onScanCardClick = viewModel::scanCard,
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