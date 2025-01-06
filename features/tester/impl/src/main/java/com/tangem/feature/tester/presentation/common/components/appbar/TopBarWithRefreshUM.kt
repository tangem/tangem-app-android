package com.tangem.feature.tester.presentation.common.components.appbar

import androidx.annotation.StringRes

internal data class TopBarWithRefreshUM(
    @StringRes val titleResId: Int,
    val onBackClick: () -> Unit,
    val refreshButton: RefreshButton,
) {

    data class RefreshButton(val isVisible: Boolean, val onRefreshClick: () -> Unit)
}