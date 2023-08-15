package com.tangem.feature.wallet.presentation.wallet.state.components

import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.feature.wallet.impl.R

/**
 * Wallet manage button state
 *
 * @param config action config
 *
* [REDACTED_AUTHOR]
 */
sealed class WalletManageButton(val config: ActionButtonConfig) {

    /** Lambda be invoked when manage button is clicked */
    abstract val onClick: (() -> Unit)?

    /**
     * Buy
     *
     * @param onClick lambda be invoked when manage button is clicked
     */
    data class Buy(override val onClick: (() -> Unit)? = null) : WalletManageButton(
        config = ActionButtonConfig(
            text = TextReference.Res(id = R.string.common_buy),
            iconResId = R.drawable.ic_plus_24,
            onClick = onClick ?: {},
            enabled = onClick != null,
        ),
    )

    /**
     * Sell
     *
     * @param onClick lambda be invoked when manage button is clicked
     */
    data class Sell(override val onClick: (() -> Unit)? = null) : WalletManageButton(
        config = ActionButtonConfig(
            text = TextReference.Res(id = R.string.common_sell),
            iconResId = R.drawable.ic_currency_24,
            onClick = onClick ?: {},
            enabled = onClick != null,
        ),
    )

    /**
     * Send
     *
     * @param onClick lambda be invoked when manage button is clicked
     */
    data class Send(override val onClick: (() -> Unit)? = null) : WalletManageButton(
        config = ActionButtonConfig(
            text = TextReference.Res(id = R.string.common_send),
            iconResId = R.drawable.ic_arrow_up_24,
            onClick = onClick ?: {},
            enabled = onClick != null,
        ),
    )

    /**
     * Receive
     *
     * @param onClick lambda be invoked when manage button is clicked
     */
    data class Receive(override val onClick: () -> Unit) : WalletManageButton(
        config = ActionButtonConfig(
            text = TextReference.Res(id = R.string.common_receive),
            iconResId = R.drawable.ic_arrow_down_24,
            onClick = onClick,
        ),
    )

    /**
     * Exchange
     *
     * @param onClick lambda be invoked when manage button is clicked
     */
    data class Exchange(override val onClick: (() -> Unit)? = null) : WalletManageButton(
        config = ActionButtonConfig(
            text = TextReference.Res(id = R.string.common_exchange),
            iconResId = R.drawable.ic_exchange_vertical_24,
            onClick = onClick ?: {},
            enabled = onClick != null,
        ),
    )

    /**
     * Copy address
     *
     * @param onClick lambda be invoked when manage button is clicked
     */
    data class CopyAddress(override val onClick: () -> Unit) : WalletManageButton(
        config = ActionButtonConfig(
            text = TextReference.Res(id = R.string.common_copy_address),
            iconResId = R.drawable.ic_copy_24,
            onClick = onClick,
        ),
    )
}
