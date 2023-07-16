package com.tangem.core.ui.components.managebuttons

import com.tangem.core.ui.R
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig

sealed class ManageButtons(val config: ActionButtonConfig) {

    /**
     * Buy
     *
     * @param onClick lambda be invoked when manage button is clicked
     */
    data class Buy(val onClick: () -> Unit) : ManageButtons(
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
    data class Send(val onClick: () -> Unit) : ManageButtons(
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
    data class Receive(val onClick: () -> Unit) : ManageButtons(
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
    data class Exchange(val onClick: () -> Unit) : ManageButtons(
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
    data class CopyAddress(val onClick: () -> Unit) : ManageButtons(
        config = ActionButtonConfig(
            text = "Copy address",
            iconResId = R.drawable.ic_copy_24,
            onClick = onClick,
        ),
    )
}
