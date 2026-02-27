package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.button.TangemButtonShape
import com.tangem.core.ui.ds.button.TangemButtonState
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.feature.wallet.impl.R

/**
 * Model for action buttons on the wallet card. It contains the button's text, icon, click listener, and enabled state.
 */
@Immutable
internal sealed class WalletActionButtons(
    private val text: TextReference,
    @DrawableRes private val iconRes: Int,
) {

    abstract val onClick: () -> Unit

    abstract val isEnabled: Boolean

    val buttonUM: TangemButtonUM
        get() = TangemButtonUM(
            text = text,
            iconRes = iconRes,
            type = TangemButtonType.Secondary,
            shape = TangemButtonShape.Rounded,
            onClick = onClick,
            isEnabled = isEnabled,
            state = if (isEnabled) {
                TangemButtonState.Default
            } else {
                TangemButtonState.Disabled
            },
        )

    data class Buy(
        override val onClick: () -> Unit,
        override val isEnabled: Boolean,
    ) : WalletActionButtons(
        text = resourceReference(R.string.common_buy),
        iconRes = R.drawable.ic_plus_default_24,
    )

    data class Swap(
        override val onClick: () -> Unit,
        override val isEnabled: Boolean,
    ) : WalletActionButtons(
        text = resourceReference(R.string.common_swap),
        iconRes = R.drawable.ic_exchange_default_24,
    )

    data class Sell(
        override val onClick: () -> Unit,
        override val isEnabled: Boolean,
    ) : WalletActionButtons(
        text = resourceReference(R.string.common_sell),
        iconRes = R.drawable.ic_dollar_default_24,
    )
}