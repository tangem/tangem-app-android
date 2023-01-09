package com.tangem.tap.features.welcome.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.fragment.app.viewModels
import com.tangem.core.ui.components.SystemBarsEffect
import com.tangem.core.ui.fragments.ComposeFragment
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.details.ui.cardsettings.resolveReference
import com.tangem.tap.features.welcome.ui.components.WelcomeScreenContent
import com.tangem.wallet.R

internal class WelcomeFragment : ComposeFragment<WelcomeScreenState>() {
    private val viewModel by viewModels<WelcomeViewModel>()

    @Composable
    override fun provideState(): State<WelcomeScreenState> {
        return viewModel.state.collectAsState()
    }

    @Composable
    override fun ScreenContent(
        modifier: Modifier,
        state: WelcomeScreenState,
    ) {
        val snackbarHostState = remember { SnackbarHostState() }
        val errorMessage by rememberUpdatedState(newValue = state.error?.resolveReference())

        val backgroundColor = colorResource(id = R.color.background_primary)
        SystemBarsEffect {
            setSystemBarsColor(color = backgroundColor)
        }

        BackHandler {
            requireActivity().finish()
        }

        Box(
            modifier = modifier
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

        LaunchedEffect(key1 = errorMessage) {
            errorMessage?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.closeError()
            }
        }
    }
}
