package com.tangem.features.managetokens.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.unit.Dp

interface ManageTokensUi {

    @Suppress("TopLevelComposableFunctions")
    @Composable
    fun Content(onHeaderSizeChange: (Dp) -> Unit, state: State<ExpandableState>)
}