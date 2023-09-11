package com.tangem.tap.features.walletSelector.ui

import android.app.Dialog
import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.analytics.Analytics
import com.tangem.core.ui.components.wallets.RenameWalletDialogContent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.screen.ComposeBottomSheetFragment
import com.tangem.core.ui.theme.AppThemeModeHolder
import com.tangem.tap.common.analytics.events.MyWallets
import com.tangem.tap.features.details.ui.cardsettings.resolveReference
import com.tangem.tap.features.walletSelector.ui.components.*
import com.tangem.tap.features.walletSelector.ui.model.DialogModel
import com.tangem.tap.features.walletSelector.ui.model.WarningModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class WalletSelectorBottomSheetFragment : ComposeBottomSheetFragment() {

    @Inject
    override lateinit var appThemeModeHolder: AppThemeModeHolder

    private val viewModel by viewModels<WalletSelectorViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Analytics.send(MyWallets.MyWalletsScreenOpened())

        return super.onCreateDialog(savedInstanceState)
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        val errorMessage by rememberUpdatedState(newValue = state.error?.resolveReference())
        val dialog by rememberUpdatedState(newValue = state.dialog)

        Box(modifier = modifier.nestedScroll(rememberNestedScrollInteropConnection())) {
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

        Dialog(dialog = dialog)

        LaunchedEffect(key1 = errorMessage) {
            errorMessage?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.closeError()
            }
        }
    }

    @Suppress("TopLevelComposableFunctions")
    @Composable
    private fun Dialog(dialog: DialogModel?) {
        if (dialog == null) return
        when (dialog) {
            is DialogModel.RemoveWalletDialog -> RemoveWalletDialogContent(dialog)
            is DialogModel.RenameWalletDialog -> {
                RenameWalletDialogContent(
                    name = dialog.currentName,
                    onConfirm = dialog.onConfirm,
                    onDismiss = dialog.onDismiss,
                )
            }
            is WarningModel.BiometricsLockoutWarning -> BiometricsLockoutWarningContent(dialog)
            is WarningModel.KeyInvalidatedWarning -> KeyInvalidatedWarningContent(dialog)
            is WarningModel.BiometricsDisabledWarning -> BiometricsDisabledWarningContent(dialog)
        }
    }
}