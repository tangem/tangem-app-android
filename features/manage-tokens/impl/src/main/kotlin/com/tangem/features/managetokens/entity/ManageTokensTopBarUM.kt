package com.tangem.features.managetokens.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed class ManageTokensTopBarUM {

    abstract val title: TextReference
    abstract val onBackButtonClick: () -> Unit

    data class ReadContent(
        override val title: TextReference,
        override val onBackButtonClick: () -> Unit,
    ) : ManageTokensTopBarUM()

    data class ManageContent(
        override val title: TextReference,
        override val onBackButtonClick: () -> Unit,
        val endButton: TopAppBarButtonUM,
    ) : ManageTokensTopBarUM()
}