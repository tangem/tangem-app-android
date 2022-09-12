package com.tangem.tap.features.userWalletsList.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import com.google.accompanist.appcompattheme.AppCompatTheme
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tangem.tap.features.userWalletsList.ui.components.WalletSelectorScreenContent
import com.tangem.wallet.R

class WalletSelectorBottomSheetFragment : BottomSheetDialogFragment() {
    private val viewModel by viewModels<WalletSelectorViewModel>()

    override fun getTheme(): Int {
        return R.style.AppTheme_BottomSheetDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(inflater.context).apply {
            background = null

            setContent {
                val state by viewModel.state.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }
                val errorMessage by rememberUpdatedState(newValue = state.error?.customMessage)

                AppCompatTheme {
                    Box(
                        modifier = Modifier
                            .wrapContentSize()
                            .clip(
                                shape = RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                ),
                            ),
                    ) {
                        WalletSelectorScreenContent(
                            state = state,
                            onWalletClick = viewModel::selectWallet,
                            onWalletLongClick = viewModel::editWallet,
                            onUnlockClick = viewModel::unlock,
                            onScanCardClick = viewModel::addWallet,
                            onClearSelectedClick = viewModel::cancelWalletEditing,
                            onEditSelectedWalletClick = viewModel::renameWallet,
                            onDeleteSelectedWalletsClick = viewModel::deleteWallet,
                        )

                        SnackbarHost(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(vertical = 16.dp)
                                .fillMaxWidth(),
                            hostState = snackbarHostState,
                        )
                    }
                }

                LaunchedEffect(key1 = errorMessage) {
                    if (errorMessage != null) {
                        snackbarHostState.showSnackbar(state.error!!.customMessage)
                        viewModel.closeError()
                    }
                }
            }
        }
    }
}
