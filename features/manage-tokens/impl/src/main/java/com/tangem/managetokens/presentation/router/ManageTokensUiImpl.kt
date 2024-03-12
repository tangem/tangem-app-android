package com.tangem.managetokens.presentation.router

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tangem.features.managetokens.navigation.ExpandableState
import com.tangem.features.managetokens.navigation.ManageTokensUi
import com.tangem.managetokens.presentation.managetokens.ui.ManageTokensScreen
import com.tangem.managetokens.presentation.managetokens.viewmodels.ManageTokensViewModel
import javax.inject.Inject

internal class ManageTokensUiImpl @Inject constructor() : ManageTokensUi {

    @Composable
    override fun Content(onHeaderSizeChange: (Dp) -> Unit, state: State<ExpandableState>) {
        val viewModel = hiltViewModel<ManageTokensViewModel>()
        viewModel.setExpandableState(state)

        ManageTokensScreen(
            state = viewModel.uiState,
            onHeaderSizeChange = onHeaderSizeChange,
        )
    }
}