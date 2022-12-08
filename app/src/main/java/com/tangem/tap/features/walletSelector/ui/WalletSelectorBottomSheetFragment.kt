package com.tangem.tap.features.walletSelector.ui

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
import com.tangem.tap.features.walletSelector.ui.components.RenameWalletDialogContent
import com.tangem.tap.features.walletSelector.ui.components.WalletSelectorScreenContent
import com.tangem.tap.features.walletSelector.ui.model.RenameWalletDialog

internal class WalletSelectorBottomSheetFragment : ComposeBottomSheetFragment<WalletSelectorScreenState>() {
    private val viewModel by viewModels<WalletSelectorViewModel>()

    @Composable
    override fun provideState(): State<WalletSelectorScreenState> {
        return viewModel.state.collectAsState()
    }

    @Composable
    override fun ScreenContent(
        modifier: Modifier,
        state: WalletSelectorScreenState,
    ) {
        val snackbarHostState = remember { SnackbarHostState() }
        val errorMessage by rememberUpdatedState(newValue = state.error?.customMessage)
        val renameWalletDialog by rememberUpdatedState(newValue = state.renameWalletDialog)

        Box(modifier = modifier) {
            WalletSelectorScreenContent(
                state = state,
                onWalletClick = viewModel::walletClicked,
                onWalletLongClick = viewModel::walletLongClicked,
                onUnlockClick = viewModel::unlock,
                onAddCardClick = viewModel::addWallet,
                onClearSelectedClick = viewModel::cancelWalletsEditing,
                onEditSelectedWalletClick = viewModel::renameWallet,
                onDeleteSelectedWalletsClick = viewModel::deleteWallets,
            )

            SnackbarHost(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(vertical = TangemTheme.dimens.spacing16)
                    .fillMaxWidth(),
                hostState = snackbarHostState,
            )
        }

        RenameWalletDialog(dialog = renameWalletDialog)

        LaunchedEffect(key1 = errorMessage) {
            errorMessage?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.closeError()
            }
        }
    }

    @Composable
    private fun RenameWalletDialog(
        modifier: Modifier = Modifier,
        dialog: RenameWalletDialog?,
    ) {
        if (dialog == null) return
        RenameWalletDialogContent(modifier, dialog)
    }
}
