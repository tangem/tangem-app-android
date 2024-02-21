package com.tangem.features.managetokens.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

interface ManageTokensUi {

    @Suppress("TopLevelComposableFunctions")
    @Composable
    fun content(onHeaderSizeChange: (Dp) -> Unit): (ExpandableState) -> Unit
}
