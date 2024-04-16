package com.tangem.feature.wallet.presentation.wallet.ui.components.visa

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.PrimaryButtonIconStart
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.DepositButtonState

private const val DEPOSIT_BUTTON_CONTENT_TYPE = "DepositButton"

internal fun LazyListScope.depositButton(state: DepositButtonState, modifier: Modifier = Modifier) {
    item(key = DEPOSIT_BUTTON_CONTENT_TYPE, contentType = DEPOSIT_BUTTON_CONTENT_TYPE) {
        PrimaryButtonIconStart(
            modifier = modifier,
            text = "Deposit",
            iconResId = R.drawable.ic_arrow_down_24,
            onClick = state.onClick,
            enabled = state.isEnabled,
        )
    }
}