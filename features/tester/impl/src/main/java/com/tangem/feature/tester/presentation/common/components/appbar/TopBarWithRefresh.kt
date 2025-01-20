package com.tangem.feature.tester.presentation.common.components.appbar

import androidx.compose.runtime.Composable
import com.tangem.core.ui.components.appbar.AppBarWithBackButtonAndIcon
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.impl.R

@Composable
internal fun TopBarWithRefresh(state: TopBarWithRefreshUM) {
    AppBarWithBackButtonAndIcon(
        onBackClick = state.onBackClick,
        text = stringResourceSafe(state.titleResId),
        iconRes = R.drawable.ic_refresh_24.takeIf { state.refreshButton.isVisible },
        onIconClick = state.refreshButton.onRefreshClick,
        backgroundColor = TangemTheme.colors.background.primary,
    )
}