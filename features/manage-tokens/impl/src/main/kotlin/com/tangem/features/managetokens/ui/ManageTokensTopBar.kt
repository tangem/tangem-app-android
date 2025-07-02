package com.tangem.features.managetokens.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.fields.SearchBar
import com.tangem.core.ui.components.fields.TangemSearchBarDefaults
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.managetokens.entity.managetokens.ManageTokensTopBarUM

@Composable
internal fun ManageTokensTopBar(topBar: ManageTokensTopBarUM?, search: SearchBarUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
    ) {
        if (topBar != null) {
            TangemTopAppBar(
                title = topBar.title.resolveReference(),
                startButton = TopAppBarButtonUM.Back(topBar.onBackButtonClick),
                endButton = when (topBar) {
                    is ManageTokensTopBarUM.ManageContent -> topBar.endButton
                    is ManageTokensTopBarUM.ReadContent -> null
                },
            )
        }
        SearchBar(
            colors = TangemSearchBarDefaults.secondaryTextFieldColors,
            state = search,
            modifier = Modifier
                .padding(bottom = TangemTheme.dimens.spacing12)
                .padding(horizontal = TangemTheme.dimens.spacing16),
        )
    }
}