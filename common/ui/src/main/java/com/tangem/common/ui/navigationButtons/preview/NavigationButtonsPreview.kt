package com.tangem.common.ui.navigationButtons.preview

import com.tangem.common.ui.R
import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.common.ui.navigationButtons.NavigationButtonsState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference

internal object NavigationButtonsPreview {

    private val extraButtons = NavigationButton(
        textReference = resourceReference(R.string.common_explore),
        iconRes = R.drawable.ic_tangem_24,
        isSecondary = true,
        isIconVisible = true,
        showProgress = false,
        isEnabled = true,
        onClick = {},
    ) to NavigationButton(
        textReference = resourceReference(R.string.common_share),
        iconRes = R.drawable.ic_tangem_24,
        isSecondary = true,
        isIconVisible = true,
        showProgress = false,
        isEnabled = true,
        onClick = {},
    )

    private val prev = NavigationButton(
        textReference = TextReference.EMPTY,
        iconRes = R.drawable.ic_back_24,
        isSecondary = true,
        isIconVisible = true,
        showProgress = false,
        isEnabled = true,
        onClick = {},
    )

    private val finished = NavigationButton(
        textReference = resourceReference(R.string.common_close),
        isSecondary = false,
        isIconVisible = false,
        showProgress = false,
        isEnabled = true,
        onClick = {},
    )

    val allButtons = NavigationButtonsState.Data(
        primaryButton = finished,
        prevButton = prev,
        extraButtons = extraButtons,
        txUrl = "https://tangem.com",
        onTextClick = {},
    )
}