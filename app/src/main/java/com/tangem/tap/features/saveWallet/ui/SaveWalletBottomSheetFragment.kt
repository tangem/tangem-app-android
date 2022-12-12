package com.tangem.tap.features.saveWallet.ui

import android.content.DialogInterface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.fragment.app.viewModels
import com.tangem.core.ui.fragments.ComposeBottomSheetFragment
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.details.ui.cardsettings.resolveReference
import com.tangem.tap.features.saveWallet.ui.components.EnrollBiometricsDialogContent
import com.tangem.tap.features.saveWallet.ui.components.SaveWalletScreenContent
import com.tangem.tap.features.saveWallet.ui.models.EnrollBiometricsDialog

internal class SaveWalletBottomSheetFragment : ComposeBottomSheetFragment<SaveWalletScreenState>() {
    override val expandedHeightFraction: Float = .98f

    private val viewModel by viewModels<SaveWalletViewModel>()

    override fun onDismiss(dialog: DialogInterface) {
        viewModel.dismiss()
        super.onDismiss(dialog)
    }

    @Composable
    override fun provideState(): State<SaveWalletScreenState> {
        return viewModel.state.collectAsState()
    }

    @Composable
    override fun ScreenContent(
        modifier: Modifier,
        state: SaveWalletScreenState,
    ) {
        val snackbarHostState = remember { SnackbarHostState() }
        val errorMessage by rememberUpdatedState(newValue = state.error?.resolveReference())
        val enrollBiometricsDialog by rememberUpdatedState(newValue = state.enrollBiometricsDialog)

        Box(modifier = modifier) {
            SaveWalletScreenContent(
                showProgress = state.showProgress,
                onSaveWalletClick = viewModel::saveWallet,
                onCloseClick = this@SaveWalletBottomSheetFragment::dismissAllowingStateLoss,
            )

            SnackbarHost(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(vertical = TangemTheme.dimens.spacing16)
                    .fillMaxWidth(),
                hostState = snackbarHostState,
            )
        }

        EnrollBiometricsDialog(dialog = enrollBiometricsDialog)

        LaunchedEffect(key1 = errorMessage) {
            errorMessage?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.closeError()
            }
        }
    }

    @Composable
    private fun EnrollBiometricsDialog(
        modifier: Modifier = Modifier,
        dialog: EnrollBiometricsDialog?,
    ) {
        if (dialog == null) return
        EnrollBiometricsDialogContent(modifier, dialog)
    }
}
