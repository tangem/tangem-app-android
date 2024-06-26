package com.tangem.features.managetokens.component.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.features.managetokens.component.ManageTokensComponent
import com.tangem.features.managetokens.entity.ManageTokensUM
import com.tangem.features.managetokens.ui.ManageTokensScreen
import kotlinx.collections.immutable.persistentListOf

internal class PreviewManageTokensComponent : ManageTokensComponent {

    private val previewState: ManageTokensUM = ManageTokensUM(
        popBack = {},
    )

    @Composable
    override fun Content(modifier: Modifier) {
        ManageTokensScreen(
            modifier = modifier,
            state = previewState,
        )
    }
}