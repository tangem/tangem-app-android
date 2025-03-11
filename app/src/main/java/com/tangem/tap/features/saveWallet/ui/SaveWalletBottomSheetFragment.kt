package com.tangem.tap.features.saveWallet.ui

import android.content.DialogInterface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.screen.ComposeBottomSheetFragment
import com.tangem.tap.features.details.ui.cardsettings.resolveReference
import com.tangem.tap.features.saveWallet.ui.components.EnrollBiometricsDialogContent
import com.tangem.tap.features.saveWallet.ui.components.SaveWalletScreenContent
import com.tangem.tap.features.saveWallet.ui.models.EnrollBiometricsDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class SaveWalletBottomSheetFragment : ComposeBottomSheetFragment() {

    @Inject
    override lateinit var uiDependencies: UiDependencies

    override val expandedHeightFraction: Float = .98f

    private val viewModel by viewModels<SaveWalletViewModel>()

    override fun onCancel(dialog: DialogInterface) {
        viewModel.cancelOrClose()
        super.onCancel(dialog)
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        val errorMessage by rememberUpdatedState(newValue = state.error?.resolveReference())
        val enrollBiometricsDialog by rememberUpdatedState(newValue = state.enrollBiometricsDialog)

        Box(modifier = modifier) {
            SaveWalletScreenContent(
                showProgress = state.showProgress,
                onSaveWalletClick = viewModel::saveWallet,
                onCloseClick = {
                    viewModel.cancelOrClose()
                    dismissAllowingStateLoss()
                },
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

    @Suppress("TopLevelComposableFunctions")
    @Composable
    private fun EnrollBiometricsDialog(dialog: EnrollBiometricsDialog?) {
        if (dialog == null) return
        EnrollBiometricsDialogContent(dialog)
    }
}