package com.tangem.managetokens.presentation.router

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tangem.features.managetokens.navigation.ManageTokensUi
import com.tangem.managetokens.presentation.managetokens.ui.ManageTokensScreen
import com.tangem.managetokens.presentation.managetokens.viewmodels.ManageTokensViewModel
import javax.inject.Inject

internal class ManageTokensUiImpl @Inject constructor() : ManageTokensUi {

    @Composable
    override fun Content(onHeaderSizeChange: (Dp) -> Unit) {
        val viewModel = hiltViewModel<ManageTokensViewModel>()

        ManageTokensScreen(
            state = viewModel.uiState,
            onHeaderSizeChange = onHeaderSizeChange,
        )
    }
}
