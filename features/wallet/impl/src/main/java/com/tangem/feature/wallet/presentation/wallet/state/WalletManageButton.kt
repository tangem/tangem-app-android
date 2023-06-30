package com.tangem.feature.wallet.presentation.wallet.state

import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.feature.wallet.impl.R

/**
 * Wallet manage button state
 *
 * @param config action config
 *
[REDACTED_AUTHOR]
 */
sealed class WalletManageButton(val config: ActionButtonConfig) {

    /**
     * Buy
     *
     * @param onClick lambda be invoked when manage button is clicked
     */
    data class Buy(val onClick: () -> Unit) : WalletManageButton(
        config = ActionButtonConfig(
            text = "Buy",
            iconResId = R.drawable.ic_plus_24,
            onClick = onClick,
        ),
    )

    /**
     * Send
     *
     * @param onClick lambda be invoked when manage button is clicked
     */
    data class Send(val onClick: () -> Unit) : WalletManageButton(
        config = ActionButtonConfig(
            text = "Send",
            iconResId = R.drawable.ic_arrow_up_24,
            onClick = onClick,
        ),
    )

    /**
     * Receive
     *
     * @param onClick lambda be invoked when manage button is clicked
     */
    data class Receive(val onClick: () -> Unit) : WalletManageButton(
        config = ActionButtonConfig(
            text = "Receive",
            iconResId = R.drawable.ic_arrow_down_24,
            onClick = onClick,
        ),
    )

    /**
     * Exchange
     *
     * @param onClick lambda be invoked when manage button is clicked
     */
    data class Exchange(val onClick: () -> Unit) : WalletManageButton(
        config = ActionButtonConfig(
            text = "Exchange",
            iconResId = R.drawable.ic_exchange_vertical_24,
            onClick = onClick,
        ),
    )

    /**
     * Copy address
     *
     * @param onClick lambda be invoked when manage button is clicked
     */
    data class CopyAddress(val onClick: () -> Unit) : WalletManageButton(
        config = ActionButtonConfig(
            text = "Copy address",
            iconResId = R.drawable.ic_copy_24,
            onClick = onClick,
        ),
    )
}